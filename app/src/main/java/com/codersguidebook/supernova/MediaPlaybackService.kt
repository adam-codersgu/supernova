package com.codersguidebook.supernova

import android.app.PendingIntent
import android.content.*
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioManager.*
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ResultReceiver
import android.provider.MediaStore
import android.service.media.MediaBrowserService
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.MediaSessionCompat.QueueItem
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.media.session.PlaybackStateCompat.*
import android.text.TextUtils
import android.view.KeyEvent
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media.MediaBrowserServiceCompat
import com.codersguidebook.supernova.params.MediaServiceConstants.Companion.ACTION_NEXT
import com.codersguidebook.supernova.params.MediaServiceConstants.Companion.ACTION_PAUSE
import com.codersguidebook.supernova.params.MediaServiceConstants.Companion.ACTION_PLAY
import com.codersguidebook.supernova.params.MediaServiceConstants.Companion.ACTION_PREVIOUS
import com.codersguidebook.supernova.params.MediaServiceConstants.Companion.LOAD_SONGS
import com.codersguidebook.supernova.params.MediaServiceConstants.Companion.MOVE_QUEUE_ITEM
import com.codersguidebook.supernova.params.MediaServiceConstants.Companion.REMOVE_QUEUE_ITEM_BY_ID
import com.codersguidebook.supernova.params.MediaServiceConstants.Companion.RESTORE_PLAY_QUEUE
import com.codersguidebook.supernova.params.MediaServiceConstants.Companion.SET_REPEAT_MODE
import com.codersguidebook.supernova.params.MediaServiceConstants.Companion.SET_SHUFFLE_MODE
import com.codersguidebook.supernova.params.MediaServiceConstants.Companion.UPDATE_QUEUE_ITEM
import com.codersguidebook.supernova.params.ResultReceiverConstants.Companion.SUCCESS
import com.codersguidebook.supernova.params.SharedPreferencesConstants.Companion.REPEAT_MODE
import com.codersguidebook.supernova.params.SharedPreferencesConstants.Companion.SHUFFLE_MODE
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException

class MediaPlaybackService : MediaBrowserServiceCompat(), MediaPlayer.OnErrorListener {

    private val channelID = "supernova"
    private var currentlyPlayingQueueItemId = -1L
    private val logTag = "AudioPlayer"
    private val handler = Handler(Looper.getMainLooper())
    private var mediaPlayer: MediaPlayer? = null
    private val playQueue: MutableList<QueueItem> = mutableListOf()
    private lateinit var audioFocusRequest: AudioFocusRequest
    private lateinit var mediaSessionCompat: MediaSessionCompat

    private val afChangeListener = OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AUDIOFOCUS_LOSS, AUDIOFOCUS_LOSS_TRANSIENT -> {
                mediaSessionCompat.controller.transportControls.pause()
            }
            AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> mediaPlayer?.setVolume(0.3f, 0.3f)
            AUDIOFOCUS_GAIN -> mediaPlayer?.setVolume(1.0f, 1.0f)
        }
    }

    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (mediaPlayer != null && mediaPlayer!!.isPlaying) mediaSessionCallback.onPause()
        }
    }

    private var playbackPositionChecker = object : Runnable {
        override fun run() {
            try {
                if (mediaPlayer?.isPlaying == true) {
                    val playbackPosition = mediaPlayer!!.currentPosition.toLong()
                    setMediaPlaybackState(STATE_PLAYING, playbackPosition, 1f, null)
                }
            } finally {
                handler.postDelayed(this, 1000L)
            }
        }
    }

    private val mediaSessionCallback: MediaSessionCompat.Callback = object : MediaSessionCompat.Callback() {
        override fun onMediaButtonEvent(mediaButtonEvent: Intent?): Boolean {
            val keyEvent: KeyEvent? = mediaButtonEvent?.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
            if (keyEvent != null && mediaPlayer != null) {
                when (keyEvent.keyCode) {
                    KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                        if (mediaPlayer!!.isPlaying) onPause()
                        else onPlay()
                    }
                    KeyEvent.KEYCODE_MEDIA_PLAY -> onPlay()
                    KeyEvent.KEYCODE_MEDIA_PAUSE -> onPause()
                    KeyEvent.KEYCODE_MEDIA_SKIP_BACKWARD -> onSkipToPrevious()
                    KeyEvent.KEYCODE_MEDIA_SKIP_FORWARD -> onSkipToNext()
                }
            }
            return super.onMediaButtonEvent(mediaButtonEvent)
        }

        override fun onAddQueueItem(description: MediaDescriptionCompat?) {
            onAddQueueItem(description, playQueue.size)
        }

        override fun onAddQueueItem(description: MediaDescriptionCompat?, index: Int) {
            super.onAddQueueItem(description, index)

            val sortedQueue = playQueue.sortedByDescending {
                it.queueId
            }
            val queueId = if (sortedQueue.isNotEmpty()) sortedQueue[0].queueId + 1
            else 0

            val queueItem = QueueItem(description, queueId)
            try {
                playQueue.add(index, queueItem)
            } catch (exception: IndexOutOfBoundsException) {
                playQueue.add(playQueue.size, queueItem)
            }
        }

        override fun onPrepare() {
            super.onPrepare()

            if (playQueue.isEmpty()) {
                error()
                return
            }

            // If no alternative currently play queue item ID has been set, then play from the beginning of the queue
            if (currentlyPlayingQueueItemId == -1L) currentlyPlayingQueueItemId = playQueue[0].queueId

            setCurrentMetadata()

            mediaPlayer?.apply {
                stop()
                release()
            }

            try {
                val currentQueueItem = getCurrentQueueItem()
                val currentQueueItemUri = currentQueueItem?.description?.mediaId?.let {
                    ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        it.toLong())
                }
                if (currentQueueItemUri == null) {
                    Toast.makeText(application,
                        "The URI for the currently loaded song could not be found.",
                        Toast.LENGTH_LONG).show()
                    return
                }
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                    )
                    setDataSource(application, currentQueueItemUri)
                    setOnErrorListener(this@MediaPlaybackService)
                    prepare()
                }
                // Refresh the notification so user can see the song has changed
                refreshNotification()
                setMediaPlaybackState(STATE_NONE, 0, 0f, null)
            } catch (e: IOException) {
                error()
            } catch (e: IllegalStateException) {
                error()
            } catch (e: IllegalArgumentException) {
                error()
            }
        }

        override fun onPlay() {
            super.onPlay()

            if (mediaPlayer != null && !mediaPlayer!!.isPlaying) {
                val audioManager = applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

                audioFocusRequest = AudioFocusRequest.Builder(AUDIOFOCUS_GAIN).run {
                    setAudioAttributes(AudioAttributes.Builder().run {
                        setOnAudioFocusChangeListener(afChangeListener)
                        setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        build()
                    })
                    build()
                }

                val audioFocusRequestOutcome = audioManager.requestAudioFocus(audioFocusRequest)
                if (audioFocusRequestOutcome == AUDIOFOCUS_REQUEST_GRANTED) {
                    startService(Intent(applicationContext, MediaBrowserService::class.java))
                    mediaSessionCompat.isActive = true
                    try {
                        mediaPlayer!!.apply {
                            start()
                            setOnCompletionListener {
                                val currentlyPlayingQueueItem = getCurrentQueueItem()
                                currentlyPlayingQueueItem?.let {
                                    val currentlyPlayingSongId = it.description.mediaId
                                    if (currentlyPlayingSongId != null) {
                                        val bundle = Bundle().apply {
                                            putLong("finishedSongId", currentlyPlayingSongId.toLong())
                                        }
                                        setMediaPlaybackState(STATE_SKIPPING_TO_NEXT, 0,
                                            0f, bundle)
                                    }
                                }

                                val repeatMode = mediaSessionCompat.controller.repeatMode
                                when {
                                    repeatMode == REPEAT_MODE_ONE -> {}
                                    repeatMode == REPEAT_MODE_ALL ||
                                    playQueue.isNotEmpty() && playQueue[playQueue.size - 1].queueId != currentlyPlayingQueueItemId -> {
                                        onSkipToNext()
                                        return@setOnCompletionListener
                                    }
                                    else -> {
                                        onStop()
                                        return@setOnCompletionListener
                                    }
                                }

                                onPrepare()
                                onPlay()
                            }
                        }
                        refreshNotification()
                        val playbackPosition = mediaPlayer!!.currentPosition.toLong()
                        setMediaPlaybackState(STATE_PLAYING, playbackPosition,
                            1f, getBundleWithSongDuration())
                    } catch (e: IllegalStateException) {
                        error()
                    } catch (e: NullPointerException) {
                        error()
                    }
                }
            }
        }

        override fun onPause() {
            super.onPause()
            mediaPlayer?.pause()
            val playbackPosition = mediaPlayer?.currentPosition?.toLong() ?: 0
            setMediaPlaybackState(STATE_PAUSED, playbackPosition, 0f, getBundleWithSongDuration())
            refreshNotification()
        }

        override fun onFastForward() {
            super.onFastForward()

            val newPlaybackPosition = mediaPlayer?.currentPosition?.plus(5000) ?: return
            if (newPlaybackPosition > (mediaPlayer?.duration ?: return)) onSkipToNext()
            else onSeekTo(newPlaybackPosition.toLong())
        }

        override fun onRewind() {
            super.onRewind()

            val newPlaybackPosition = mediaPlayer?.currentPosition?.minus(5000) ?: return
            if (newPlaybackPosition < 0) onSkipToPrevious()
            else onSeekTo(newPlaybackPosition.toLong())
        }

        override fun onSetRepeatMode(repeatMode: Int) {
            super.onSetRepeatMode(repeatMode)

            mediaSessionCompat.setRepeatMode(repeatMode)
        }

        override fun onSetShuffleMode(shuffleMode: Int) {
            super.onSetShuffleMode(shuffleMode)

            mediaSessionCompat.setShuffleMode(shuffleMode)
        }

        override fun onCommand(command: String?, extras: Bundle?, cb: ResultReceiver?) {
            super.onCommand(command, extras, cb)

            when (command) {
                RESTORE_PLAY_QUEUE -> {
                    extras?.let {
                        val queueItemsJson = it.getString("queueItemPairs") ?: return@let
                        val gson = Gson()
                        val itemType = object : TypeToken<List<Pair<Long, Long>>>() {}.type
                        val queueItemPairs = gson.fromJson<List<Pair<Long, Long>>>(queueItemsJson, itemType)

                        playQueue.clear()
                        currentlyPlayingQueueItemId = -1L

                        // Pair mapping is <Long, Long> -> <QueueId, songId>
                        for (pair in queueItemPairs) {
                            val mediaDescription = MediaDescriptionCompat.Builder()
                                .setMediaId(pair.second.toString())
                                .build()
                            val queueItem = QueueItem(mediaDescription, pair.first)
                            playQueue.add(queueItem)
                        }

                        mediaSessionCompat.setQueue(playQueue)

                        currentlyPlayingQueueItemId = it.getLong("queueItemId", -1L)
                        if (currentlyPlayingQueueItemId != -1L) {
                            onSkipToQueueItem(currentlyPlayingQueueItemId)
                        }

                        cb?.send(SUCCESS, Bundle())
                    }
                }
                LOAD_SONGS -> {
                    extras?.let {
                        val songIdsJson = it.getString("songIds") ?: return@let
                        val gson = Gson()
                        val listType = object : TypeToken<List<Long>>() {}.type
                        var songIds = gson.fromJson<List<Long>>(songIdsJson, listType)

                        val addSongsAfterCurrentQueueItem = it.getBoolean("addSongsAfterCurrentQueueItem")
                        if (addSongsAfterCurrentQueueItem) songIds = songIds.reversed()
                        for (id in songIds) {
                            val mediaDescription = MediaDescriptionCompat.Builder()
                                .setMediaId(id.toString())
                                .build()
                            if (addSongsAfterCurrentQueueItem) {
                                val targetIndex = playQueue.indexOfFirst { queueItem ->
                                    queueItem.queueId == currentlyPlayingQueueItemId
                                } + 1
                                onAddQueueItem(mediaDescription, targetIndex)
                            } else onAddQueueItem(mediaDescription)
                        }

                        if (it.getBoolean("shuffle")) playQueue.shuffle()

                        mediaSessionCompat.setQueue(playQueue)
                        cb?.send(SUCCESS, Bundle())
                    }
                }
                MOVE_QUEUE_ITEM -> {
                    extras?.let {
                        val queueItemId = it.getLong("queueItemId", -1L)
                        val newIndex = it.getInt("newIndex", -1)
                        if (queueItemId == -1L || newIndex == -1 || newIndex >= playQueue.size) return@let

                        val oldIndex = playQueue.indexOfFirst { queueItem -> queueItem.queueId == queueItemId }
                        if (oldIndex == -1) return@let
                        val queueItem = playQueue[oldIndex]
                        playQueue.removeAt(oldIndex)
                        playQueue.add(newIndex, queueItem)
                        setPlayQueue()
                    }
                }
                REMOVE_QUEUE_ITEM_BY_ID -> {
                    extras?.let {
                        val queueItemId = extras.getLong("queueItemId", -1L)
                        when (queueItemId) {
                            -1L -> return@let
                            currentlyPlayingQueueItemId -> onSkipToNext()
                        }
                        playQueue.removeIf { it.queueId == queueItemId }
                        setPlayQueue()
                        cb?.send(SUCCESS, Bundle())
                    }
                }
                SET_REPEAT_MODE -> {
                    extras?.let {
                        val repeatMode = extras.getInt(REPEAT_MODE, REPEAT_MODE_NONE)
                        onSetRepeatMode(repeatMode)
                    }
                }
                SET_SHUFFLE_MODE -> {
                    extras?.let {
                        val shuffleMode = extras.getInt(SHUFFLE_MODE, SHUFFLE_MODE_NONE)
                        onSetShuffleMode(shuffleMode)

                        if (shuffleMode == SHUFFLE_MODE_ALL) {
                            getCurrentQueueItem()?.let { currentQueueItem ->
                                playQueue.remove(currentQueueItem)
                                playQueue.shuffle()
                                playQueue.add(0, currentQueueItem)
                            }
                        } else {
                            playQueue.sortBy { it.queueId }
                        }

                        setPlayQueue()
                        cb?.send(SUCCESS, Bundle())
                    }
                }
                UPDATE_QUEUE_ITEM -> {
                    extras?.let {
                        val mediaDescription = MediaDescriptionCompat.Builder()
                            .setExtras(it.getBundle("extras"))
                            .setMediaId(it.getString("mediaId"))
                            .setSubtitle(it.getString("subtitle"))
                            .setTitle(it.getString("title"))
                            .build()
                        val queueItemId = it.getLong("queueItemId")
                        updateMetadataForQueueItem(mediaDescription, queueItemId)
                    }
                }
            }
        }

        override fun onSkipToQueueItem(id: Long) {
            super.onSkipToQueueItem(id)
            
            if (playQueue.find { it.queueId == id} != null) {
                val playbackState = mediaSessionCompat.controller.playbackState.state
                currentlyPlayingQueueItemId = id
                onPrepare()
                if (playbackState == STATE_PLAYING || playbackState == STATE_SKIPPING_TO_NEXT) {
                    onPlay()
                }
            }
        }

        override fun onSkipToPrevious() {
            super.onSkipToPrevious()

            if (playQueue.isNotEmpty()) {
                if (mediaPlayer != null && mediaPlayer!!.currentPosition > 5000 ||
                            currentlyPlayingQueueItemId == playQueue[0].queueId) onSeekTo(0L)
                else {
                    val indexOfCurrentQueueItem = playQueue.indexOfFirst {
                        it.queueId == currentlyPlayingQueueItemId
                    }
                    currentlyPlayingQueueItemId = playQueue[indexOfCurrentQueueItem - 1].queueId
                    onSkipToQueueItem(currentlyPlayingQueueItemId)
                }
            }
        }

        override fun onSkipToNext() {
            super.onSkipToNext()

            val repeatMode = mediaSessionCompat.controller.repeatMode
            currentlyPlayingQueueItemId = when {
                playQueue.isNotEmpty() && playQueue[playQueue.size - 1].queueId != currentlyPlayingQueueItemId -> {
                    val indexOfCurrentQueueItem = playQueue.indexOfFirst {
                        it.queueId == currentlyPlayingQueueItemId
                    }
                    playQueue[indexOfCurrentQueueItem + 1].queueId
                }
                // We are at the end of the queue. Check whether we should start over from the beginning
                repeatMode == REPEAT_MODE_ALL -> playQueue[0].queueId
                else -> return
            }

            onSkipToQueueItem(currentlyPlayingQueueItemId)
        }

        override fun onStop() {
            super.onStop()

            playQueue.clear()
            mediaSessionCompat.setQueue(playQueue)
            currentlyPlayingQueueItemId = -1L
            if (mediaPlayer != null) {
                mediaPlayer?.stop()
                mediaPlayer?.release()
                mediaPlayer = null
                stopForeground(true)
                try {
                    val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    audioManager.abandonAudioFocusRequest(audioFocusRequest)
                } catch (_: UninitializedPropertyAccessException){ }
            }
            setMediaPlaybackState(STATE_STOPPED, 0L, 0f, null)
            stopSelf()
        }

        override fun onSeekTo(pos: Long) {
            super.onSeekTo(pos)

            mediaPlayer?.apply {
                if (pos > this.duration.toLong()) return@apply

                val wasPlaying = this.isPlaying
                if (wasPlaying) this.pause()

                this.seekTo(pos.toInt())
                val playbackPosition = this.currentPosition.toLong()

                if (wasPlaying) {
                    this.start()
                    setMediaPlaybackState(STATE_PLAYING, playbackPosition,
                        1f, getBundleWithSongDuration())
                } else setMediaPlaybackState(STATE_PAUSED, playbackPosition,
                    0f, getBundleWithSongDuration())
            }
        }
    }

    /**
     * Generate a Bundle featuring the duration of the currently playing song. The bundle can be
     * packaged with media playback state updates.
     *
     * @return Bundle - containing a key called duration that holds an Integer representing the
     * duration of the currently playing song.
     */
    private fun getBundleWithSongDuration(): Bundle {
        val playbackDuration = mediaPlayer?.duration ?: 0
        return Bundle().apply {
            putInt("duration", playbackDuration)
        }
    }

    /**
     * Retrieves the QueueItem object for the currently playing song.
     *
     * @return QueueItem or null if no currently playing song can be found.
     */
    private fun getCurrentQueueItem(): QueueItem? {
        return playQueue.find {
            it.queueId == currentlyPlayingQueueItemId
        }
    }

    /**
     * Update the metadata for a given item in the play queue.
     *
     * @param mediaDescription - A MediaDescriptionCompat object containing the metadata for a given song.
     * @param queueId - The ID of the target queue item.
     */
    private fun updateMetadataForQueueItem(mediaDescription: MediaDescriptionCompat, queueId: Long) {
        val index = playQueue.indexOfFirst {
            it.queueId == queueId
        }

        if (index == -1) return

        playQueue.removeAt(index)
        val updatedQueueItem = QueueItem(mediaDescription, queueId)
        playQueue.add(index, updatedQueueItem)

        if (queueId == currentlyPlayingQueueItemId) {
            setCurrentMetadata()
            refreshNotification()
        }
        setPlayQueue()
    }

    /** Set the play queue for the media session and notify all observers of the playback state. */
    private fun setPlayQueue() {
        mediaSessionCompat.setQueue(playQueue)
        setMediaPlaybackState(STATE_NONE, 0, 0f, null)
    }

    override fun onCreate() {
        super.onCreate()

        mediaSessionCompat = MediaSessionCompat(baseContext, logTag).apply {
            setFlags(MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS)
            setCallback(mediaSessionCallback)
            setSessionToken(sessionToken)
            val builder = Builder().setActions(PlaybackStateCompat.ACTION_PLAY)
            setPlaybackState(builder.build())
        }
        initNoisyReceiver()
        playbackPositionChecker.run()
    }

    /** Handles playback becoming 'noisy' i.e. headphones being unplugged. */
    private fun initNoisyReceiver() {
        val filter = IntentFilter(ACTION_AUDIO_BECOMING_NOISY)
        registerReceiver(noisyReceiver, filter)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaSessionCompat.controller.transportControls.stop()
        handler.removeCallbacks(playbackPositionChecker)
        unregisterReceiver(noisyReceiver)
        mediaSessionCompat.release()
        NotificationManagerCompat.from(this).cancel(1)
    }

    /** Refresh the metadata displayed in the media player notification and handle user interactions. */
    private fun refreshNotification() {
        val isPlaying = mediaPlayer?.isPlaying ?: false
        val playPauseIntent = if (isPlaying) {
            Intent(applicationContext, MediaPlaybackService::class.java).setAction(ACTION_PAUSE)
        } else Intent(applicationContext, MediaPlaybackService::class.java).setAction(ACTION_PLAY)
        val nextIntent = Intent(applicationContext, MediaPlaybackService::class.java).setAction(ACTION_NEXT)
        val prevIntent = Intent(applicationContext, MediaPlaybackService::class.java).setAction(ACTION_PREVIOUS)

        val intent = packageManager
            .getLaunchIntentForPackage(packageName)
            ?.setPackage(null)
            ?.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        val activityIntent = PendingIntent.getActivity(applicationContext, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(applicationContext, channelID).apply {
            val mediaMetadata = mediaSessionCompat.controller.metadata

            // Previous button
            addAction(
                NotificationCompat.Action(R.drawable.ic_back, getString(R.string.play_prev),
                    PendingIntent.getService(applicationContext, 0, prevIntent, PendingIntent.FLAG_IMMUTABLE)
                )
            )

            // Play/pause button
            val playOrPause = if (isPlaying) R.drawable.ic_pause
            else R.drawable.ic_play
            addAction(
                NotificationCompat.Action(playOrPause, getString(R.string.play_pause),
                    PendingIntent.getService(applicationContext, 0, playPauseIntent, PendingIntent.FLAG_IMMUTABLE)
                )
            )

            // Next button
            addAction(
                NotificationCompat.Action(R.drawable.ic_next, getString(R.string.play_next),
                    PendingIntent.getService(applicationContext, 0, nextIntent, PendingIntent.FLAG_IMMUTABLE)
                )
            )

            setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(0, 1, 2)
                .setMediaSession(mediaSessionCompat.sessionToken)
            )

            val smallIcon = if (isPlaying) R.drawable.play
            else R.drawable.pause
            setSmallIcon(smallIcon)

            setContentIntent(activityIntent)

            // Add the metadata for the currently playing track
            setContentTitle(mediaMetadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE))
            setContentText(mediaMetadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST))
            setLargeIcon(mediaMetadata.getBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART))

            // Make the transport controls visible on the lockscreen
            setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            priority = NotificationCompat.PRIORITY_DEFAULT
        }
        // Display the notification and place the service in the foreground
        startForeground(1, builder.build())
    }

    /**
     * Dispatch media playback state updates.
     *
     * @param state - An Integer representing the current playback status.
     * @param position - The playback position in the currently playing song.
     * @param playbackSpeed - The speed of playback.
     * @param bundle - An option bundle of extras to be packaged with the playback status update.
     */
    private fun setMediaPlaybackState(state: Int, position: Long, playbackSpeed: Float, bundle: Bundle?) {
        val playbackStateBuilder = Builder()
            .setState(state, position, playbackSpeed)
            .setActiveQueueItemId(currentlyPlayingQueueItemId)
        if (bundle != null) playbackStateBuilder.setExtras(bundle)
        mediaSessionCompat.setPlaybackState(playbackStateBuilder.build())
    }

    /** Set the media session metadata to information about the currently playing song. */
    private fun setCurrentMetadata() {
        val currentQueueItem = getCurrentQueueItem() ?: return
        val currentQueueItemDescription = currentQueueItem.description
        val metadataBuilder= MediaMetadataCompat.Builder().apply {
            putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, currentQueueItemDescription.mediaId)
            putString(MediaMetadataCompat.METADATA_KEY_TITLE, currentQueueItemDescription.title.toString())
            putString(MediaMetadataCompat.METADATA_KEY_ARTIST, currentQueueItemDescription.subtitle.toString())
            val extras = currentQueueItemDescription.extras
            val albumName = extras?.getString("album") ?: "Unknown album"
            putString(MediaMetadataCompat.METADATA_KEY_ALBUM, albumName)
            val albumId = extras?.getString("album_id")
            putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, albumId)
            putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, getArtworkAsBitmap(albumId))
        }
        mediaSessionCompat.setMetadata(metadataBuilder.build())
    }

    /**
     * Retrieve the album artwork for a given album ID. If no artwork is found,
     * then a default artwork image is returned instead.
     *
     * @param albumId - The ID of the album that artwork should be retrieved for.
     * @return A Bitmap representation of the album artwork.
     */
    private fun getArtworkAsBitmap(albumId: String?) : Bitmap {
        if (albumId != null) {
            try {
                return BitmapFactory.Options().run {
                    inJustDecodeBounds = true
                    val contextWrapper = ContextWrapper(applicationContext)
                    val imageDirectory = contextWrapper.getDir("albumArt", Context.MODE_PRIVATE)
                    val imageFile = File(imageDirectory, "$albumId.jpg")
                    BitmapFactory.decodeStream(FileInputStream(imageFile))

                    inSampleSize = calculateInSampleSize(this)
                    inJustDecodeBounds = false
                    BitmapFactory.decodeStream(FileInputStream(imageFile))
                }
            } catch (_: FileNotFoundException) { }
        }
        // If an error has occurred or the album ID is null, then return a default artwork image
        return BitmapFactory.decodeResource(applicationContext.resources, R.drawable.no_album_artwork)
    }

    /**
     * Calculate an inSampleSize value that can be used to scale the
     * dimensions of a Bitmap image. Useful for compressing large images.
     *
     * @param options - The BitmapFactory.Options instance the should be used to
     * calculate the inSampleSize value for.
     * @return An integer inSampleSize value.
     */
    private fun calculateInSampleSize(options: BitmapFactory.Options): Int {
        val reqWidth = 100; val reqHeight = 100
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2; val halfWidth = width / 2

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    // Not important for general audio service, required for class
    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot? {
        return if (TextUtils.equals(clientPackageName, packageName)) {
            BrowserRoot(getString(R.string.app_name), null)
        } else null
    }

    //Not important for general audio service, required for class
    override fun onLoadChildren(parentId: String, result: Result<List<MediaBrowserCompat.MediaItem>>) {
        result.sendResult(null)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val actionString = intent.action
        if (actionString != null) {
            when (actionString) {
                ACTION_PLAY -> mediaSessionCallback.onPlay()
                ACTION_PAUSE -> mediaSessionCallback.onPause()
                ACTION_NEXT -> mediaSessionCallback.onSkipToNext()
                ACTION_PREVIOUS -> mediaSessionCallback.onSkipToPrevious()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    /** Handle errors that occur during playback */
    private fun error() {
        mediaSessionCompat.controller.transportControls.stop()
        stopForeground(true)
        Toast.makeText(application, getString(R.string.error), Toast.LENGTH_LONG).show()
    }

    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        error()
        return true
    }
}
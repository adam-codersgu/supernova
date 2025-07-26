package com.codersguidebook.supernova

import android.content.*
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioManager.*
import android.media.MediaPlayer.*
import android.os.*
import android.support.v4.media.session.MediaSessionCompat.QueueItem
import android.support.v4.media.session.PlaybackStateCompat.*
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.codersguidebook.supernova.utils.MediaItemHelper.getContentUri
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/**
 * DOCUMENTATION
 *
 * https://developer.android.com/media/implement/surfaces/mobile
 */
@UnstableApi
class MediaPlaybackService : MediaSessionService(), MediaSession.Callback {

    private var currentlyPlayingQueueItemId = -1L
    // private val handler = Handler(Looper.getMainLooper())
    //private var mediaPlayer: MediaPlayer? = null
    private val playQueue: MutableList<QueueItem> = mutableListOf()
    private lateinit var audioFocusRequest: AudioFocusRequest
    // private lateinit var mediaSessionCompat: MediaSessionCompat
    private lateinit var player: Player
    private var mediaSession: MediaSession? = null

    private val afChangeListener = OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AUDIOFOCUS_LOSS, AUDIOFOCUS_LOSS_TRANSIENT -> {
                player.pause()
            }
            AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> player.volume = 0.3f
            AUDIOFOCUS_GAIN -> player.volume = 1.0f
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            super.onPlayWhenReadyChanged(playWhenReady, reason)
            if (playWhenReady)
                requestAudioFocus()
        }
    }

    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (player.isPlaying) mediaSession!!.player.pause()
        }
    }

    /* private var playbackPositionRunnable = object : Runnable {
        override fun run() {
            try {
                if (mediaPlayer?.isPlaying == true) setMediaPlaybackState(STATE_PLAYING)
            } finally {
                handler.postDelayed(this, 1000L)
            }
        }
    }

    private val mediaSessionCallback: MediaSessionCompat.Callback = object : MediaSessionCompat.Callback() {
        override fun onMediaButtonEvent(mediaButtonEvent: Intent?): Boolean {
            val keyEvent: KeyEvent? = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                // Pre-SDK 33
                @Suppress("DEPRECATION")
                mediaButtonEvent?.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
            } else {
                // SDK 33 and up
                mediaButtonEvent?.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
            }

            keyEvent?.let { event ->
                when (event.keyCode) {
                    KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                        if (mediaPlayer?.isPlaying == true) onPause()
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
            val presetQueueId = description?.extras?.getLong("queue_id")
            val queueId = when {
                presetQueueId != null && sortedQueue.find { it.queueId == presetQueueId } == null -> {
                    presetQueueId
                }
                sortedQueue.isNotEmpty() -> sortedQueue[0].queueId + 1
                else -> 0
            }

            val queueItem = QueueItem(description, queueId)
            try {
                playQueue.add(index, queueItem)
            } catch (exception: IndexOutOfBoundsException) {
                playQueue.add(playQueue.size, queueItem)
            }

            mediaSession.setQueue(playQueue)
        }

        override fun onPrepare() {
            super.onPrepare()

            if (playQueue.isEmpty()) {
                onError(mediaPlayer, MEDIA_ERROR_UNKNOWN, MEDIA_ERROR_EMPTY_PLAY_QUEUE)
                return
            }

            // If no queue item ID has been set, then start from the beginning of the play queue
            if (currentlyPlayingQueueItemId == -1L) currentlyPlayingQueueItemId = playQueue[0].queueId

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
                    onError(mediaPlayer, MEDIA_ERROR_UNKNOWN, MEDIA_ERROR_MALFORMED)
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
                // Refresh the notification and metadata so user can see the song has changed
                setCurrentMetadata()
                refreshNotification()
                setMediaPlaybackState(STATE_NONE)
            } catch (_: IOException) {
                onError(mediaPlayer, MEDIA_ERROR_UNKNOWN, MEDIA_ERROR_IO)
            } catch (_: IllegalStateException) {
                onError(mediaPlayer, MEDIA_ERROR_UNKNOWN, MEDIA_ERROR_IO)
            } catch (_: IllegalArgumentException) {
                onError(mediaPlayer, MEDIA_ERROR_UNKNOWN, MEDIA_ERROR_MALFORMED)
            }
        }

        override fun onPlay() {
            super.onPlay()

            try {
                if (mediaPlayer != null && !mediaPlayer!!.isPlaying) {


                    if (audioFocusRequestOutcome == AUDIOFOCUS_REQUEST_GRANTED) {
                        startService(Intent(applicationContext, MediaBrowserService::class.java))
                        mediaSessionCompat.isActive = true
                        try {
                            mediaPlayer?.apply {
                                start()
                                setOnCompletionListener {
                                    val currentlyPlayingQueueItem = getCurrentQueueItem()
                                    currentlyPlayingQueueItem?.let {
                                        val currentlyPlayingSongId = it.description.mediaId
                                        if (currentlyPlayingSongId != null) {
                                            val bundle = Bundle().apply {
                                                putLong("finishedSongId", currentlyPlayingSongId.toLong())
                                            }
                                            setMediaPlaybackState(STATE_SKIPPING_TO_NEXT, bundle)
                                        }
                                    }

                                    val repeatMode = mediaSessionCompat.controller.repeatMode
                                    when {
                                        repeatMode == REPEAT_MODE_ONE -> {}
                                        repeatMode == REPEAT_MODE_ALL ||
                                                playQueue.isNotEmpty() &&
                                                playQueue[playQueue.size - 1].queueId
                                                != currentlyPlayingQueueItemId -> {
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
                            setMediaPlaybackState(STATE_PLAYING, getBundleWithSongDuration())
                        } catch (_: NullPointerException) {
                            onError(mediaPlayer, MEDIA_ERROR_UNKNOWN, 0)
                        }
                    }
                }
            } catch (_: IllegalStateException) {
                onError(mediaPlayer, MEDIA_ERROR_UNKNOWN, MEDIA_ERROR_IO)
            }
        }

        override fun onPause() {
            super.onPause()
            mediaPlayer?.pause()
            setMediaPlaybackState(STATE_PAUSED, getBundleWithSongDuration())
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
                        mediaSessionCompat.setQueue(playQueue)
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
                    }
                }
                UPDATE_QUEUE_ITEM -> {
                    extras?.let {
                        val mediaDescription = buildMediaDescriptionFromBundle(it)
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

            val repeatMode = mediaSession.controller.repeatMode
            onSkipToQueueItem(when {
                playQueue.isNotEmpty() &&
                        playQueue[playQueue.size - 1].queueId != currentlyPlayingQueueItemId -> {
                    val indexOfCurrentQueueItem = playQueue.indexOfFirst {
                        it.queueId == currentlyPlayingQueueItemId
                    }
                    playQueue[indexOfCurrentQueueItem + 1].queueId
                }
                // We are at the end of the queue. Check whether we should start over from the beginning
                repeatMode == REPEAT_MODE_ALL -> playQueue[0].queueId
                else -> return
            })
        }

        override fun onStop() {
            super.onStop()

            playQueue.clear()
            mediaSession.setQueue(playQueue)
            currentlyPlayingQueueItemId = -1L
            if (mediaPlayer != null) {
                mediaPlayer?.stop()
                mediaPlayer?.release()
                mediaPlayer = null
                stopForeground(STOP_FOREGROUND_REMOVE)
                try {
                    val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    audioManager.abandonAudioFocusRequest(audioFocusRequest)
                } catch (_: UninitializedPropertyAccessException){ }
            }
            setMediaPlaybackState(STATE_STOPPED)
            stopSelf()
        }

        override fun onSeekTo(pos: Long) {
            super.onSeekTo(pos)

            mediaPlayer?.apply {
                if (pos > this.duration.toLong()) return@apply

                val wasPlaying = this.isPlaying
                if (wasPlaying) this.pause()

                this.seekTo(pos.toInt())

                if (wasPlaying) {
                    this.start()
                    setMediaPlaybackState(STATE_PLAYING, getBundleWithSongDuration())
                } else setMediaPlaybackState(STATE_PAUSED, getBundleWithSongDuration())
            }
        }
    } */

    override fun onCreate() {
        super.onCreate()

        /* mediaSessionCompat = MediaSessionCompat(baseContext, NOTIFICATION_CHANNEL_ID).apply {
            setFlags(MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS)
            setCallback(mediaSessionCallback)
            setSessionToken(sessionToken)
            val builder = Builder().setActions(PlaybackStateCompat.ACTION_PLAY)
            setPlaybackState(builder.build())
        } */
        player = ExoPlayer.Builder(this).build().also { it.addListener(playerListener) }
        mediaSession = MediaSession.Builder(this, player).setCallback(this).build()
        initNoisyReceiver()
        // playbackPositionRunnable.run()
    }

    private fun requestAudioFocus() {
        audioFocusRequest = AudioFocusRequest.Builder(AUDIOFOCUS_GAIN).run {
            setAudioAttributes(AudioAttributes.Builder().run {
                setOnAudioFocusChangeListener(afChangeListener)
                setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                build()
            })
            build()
        }
        val audioManager = applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        /* val audioFocusRequestOutcome = */ audioManager.requestAudioFocus(audioFocusRequest)
    }

    override fun onMediaButtonEvent(
        session: MediaSession,
        controllerInfo: MediaSession.ControllerInfo,
        intent: Intent
    ): Boolean {
        Log.i("DEBUG", "An intent received with action ${intent.action}")
        return super.onMediaButtonEvent(session, controllerInfo, intent)
    }

    override fun onAddMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: MutableList<MediaItem>
    ): ListenableFuture<MutableList<MediaItem>> {
        val updatedMediaItems = mediaItems.map {
            val uri = getContentUri(it.mediaId)
            it.buildUpon().setUri(uri).build()
        }.toMutableList()
        return Futures.immediateFuture(updatedMediaItems)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession


    /** Handles playback becoming 'noisy' i.e. headphones being unplugged. */
    private fun initNoisyReceiver() {
        val filter = IntentFilter(ACTION_AUDIO_BECOMING_NOISY)
        registerReceiver(noisyReceiver, filter)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaSession?.run {
            release()
            player.release()
            mediaSession = null
        }
        unregisterReceiver(noisyReceiver)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        val player = mediaSession?.player
        if (player!!.playWhenReady) {
            player.pause()
        }
        stopSelf()
    }


    /* override fun onDestroy() {
        super.onDestroy()
        mediaSessionCompat.controller.transportControls.stop()
        handler.removeCallbacks(playbackPositionRunnable)
        unregisterReceiver(noisyReceiver)
        mediaSessionCompat.release()
        NotificationManagerCompat.from(this).cancel(1)
    } */

    /** Refresh the metadata displayed in the media player notification and handle user interactions. */
    /* private fun refreshNotification() {
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

        val builder = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID).apply {
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
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.TIRAMISU) {
            // Pre-SDK 34
            startForeground(1, builder.build())
        } else {
            // SDK 34 and up
            startForeground(1, builder.build(), FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        }
    } */

    /**
     * Dispatch media playback state updates.
     *
     * @param state An Integer representing the current playback status.
     * @param bundle An option bundle of extras to be packaged with the playback status update.
     * Default = null.
     */
    /* private fun setMediaPlaybackState(state: Int, bundle: Bundle? = null) {
        val playbackPosition = mediaPlayer?.currentPosition?.toLong() ?: 0L
        val playbackSpeed = mediaPlayer?.playbackParams?.speed ?: 0f
        val playbackStateBuilder = Builder()
            .setState(state, playbackPosition, playbackSpeed)
            .setActiveQueueItemId(currentlyPlayingQueueItemId)
        bundle?.let { playbackStateBuilder.setExtras(it) }
        mediaSessionCompat.setPlaybackState(playbackStateBuilder.build())
    } */

    /** Set the media session metadata to information about the currently playing song. */
    /* private fun setCurrentMetadata() {
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
            putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, getArtworkByAlbumId(albumId))
            putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, albumId)
        }
        mediaSessionCompat.setMetadata(metadataBuilder.build())
    } */

    /**
     * Retrieve the album artwork stored by the app for a given album ID.
     * If no artwork is found then a default artwork image is returned instead.
     *
     * @param albumId The ID of the album that artwork should be retrieved for.
     * @return A Bitmap representation of the album artwork.
     */
    /* private fun getArtworkByAlbumId(albumId: String?): Bitmap {
        albumId?.let {
            try {
                val directory = ContextWrapper(applicationContext).getDir("albumArt", Context.MODE_PRIVATE)
                val imageFile = File(directory, "$albumId.jpg")
                if (imageFile.exists()) {
                    return BitmapFactory.decodeStream(FileInputStream(imageFile))
                }
            } catch (_: Exception) { }
        }
        // If an error has occurred or the album ID is null, then return a default artwork image
        return BitmapFactory.decodeResource(applicationContext.resources, R.drawable.no_album_artwork)
    }

    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        setMediaPlaybackState(STATE_ERROR)
        mediaSessionCompat.controller.transportControls.stop()
        stopForeground(STOP_FOREGROUND_REMOVE)

        val message = when (extra) {
            MEDIA_ERROR_EMPTY_PLAY_QUEUE -> {
                getString(R.string.error_media_service_empty_queue)
            }
            MEDIA_ERROR_IO -> getString(R.string.error_media_service_player_state)
            else -> getString(R.string.error_media_service_default)
        }
        Toast.makeText(application, message, Toast.LENGTH_LONG).show()

        return true
    } */
}
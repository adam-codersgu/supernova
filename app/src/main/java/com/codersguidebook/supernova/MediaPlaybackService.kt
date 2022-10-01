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
import android.service.media.MediaBrowserService
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.MediaSessionCompat.QueueItem
import android.support.v4.media.session.PlaybackStateCompat
import android.text.TextUtils
import android.view.KeyEvent
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.lifecycleScope
import androidx.media.MediaBrowserServiceCompat
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException

class MediaPlaybackService : MediaBrowserServiceCompat(), MediaPlayer.OnErrorListener {

    private val channelID = "supernova"
    private var currentlyPlayingQueueItemId = 0L
    private val logTag = "AudioPlayer"
    private val handler = Handler(Looper.getMainLooper())
    private var mediaPlayer: MediaPlayer? = null
    private val playQueue: MutableList<QueueItem> = mutableListOf()
    private lateinit var audioFocusRequest: AudioFocusRequest
    private lateinit var mediaSessionCompat: MediaSessionCompat

    private val afChangeListener = OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AUDIOFOCUS_LOSS -> {
                mMediaSessionCallback.onPause()
            }
            AUDIOFOCUS_LOSS_TRANSIENT -> {
                mMediaSessionCallback.onPause()
            }
            AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                if (mediaPlayer!!.isPlaying) mediaPlayer!!.setVolume(0.3f, 0.3f)
            }
            AUDIOFOCUS_GAIN -> {
                if (mediaPlayer != null) mediaPlayer!!.setVolume(1.0f, 1.0f)
            }
        }
    }

    private val mNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(
            context: Context,
            intent: Intent
        ) {
            if (mediaPlayer != null && mediaPlayer!!.isPlaying) mMediaSessionCallback.onPause()
        }
    }

    private var playbackPositionChecker = object : Runnable {
        override fun run() {
            try {
                if (mediaPlayer != null && mediaPlayer!!.isPlaying) {
                    val playbackPosition = mediaPlayer!!.currentPosition.toLong()
                    setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING, playbackPosition, 1f, null)
                }
            } finally {
                handler.postDelayed(this, 1000L)
            }
        }
    }

    private val mMediaSessionCallback: MediaSessionCompat.Callback = object : MediaSessionCompat.Callback() {
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
            val highestQueueId = if (sortedQueue.isNotEmpty()) sortedQueue[0].queueId
            else -1

            val queueItem = QueueItem(description, highestQueueId + 1)
            playQueue.add(index, queueItem)
            // TODO: Do we somehow need to update MainActivity here that a song has been added to the play queue e.g. through a state update
            // TODO: Yes, send a STATE_NONE update
        }

        /*
        TODO: Play new songs pathway should be add songs -> onPrepare -> onPlay
         */
        override fun onPrepare() {
            super.onPrepare()
            if (playQueue.isEmpty()) {
                error()
                return
            }

            // If no alternative currently play queue item ID has been set, then play from the beginning of the queue
            if (currentlyPlayingQueueItemId == 0L) currentlyPlayingQueueItemId = playQueue[0].queueId

            setCurrentMetadata()

            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer.create(applicationContext, 1)
            } else {
                mediaPlayer!!.apply {
                    stop()
                    release()
                }
            }

            try {
                val currentQueueItem = getCurrentQueueItem()
                val currentQueueItemUri = currentQueueItem?.description?.mediaUri
                if (currentQueueItemUri == null) {
                    error()
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
                val bundle = Bundle()
                bundle.putLong("currentQueueItemId", currentQueueItem.queueId)
                setMediaPlaybackState(PlaybackStateCompat.STATE_SKIPPING_TO_QUEUE_ITEM, 0,
                    0f, bundle)
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
            if (mediaPlayer != null) {
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
                                        val bundle = Bundle()
                                        bundle.putLong("finishedSongId", currentlyPlayingSongId.toLong())
                                        setMediaPlaybackState(PlaybackStateCompat.STATE_SKIPPING_TO_NEXT, 0,
                                            0f, bundle)
                                    }
                                }

                                val repeatMode = mediaSessionCompat.controller.repeatMode
                                when {
                                    repeatMode == PlaybackStateCompat.REPEAT_MODE_ONE -> {}
                                    playQueue.isNotEmpty() && playQueue[playQueue.size - 1].queueId != currentlyPlayingQueueItemId -> {
                                        val indexOfCurrentQueueItem = playQueue.indexOfFirst {
                                            it.queueId == currentlyPlayingQueueItemId
                                        }
                                        currentlyPlayingQueueItemId = playQueue[indexOfCurrentQueueItem + 1].queueId
                                    }
                                    // We have reached the end of the queue. Check whether we should start over from the beginning
                                    repeatMode == PlaybackStateCompat.REPEAT_MODE_ALL -> currentlyPlayingQueueItemId = playQueue[0].queueId
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
                        setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING, playbackPosition,
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
            setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED, playbackPosition,
                0f, getBundleWithSongDuration())
            refreshNotification()
        }

        override fun onFastForward() {
            super.onFastForward()
            // TODO: Experiment using mediaPlayer!!.playbackParams
            // TODO: Likewise for onFastRewind()
        }

        override fun onSetRepeatMode(repeatMode: Int) {
            super.onSetRepeatMode(repeatMode)
            // TODO: Implement
        }

        override fun onSetShuffleMode(shuffleMode: Int) {
            super.onSetShuffleMode(shuffleMode)
            // TODO: Implement
        }

        override fun onSkipToNext() {
            super.onSkipToNext()

            if (mediaSessionCompat.mediaSession)
                mediaSessionCompat.controller

            val bundle = Bundle()
            bundle.putBoolean("finished", false)
            setMediaPlaybackState(PlaybackStateCompat.STATE_SKIPPING_TO_NEXT, 0, 0f, bundle)
        }

        override fun onSkipToQueueItem(id: Long) {
            super.onSkipToQueueItem(id)
            if (playQueue.find { it.queueId == id} != null) {
                val isPlaying = mediaPlayer?.isPlaying ?: false
                currentlyPlayingQueueItemId = id
                onPrepare()
                if (isPlaying) onPlay()
            }
        }

        override fun onSkipToPrevious() {
            super.onSkipToPrevious()

            if (playQueue.isNotEmpty() && currentlyPlayingQueueItemId != playQueue[0].queueID) {
                val index = playQueue.indexOfFirst {
                    it.queueID == currentlyPlayingQueueItemId
                }
                currentlyPlayingQueueItemId = playQueue[index - 1].queueID
                lifecycleScope.launch {
                    updateCurrentlyPlaying()
                    if (pbState == PlaybackStateCompat.STATE_PLAYING) play()
                }
            }

            // currentPosition returns playback position in milliseconds
            // if the media player is more than 5 seconds into song then restart song, otherwise, skip back to previous song (if possible)
            if (mediaPlayer != null && mediaPlayer!!.currentPosition > 5000) onSeekTo(0)
            else setMediaPlaybackState(PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS, 0, 0f, null)
        }

        override fun onStop() {
            super.onStop()
            playQueue.clear()
            currentlyPlayingQueueItemId = 0
            if (mediaPlayer != null) {
                mediaPlayer!!.stop()
                mediaPlayer!!.release()
                mediaPlayer = null
                stopForeground(true)
                try {
                    val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    audioManager.abandonAudioFocusRequest(audioFocusRequest)
                } catch (_: UninitializedPropertyAccessException){ }
            }
            setMediaPlaybackState(PlaybackStateCompat.STATE_STOPPED, 0L, 0f, null)
            handler.removeCallbacks(playbackPositionChecker)
            stopSelf()
        }

        override fun onSeekTo(pos: Long) {
            super.onSeekTo(pos)
            try {
                if (mediaPlayer!!.isPlaying) {
                    mediaPlayer!!.pause()
                    mediaPlayer!!.seekTo(pos.toInt())
                    mediaPlayer!!.start()
                    val playbackPosition = mediaPlayer!!.currentPosition.toLong()
                    setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING, playbackPosition, 1f, null)
                } else {
                    mediaPlayer!!.seekTo(pos.toInt())
                    val playbackPosition = mediaPlayer!!.currentPosition.toLong()
                    setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED, playbackPosition, 0f, null)
                }
            } catch (_: NullPointerException) { }
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
        val bundle = Bundle()
        bundle.putInt("duration", playbackDuration)
        return bundle
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

    override fun onCreate() {
        super.onCreate()

        mediaSessionCompat = MediaSessionCompat(baseContext, logTag).apply {
            setCallback(mMediaSessionCallback)
            setSessionToken(sessionToken)
            val builder: PlaybackStateCompat.Builder = PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY)
            setPlaybackState(builder.build())
        }
        initNoisyReceiver()
        playbackPositionChecker.run()
    }

    private fun initNoisyReceiver() {
        // Handles headphones coming unplugged
        val filter = IntentFilter(ACTION_AUDIO_BECOMING_NOISY)
        registerReceiver(mNoisyReceiver, filter)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(mNoisyReceiver)
        mMediaSessionCallback.onStop()
        mediaSessionCompat.release()
        NotificationManagerCompat.from(this).cancel(1)
    }

    private fun refreshNotification() {
        val isPlaying = mediaPlayer?.isPlaying ?: false
        val playPauseIntent = if (isPlaying) Intent(applicationContext, MediaPlaybackService::class.java).setAction("ACTION_PAUSE")
        else Intent(applicationContext, MediaPlaybackService::class.java).setAction("ACTION_PLAY")
        val nextIntent = Intent(applicationContext, MediaPlaybackService::class.java).setAction("ACTION_NEXT")
        val prevIntent = Intent(applicationContext, MediaPlaybackService::class.java).setAction("ACTION_PREVIOUS")

        val intent = packageManager
            .getLaunchIntentForPackage(packageName)
            ?.setPackage(null)
            ?.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        val activityIntent = PendingIntent.getActivity(applicationContext, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(applicationContext, channelID).apply {
            val mediaMetadata = mediaSessionCompat.controller.metadata

            // previous button
            addAction(
                NotificationCompat.Action(
                    R.drawable.ic_back,
                    getString(R.string.play_prev),
                    PendingIntent.getService(applicationContext, 0, prevIntent, PendingIntent.FLAG_IMMUTABLE)
                )
            )

            // play/pause button
            val playOrPause = if (isPlaying) R.drawable.ic_pause
            else R.drawable.ic_play
            addAction(
                NotificationCompat.Action(
                    playOrPause,
                    getString(R.string.play_pause),
                    PendingIntent.getService(applicationContext, 0, playPauseIntent, PendingIntent.FLAG_IMMUTABLE)
                )
            )

            // next button
            addAction(
                NotificationCompat.Action(
                    R.drawable.ic_next,
                    getString(R.string.play_next),
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

    private fun setMediaPlaybackState(state: Int, position: Long, playbackSpeed: Float, bundle: Bundle?) {
        val playbackStateBuilder = PlaybackStateCompat.Builder()
        playbackStateBuilder.setState(state, position, playbackSpeed)
        if (bundle != null) playbackStateBuilder.setExtras(bundle)
        mediaSessionCompat.setPlaybackState(playbackStateBuilder.build())
    }

    /**
     * Set the media session metadata to information about the currently playing song.
     *
     */
    private fun setCurrentMetadata() {
        val currentQueueItem = getCurrentQueueItem() ?: return
        val currentQueueItemDescription = currentQueueItem.description
        val metadataBuilder= MediaMetadataCompat.Builder().apply {
            putString(MediaMetadataCompat.METADATA_KEY_TITLE,currentQueueItemDescription.title.toString())
            putString(MediaMetadataCompat.METADATA_KEY_ARTIST, currentQueueItemDescription.subtitle.toString())
            val extras = currentQueueItemDescription.extras
            val album = extras?.getString("album") ?: "Unknown album"
            putString(MediaMetadataCompat.METADATA_KEY_ALBUM, album)
            putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, currentQueueItemDescription.iconBitmap)
        }
        mediaSessionCompat.setMetadata(metadataBuilder.build())
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
                // TODO: Are these actions stored as params anywhere? Would then need to update refreshNotification() also
                "ACTION_PLAY" -> mMediaSessionCallback.onPlay()
                "ACTION_PAUSE" -> mMediaSessionCallback.onPause()
                "ACTION_NEXT" -> mMediaSessionCallback.onSkipToNext()
                "ACTION_PREVIOUS" -> mMediaSessionCallback.onSkipToPrevious()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    /**
     * Handle errors that occur during playback
     *
     * TODO - Could include a parameter that gives a description for different errors. This could be printed in the Toast.
     */
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
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
import android.net.Uri
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
import androidx.media.MediaBrowserServiceCompat
import com.codersguidebook.supernova.entities.Song
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException

class MediaPlaybackService : MediaBrowserServiceCompat(), MediaPlayer.OnErrorListener {

    private val channelID = "supernova"
    private var currentlyPlayingSong: Song? = null
    private val logTag = "AudioPlayer"
    private val handler = Handler(Looper.getMainLooper())
    private var mMediaPlayer: MediaPlayer? = null
    private val playQueue: MutableList<QueueItem> = mutableListOf()
    private lateinit var audioFocusRequest: AudioFocusRequest
    private lateinit var mMediaSessionCompat: MediaSessionCompat

    private val afChangeListener = OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AUDIOFOCUS_LOSS -> {
                mMediaSessionCallback.onPause()
            }
            AUDIOFOCUS_LOSS_TRANSIENT -> {
                mMediaSessionCallback.onPause()
            }
            AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                if (mMediaPlayer!!.isPlaying) mMediaPlayer!!.setVolume(0.3f, 0.3f)
            }
            AUDIOFOCUS_GAIN -> {
                if (mMediaPlayer != null) mMediaPlayer!!.setVolume(1.0f, 1.0f)
            }
        }
    }

    private val mNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(
            context: Context,
            intent: Intent
        ) {
            if (mMediaPlayer != null && mMediaPlayer!!.isPlaying) mMediaSessionCallback.onPause()
        }
    }

    private var playbackPositionChecker = object : Runnable {
        override fun run() {
            try {
                if (mMediaPlayer != null && mMediaPlayer!!.isPlaying) {
                    val playbackPosition = mMediaPlayer!!.currentPosition.toLong()
                    setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING, playbackPosition, 1f, null)
                }
            } finally {
                handler.postDelayed(this, 1000L)
            }
        }
    }

    private val mMediaSessionCallback: MediaSessionCompat.Callback = object : MediaSessionCompat.Callback() {
        override fun onMediaButtonEvent(mediaButtonEvent: Intent?): Boolean {
            val ke: KeyEvent? = mediaButtonEvent?.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
            if (ke != null && mMediaPlayer != null) {
                when (ke.keyCode) {
                    KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                        if (mMediaPlayer!!.isPlaying) onPause()
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

        /**
         * Add a song to the end of the play queue.
         *
         * @param description - A MediaDescriptionCompat object containing metadata for a given song.
         */
        override fun onAddQueueItem(description: MediaDescriptionCompat?) {
            onAddQueueItem(description, playQueue.size)
        }

        /**
         * Add a song at a given index in the play queue.
         *
         * @param description - A MediaDescriptionCompat object containing metadata for a given song.
         * @param index - The index that the song should be added in the play queue.
         */
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
        }

        override fun onPrepareFromUri(uri: Uri?, extras: Bundle?) {
            super.onPrepareFromUri(uri, extras)
            val bundle = extras!!.getString("song")
            val type = object : TypeToken<Song>() {}.type
            currentlyPlayingSong = Gson().fromJson(bundle, type)
            setCurrentMetadata()

            if (mMediaPlayer == null) {
                mMediaPlayer = MediaPlayer.create(applicationContext, 1)
                mMediaPlayer!!.isLooping = true
                mMediaPlayer!!.start()
            } else mMediaPlayer!!.start()

            if (mMediaPlayer != null) mMediaPlayer!!.release()
            try {
                mMediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                    )
                    setDataSource(application, Uri.parse(currentlyPlayingSong!!.uri))
                    setOnErrorListener(this@MediaPlaybackService)
                    prepare()
                    // refresh notification so user can see song has changed
                    showNotification(false)
                }
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
            if (currentlyPlayingSong != null){
                val am = applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                // Request audio focus for playback
                audioFocusRequest = AudioFocusRequest.Builder(AUDIOFOCUS_GAIN).run {
                    setAudioAttributes(AudioAttributes.Builder().run {
                        setOnAudioFocusChangeListener(afChangeListener)
                        setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        build()
                    })
                    build()
                }
                val result = am.requestAudioFocus(audioFocusRequest)
                if (result == AUDIOFOCUS_REQUEST_GRANTED) {
                    // Start the service
                    startService(Intent(applicationContext, MediaBrowserService::class.java))
                    // Set the session active
                    mMediaSessionCompat.isActive = true
                    showNotification(true)
                    try {
                        mMediaPlayer!!.start()
                        val playbackPosition = mMediaPlayer!!.currentPosition.toLong()
                        val playbackDuration = mMediaPlayer!!.duration
                        val bundle = Bundle()
                        bundle.putInt("duration", playbackDuration)
                        setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING, playbackPosition, 1f, bundle)
                        mMediaPlayer!!.setOnCompletionListener {
                            val completedBundle = Bundle()
                            completedBundle.putBoolean("finished", true)
                            setMediaPlaybackState(PlaybackStateCompat.STATE_SKIPPING_TO_NEXT, 0, 0f, completedBundle)
                        }
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
            if (mMediaPlayer != null && mMediaPlayer!!.isPlaying) {
                mMediaPlayer!!.pause()
                val playbackPosition = mMediaPlayer!!.currentPosition.toLong()
                val playbackDuration = mMediaPlayer!!.duration
                val bundle = Bundle()
                bundle.putInt("duration", playbackDuration)
                setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED, playbackPosition, 0f, bundle)
                showNotification(false)
            }
        }

        override fun onSkipToNext() {
            super.onSkipToNext()

            val bundle = Bundle()
            bundle.putBoolean("finished", false)
            setMediaPlaybackState(PlaybackStateCompat.STATE_SKIPPING_TO_NEXT, 0, 0f, bundle)
        }

        override fun onSkipToPrevious() {
            super.onSkipToPrevious()

            // currentPosition returns playback position in milliseconds
            // if the media player is more than 5 seconds into song then restart song, otherwise, skip back to previous song (if possible)
            if (mMediaPlayer != null && mMediaPlayer!!.currentPosition > 5000) onSeekTo(0)
            else setMediaPlaybackState(PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS, 0, 0f, null)
        }

        override fun onStop() {
            super.onStop()
            playQueue.clear()
            stopPlayback()
            setMediaPlaybackState(PlaybackStateCompat.STATE_STOPPED, 0L, 0f, null)
            handler.removeCallbacks(playbackPositionChecker)
            stopSelf()
        }

        override fun onSeekTo(pos: Long) {
            super.onSeekTo(pos)
            try {
                if (mMediaPlayer!!.isPlaying) {
                    mMediaPlayer!!.pause()
                    mMediaPlayer!!.seekTo(pos.toInt())
                    mMediaPlayer!!.start()
                    val playbackPosition = mMediaPlayer!!.currentPosition.toLong()
                    setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING, playbackPosition, 1f, null)
                } else {
                    mMediaPlayer!!.seekTo(pos.toInt())
                    val playbackPosition = mMediaPlayer!!.currentPosition.toLong()
                    setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED, playbackPosition, 0f, null)
                }
            } catch (_: NullPointerException) { }
        }
    }

    private fun stopPlayback() {
        if (mMediaPlayer != null) {
            mMediaPlayer!!.stop()
            mMediaPlayer!!.release()
            mMediaPlayer = null
            currentlyPlayingSong = null
            stopForeground(true)
            try {
                val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                audioManager.abandonAudioFocusRequest(audioFocusRequest)
            } catch (_: UninitializedPropertyAccessException){ }
        }
    }

    override fun onCreate() {
        super.onCreate()

        mMediaSessionCompat = MediaSessionCompat(baseContext, logTag).apply {
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
        mMediaSessionCompat.release()
        NotificationManagerCompat.from(this).cancel(1)
    }

    private fun showNotification(isPlaying: Boolean) {
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
            // Get the session's metadata
            val controller = mMediaSessionCompat.controller
            val mediaMetadata = controller.metadata

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
                .setMediaSession(mMediaSessionCompat.sessionToken)
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
        mMediaSessionCompat.setPlaybackState(playbackStateBuilder.build())
    }

    private fun setCurrentMetadata() {
        val metadataBuilder= MediaMetadataCompat.Builder().apply {
            putString(
                MediaMetadataCompat.METADATA_KEY_TITLE,
                currentlyPlayingSong?.title
            )
            putString(
                MediaMetadataCompat.METADATA_KEY_ARTIST,
                currentlyPlayingSong?.artist
            )
            putString(
                MediaMetadataCompat.METADATA_KEY_ALBUM,
                currentlyPlayingSong?.albumName
            )
            putBitmap(
                MediaMetadataCompat.METADATA_KEY_ALBUM_ART,
                getArtwork(currentlyPlayingSong?.albumId) ?: BitmapFactory.decodeResource(application.resources, R.drawable.no_album_artwork)
            )
        }
        mMediaSessionCompat.setMetadata(metadataBuilder.build())
    }

    @Deprecated("Should retrieve the artwork from MediaSession.QueueItem.getDescription.getIconBitmap")
    private fun getArtwork(albumArtwork: String?) : Bitmap? {
        // set album artwork on player controls
        try {
            return BitmapFactory.Options().run {
                inJustDecodeBounds = true
                val cw = ContextWrapper(applicationContext)
                val directory = cw.getDir("albumArt", Context.MODE_PRIVATE)
                val f = File(directory, "$albumArtwork.jpg")
                BitmapFactory.decodeStream(FileInputStream(f))

                // Calculate inSampleSize. width and height are in pixels
                inSampleSize = calculateInSampleSize(this)

                // Decode bitmap with inSampleSize set
                inJustDecodeBounds = false

                BitmapFactory.decodeStream(FileInputStream(f))
            }
        } catch (e: FileNotFoundException) { }
        return null
    }

    @Deprecated("Should retrieve the artwork from MediaSession.QueueItem.getDescription.getIconBitmap")
    private fun calculateInSampleSize(options: BitmapFactory.Options): Int {
        val reqWidth = 100
        val reqHeight = 100
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {

            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    //Not important for general audio service, required for class
    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
        return if (TextUtils.equals(clientPackageName, packageName)) {
            BrowserRoot(getString(R.string.app_name), null)
        } else null
    }

    //Not important for general audio service, required for class
    override fun onLoadChildren(
        parentId: String,
        result: Result<List<MediaBrowserCompat.MediaItem>>
    ) {
        result.sendResult(null)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val actionString = intent.action
        if (actionString != null) {
            when (actionString) {
                "ACTION_PLAY" -> mMediaSessionCallback.onPlay()
                "ACTION_PAUSE" -> mMediaSessionCallback.onPause()
                "ACTION_NEXT" -> mMediaSessionCallback.onSkipToNext()
                "ACTION_PREVIOUS" -> mMediaSessionCallback.onSkipToPrevious()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun error() {
        mMediaPlayer = null
        mMediaPlayer?.reset()
        setMediaPlaybackState(PlaybackStateCompat.STATE_STOPPED, 0L, 0f, null)
        currentlyPlayingSong = null
        stopForeground(true)
        Toast.makeText(application, getString(R.string.error), Toast.LENGTH_LONG).show()
    }

    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        error()
        return true
    }
}
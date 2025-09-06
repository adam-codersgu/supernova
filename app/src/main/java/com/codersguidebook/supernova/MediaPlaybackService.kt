package com.codersguidebook.supernova

import android.content.*
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioManager.*
import android.media.MediaPlayer.*
import android.os.*
import android.provider.MediaStore
import android.support.v4.media.session.PlaybackStateCompat.*
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/**
 * DOCUMENTATION
 *
 * https://developer.android.com/media/implement/surfaces/mobile
 */
@UnstableApi
class MediaPlaybackService : MediaSessionService(), MediaSession.Callback {

    private lateinit var audioFocusRequest: AudioFocusRequest
    private lateinit var player: Player

    private var mediaSession: MediaSession? = null

    private val afChangeListener = OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AUDIOFOCUS_LOSS, AUDIOFOCUS_LOSS_TRANSIENT -> player.pause()
            AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> player.volume = 0.3f
            AUDIOFOCUS_GAIN -> player.volume = 1.0f
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            super.onPlayWhenReadyChanged(playWhenReady, reason)
            if (playWhenReady) requestAudioFocus()
        }
    }

    override fun onCreate() {
        super.onCreate()

        player = ExoPlayer.Builder(this).build().also {
            it.addListener(playerListener)
            it.setHandleAudioBecomingNoisy(true)
        }
        mediaSession = MediaSession.Builder(this, player).setCallback(this).build()
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
        audioManager.requestAudioFocus(audioFocusRequest)
    }

    override fun onAddMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: MutableList<MediaItem>
    ): ListenableFuture<MutableList<MediaItem>> {
        val updatedMediaItems = mediaItems.map {
            val uri = ContentUris.withAppendedId(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, it.mediaId.toLong())
            it.buildUpon().setUri(uri).build()
        }.toMutableList()
        return Futures.immediateFuture(updatedMediaItems)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = mediaSession

    override fun onDestroy() {
        super.onDestroy()
        mediaSession?.run {
            release()
            player.release()
            mediaSession = null
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        val player = mediaSession?.player
        if (player!!.playWhenReady) {
            player.pause()
        }
        stopSelf()
    }

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
    } */
}
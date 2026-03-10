package com.codersguidebook.supernova

import android.app.PendingIntent
import android.content.ContentUris
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioManager.AUDIOFOCUS_GAIN
import android.media.AudioManager.AUDIOFOCUS_LOSS
import android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT
import android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK
import android.media.AudioManager.OnAudioFocusChangeListener
import android.net.Uri
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.MediaStyleNotificationHelper
import com.codersguidebook.supernova.params.MediaServiceConstants.Companion.ALBUM_ID
import com.codersguidebook.supernova.params.MediaServiceConstants.Companion.NOTIFICATION_CHANNEL_ID
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import java.io.File

/**
 * DOCUMENTATION
 *
 * https://developer.android.com/media/implement/surfaces/mobile
 */
@UnstableApi
class MediaPlaybackService : MediaSessionService(), MediaSession.Callback {

    private lateinit var audioFocusRequest: AudioFocusRequest
    private lateinit var player: ForwardingPlayer

    private lateinit var artworkDirectory: File
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

        artworkDirectory = ContextWrapper(applicationContext).getDir("albumArt", Context.MODE_PRIVATE)

        val basePlayer = ExoPlayer.Builder(this).build().also {
            it.addListener(playerListener)
            it.setHandleAudioBecomingNoisy(true)
        }
        player = object : ForwardingPlayer(basePlayer) {
            override fun getAvailableCommands(): Player.Commands {
                return super.getAvailableCommands().buildUpon()
                    .add(Player.COMMAND_SEEK_TO_NEXT)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                    .build()
            }

            override fun isCommandAvailable(@Player.Command command: Int): Boolean {
                if (command == Player.COMMAND_SEEK_TO_NEXT) return true
                return super.isCommandAvailable(command)
            }
        }

        val intent = packageManager
            .getLaunchIntentForPackage(packageName)
            ?.setPackage(null)
            ?.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        val activityIntent = PendingIntent.getActivity(applicationContext, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        mediaSession = MediaSession.Builder(this, player).setCallback(this).setSessionActivity(activityIntent).build()

        // TODO USE BUILDER, CREATE NOTIFICATION AND SETTER METHODS FOR FURTHER CUSTOMISATION???
        setMediaNotificationProvider(object : DefaultMediaNotificationProvider(this) {
            /* override fun createNotification(
                mediaSession: MediaSession,
                customLayout: ImmutableList<CommandButton>,
                actionFactory: MediaNotification.ActionFactory,
                onNotificationChangedCallback: MediaNotification.Provider.Callback
            ): MediaNotification {
                return updateNotification(mediaSession)
            }

            override fun handleCustomCommand(session: MediaSession, action: String, extras: Bundle): Boolean { return false }

             */
        })
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
            val albumId = it.mediaMetadata.extras?.getString(ALBUM_ID)
            val metadata = if (albumId != null) {
                val imageFile = File(artworkDirectory, "$albumId.jpg")
                val artworkUri = Uri.fromFile(imageFile)
                it.mediaMetadata.buildUpon().setArtworkUri(artworkUri).build()
            } else null

            val uri = ContentUris.withAppendedId(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, it.mediaId.toLong())

            val builder = it.buildUpon().setUri(uri)
            if (metadata != null) builder.setMediaMetadata(metadata)
            builder.build()
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

    private fun updateNotification(session: MediaSession): MediaNotification {
        val isPlaying = session.player.isPlaying
        val smallIcon = if (isPlaying) R.drawable.play
        else R.drawable.pause

        val playOrPause = if (isPlaying) R.drawable.ic_pause
        else R.drawable.ic_play
        val playPauseIntent = if (isPlaying) {
            Intent(applicationContext, MediaPlaybackService::class.java).setAction("ACTION_PAUSE")
        } else Intent(applicationContext, MediaPlaybackService::class.java).setAction("ACTION_PLAY")
        val nextIntent = Intent(applicationContext, MediaPlaybackService::class.java).setAction("ACTION_NEXT")
        val prevIntent = Intent(applicationContext, MediaPlaybackService::class.java).setAction("ACTION_PREVIOUS")

        val intent = packageManager
            .getLaunchIntentForPackage(packageName)
            ?.setPackage(null)
            ?.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        val activityIntent = PendingIntent.getActivity(applicationContext, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val notificationCompat = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(smallIcon)
            .addAction(
                NotificationCompat.Action(R.drawable.ic_next, getString(R.string.play_next),
                    PendingIntent.getService(applicationContext, 0, nextIntent, PendingIntent.FLAG_IMMUTABLE)
                )
            )
            .addAction(
                NotificationCompat.Action(R.drawable.ic_back, getString(R.string.play_prev),
                    PendingIntent.getService(applicationContext, 0, prevIntent, PendingIntent.FLAG_IMMUTABLE)
                )
            )
            .addAction(
                NotificationCompat.Action(playOrPause, getString(R.string.play_pause),
                    PendingIntent.getService(applicationContext, 0, playPauseIntent, PendingIntent.FLAG_IMMUTABLE)
                )
            )
            .addAction(
                NotificationCompat.Action(R.drawable.ic_next, getString(R.string.play_next),
                    PendingIntent.getService(applicationContext, 0, nextIntent, PendingIntent.FLAG_IMMUTABLE)
                )
            )
            .setContentIntent(activityIntent)
            .setStyle(MediaStyleNotificationHelper.MediaStyle(session).setShowActionsInCompactView(0, 1, 2))

        // Add the metadata for the currently playing track
        /* setContentTitle(mediaMetadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE))
        setContentText(mediaMetadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST))
        setLargeIcon(mediaMetadata.getBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART)) */

        // Make the transport controls visible on the lockscreen
        /* setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        priority = NotificationCompat.PRIORITY_DEFAULT */
            .build()
        return MediaNotification(1, notificationCompat)
    }
}
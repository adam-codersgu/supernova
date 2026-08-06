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
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.codersguidebook.supernova.params.MediaServiceConstants.Companion.ALBUM_ID
import com.codersguidebook.supernova.params.MediaServiceConstants.Companion.SKIP_TO_NEXT
import com.codersguidebook.supernova.params.MediaServiceConstants.Companion.SKIP_TO_PREV
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

            override fun seekToNext() {
                sendBroadcastIntent(SKIP_TO_NEXT)
            }

            override fun seekToPrevious() {
                sendBroadcastIntent(SKIP_TO_PREV)
            }
        }

        val intent = packageManager
            .getLaunchIntentForPackage(packageName)
            ?.setPackage(null)
            ?.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        val activityIntent = PendingIntent.getActivity(applicationContext, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        mediaSession = MediaSession.Builder(this, player).setCallback(this).setSessionActivity(activityIntent).build()
    }

    private fun sendBroadcastIntent(intentKey: String) {
        val intent = Intent(intentKey).apply {
            setPackage(packageName)
        }
        sendBroadcast(intent)
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
}
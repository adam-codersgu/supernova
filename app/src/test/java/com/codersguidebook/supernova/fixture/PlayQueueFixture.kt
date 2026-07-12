package com.codersguidebook.supernova.fixture

import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.codersguidebook.supernova.params.MediaServiceConstants.Companion.ALBUM_ID
import com.codersguidebook.supernova.params.MediaServiceConstants.Companion.ORDER_ID
import com.codersguidebook.supernova.params.MediaServiceConstants.Companion.REMEMBER_PROGRESS

object PlayQueueFixture {

    fun getPlayQueue(length: Int = 1): List<MediaItem> {
        val playQueue = mutableListOf<MediaItem>()
        for (i in 1..length) {
            playQueue.add(getMediaItem(i.toString()))
        }
        return playQueue
    }

    private fun getMetadata(orderId: Int? = null): MediaMetadata {
        val extras = Bundle().apply {
            putString(ALBUM_ID, "4343")
            putBoolean(REMEMBER_PROGRESS, false)
            if (orderId != null) {
                putInt(ORDER_ID, orderId)
            }
        }
        return MediaMetadata.Builder()
            .setAlbumTitle("Album name")
            .setArtist("Artist name")
            .setExtras(extras)
            .setTitle("Song title")
            .build()
    }

    private fun getMediaItem(mediaId: String, orderId: Int? = null): MediaItem {
        return MediaItem.Builder()
            .setMediaId(mediaId)
            .setMediaMetadata(getMetadata(orderId))
            .build()
    }
}
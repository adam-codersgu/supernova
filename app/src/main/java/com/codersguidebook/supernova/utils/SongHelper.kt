package com.codersguidebook.supernova.utils

import androidx.media3.common.MediaItem
import com.codersguidebook.supernova.entities.Song
import com.codersguidebook.supernova.params.MediaServiceConstants.Companion.ALBUM_ID

object SongHelper {

    fun buildFromMediaItem(mediaItem: MediaItem): Song {
        val metadata = mediaItem.mediaMetadata
        val extras = metadata.extras
            ?: throw RuntimeException("Extras null for ${mediaItem.mediaMetadata.title}")
        return Song(mediaItem.mediaId.toLong(), 0, metadata.title.toString(),
            metadata.artist.toString(), metadata.albumTitle.toString(),
            extras.getString(ALBUM_ID, "-1"), "0")
    }
}
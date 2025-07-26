package com.codersguidebook.supernova.utils

import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore

object MediaItemHelper {

    private const val DELIMITER = "-"

    fun combineSongIdAndQueueId(songId: Long, queueId: Int): String {
        return songId.toString() + DELIMITER + queueId.toString()
    }

    fun extractQueueId(mediaId: String): Int {
        val components = mediaId.split(DELIMITER)
        return components[1].toInt()
    }

    private fun extractSongId(mediaId: String): Long {
        val components = mediaId.split(DELIMITER)
        return components[0].toLong()
    }

    fun getContentUri(mediaId: String): Uri {
        return ContentUris.withAppendedId(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            extractSongId(mediaId))
    }
}
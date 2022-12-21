package com.codersguidebook.supernova.utils

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken

/** Helper that assists with the handling of playlists. */
object PlaylistHelper {

    /**
     * Convert a JSON String representing a playlist's songs to a list of song IDs.
     *
     * @param json - The JSON String representation of a playlist's songs.
     * @return - A mutable list of song IDs, or an empty mutable list if no song IDs found.
     */
    fun extractSongIds(json: String?): MutableList<Long> {
        return if (json != null) {
            val listType = object : TypeToken<List<Long>>() {}.type
            Gson().fromJson(json, listType)
        } else mutableListOf()
    }

    /**
     * Convert a list of song IDs to a JSON String.
     *
     * @param songIds - A list of song IDs.
     * @return A serialised JSON String of the song IDs.
     */
    fun serialiseSongIds(songIds: List<Long>): String = GsonBuilder().create().toJson(songIds)
}
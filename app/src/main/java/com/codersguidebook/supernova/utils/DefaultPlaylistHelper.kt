package com.codersguidebook.supernova.utils

import android.content.Context
import com.codersguidebook.supernova.R

/** Helper for coordinating the default playlists held by the application. */
class DefaultPlaylistHelper(context: Context) {

    // Pair mapping is <ID, Playlist name>
    val favourites = Pair(1, context.getString(R.string.favourites))
    val recentlyPlayed = Pair(2, context.getString(R.string.recently_played))
    val songOfTheDay = Pair(3, context.getString(R.string.song_day))
    val mostPlayed = Pair(4, context.getString(R.string.most_played))

    val playlistPairs = listOf(favourites, recentlyPlayed, songOfTheDay, mostPlayed)

    /**
     * Retrieve a list containing the names of all default playlists.
     *
     * @return A list of Playlist names in String format.
     */
    fun getDefaultPlaylistNames(): List<String> = playlistPairs.map { it.second }
}
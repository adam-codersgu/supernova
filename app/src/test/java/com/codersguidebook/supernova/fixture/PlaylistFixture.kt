package com.codersguidebook.supernova.fixture

import com.codersguidebook.supernova.entities.Playlist
import com.codersguidebook.supernova.entities.Song
import com.codersguidebook.supernova.utils.PlaylistHelper

object PlaylistFixture {

    fun getMockPlaylist(): Playlist {
        val songIds = PlaylistHelper.serialiseSongIds(listOf(getMockSong().songId))
        return Playlist(1, "Playlist A", songIds, false)
    }

    // TODO: Delegate the below song data setup methods to another fixture class
    fun getMockSong(isFavourite: Boolean = false): Song {
        return getMockSong(1L, isFavourite)
    }

    fun getMockSong(songId: Long, isFavourite: Boolean = false): Song {
        return Song(songId, 1, "Title", "Artist", "Album",
            "1", "2024", isFavourite)
    }
}
package com.codersguidebook.supernova.fixture

import com.codersguidebook.supernova.entities.Playlist
import com.codersguidebook.supernova.entities.Song
import com.codersguidebook.supernova.utils.PlaylistHelper

object PlaylistFixture {

    fun getMockPlaylist(songs: List<Song> = listOf(getMockSong())): Playlist {
        val songIds = PlaylistHelper.serialiseSongIds(songs.map { it.songId })
        return Playlist(1, "Playlist A", songIds, false)
    }

    fun getMockFavouritesPlaylist(): Playlist {
        val songIds = PlaylistHelper.serialiseSongIds(listOf(getMockSong(true).songId))
        return Playlist(1, "Favourites", songIds, true)
    }

    fun getMockSongOfTheDayPlaylist(songQty: Int = 1): Playlist {
        val songIds = mutableListOf<Long>()
        for (i in 1..songQty) {
            songIds.add(i.toLong())
        }
        return Playlist(3, "Song of the day", PlaylistHelper.serialiseSongIds(songIds), true)
    }

    // TODO: Delegate the below song data setup methods to another fixture class
    fun getMockSong(isFavourite: Boolean = false): Song {
        return getMockSong(1L, isFavourite)
    }

    fun getMockSong(songId: Long, isFavourite: Boolean = false): Song {
        return Song(
            songId, 1, "Title", "Artist", "Album",
            "1", "2024", isFavourite
        )
    }
}
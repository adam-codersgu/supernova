package com.codersguidebook.supernova.fixture

import com.codersguidebook.supernova.entities.Playlist
import com.codersguidebook.supernova.entities.Song
import com.codersguidebook.supernova.utils.PlaylistHelper

object PlaylistFixture {

    fun getMockPlaylist(songs: List<Song> = listOf(getMockSong())): Playlist {
        return getMockPlaylist(1, songs)
    }

    fun getMockPlaylist(playlistId: Int, songs: List<Song> = listOf(getMockSong())): Playlist {
        val songIds = PlaylistHelper.serialiseSongIds(songs.map { it.songId })
        return Playlist(playlistId, "Playlist A", songIds, false)
    }

    fun getMockFavouritesPlaylist(): Playlist {
        val songIds = PlaylistHelper.serialiseSongIds(listOf(getMockSong(true).songId))
        return Playlist(1, "Favourites", songIds, true)
    }

    fun getMockRecentlyPlayedPlaylist(songQty: Int = 1): Playlist {
        val songIds = getListOfIds(songQty)
        return Playlist(2, "Recently played", PlaylistHelper.serialiseSongIds(songIds), true)
    }

    fun getMockSongOfTheDayPlaylist(songQty: Int = 1): Playlist {
        val songIds = getListOfIds(songQty)
        return Playlist(3, "Song of the day", PlaylistHelper.serialiseSongIds(songIds), true)
    }

    fun getMockMostPlayedPlaylist(songQty: Int = 1): Playlist {
        val songIds = getListOfIds(songQty)
        return Playlist(4, "Most played", PlaylistHelper.serialiseSongIds(songIds), true)
    }

    private fun getListOfIds(length: Int): MutableList<Long> {
        val songIds = mutableListOf<Long>()
        for (i in 1..length) {
            songIds.add(i.toLong())
        }
        return songIds
    }

    // TODO: Delegate the below song data setup methods to another fixture class
    fun getMockSongs(length: Int): MutableList<Song> {
        val songs = mutableListOf<Song>()
        for (i in 1..length) {
            songs.add(getMockSong(i.toLong()))
        }
        return songs
    }

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
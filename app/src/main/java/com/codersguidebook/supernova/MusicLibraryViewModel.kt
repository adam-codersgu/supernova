package com.codersguidebook.supernova

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.codersguidebook.supernova.entities.Artist
import com.codersguidebook.supernova.entities.Playlist
import com.codersguidebook.supernova.entities.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// we use AndroidViewModel so we can request application context
class MusicLibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MusicRepository
    val allSongs: LiveData<List<Song>>
    val mostPlayedSongsById: LiveData<List<Long>>
    val allArtists: LiveData<List<Artist>>
    val allPlaylists: LiveData<List<Playlist>>

    init {
        val musicDao = MusicDatabase.getDatabase(application, viewModelScope).musicDao()
        val playlistDao = MusicDatabase.getDatabase(application, viewModelScope).playlistDao()
        repository = MusicRepository(musicDao, playlistDao)
        allSongs = repository.allSongs
        mostPlayedSongsById = repository.mostPlayedSongsById
        allArtists = repository.allArtists
        allPlaylists = repository.allPlaylists
    }

    fun deleteSong(song: Song) = viewModelScope.launch(Dispatchers.IO) {
        repository.deleteSong(song)
    }

    fun deletePlaylist(playlist: Playlist) = viewModelScope.launch(Dispatchers.IO) {
        repository.deletePlaylist(playlist)
    }

    fun insertSongs(songs: List<Song>) = viewModelScope.launch(Dispatchers.IO) {
        repository.insertSongs(songs)
    }

    fun insertPlaylist(playlist: Playlist) = viewModelScope.launch(Dispatchers.IO) {
        repository.insertPlaylist(playlist)
    }

    fun updateMusicInfo(songs: List<Song>) = viewModelScope.launch(Dispatchers.IO) {
        repository.updateMusicInfo(songs)
    }

    fun updatePlaylists(playlists: List<Playlist>) = viewModelScope.launch(Dispatchers.IO) {
        repository.updatePlaylist(playlists)
    }

    fun increaseSongPlaysBySongId(songId: Long) {
        repository.increaseSongPlaysBySongId(songId)
    }
}
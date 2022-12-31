package com.codersguidebook.supernova

import androidx.lifecycle.LiveData
import com.codersguidebook.supernova.entities.Artist
import com.codersguidebook.supernova.entities.Playlist
import com.codersguidebook.supernova.entities.Song

class MusicRepository(private val musicDao: MusicDao, private val playlistDao: PlaylistDao) {

    val allSongs: LiveData<List<Song>> = musicDao.getAlphabetizedSongs()
    val allArtists: LiveData<List<Artist>> = musicDao.getAlphabetizedArtists()
    val mostPlayedSongsById: LiveData<List<Long>> = musicDao.findMostPlayedSongsById()
    val allPlaylists: LiveData<List<Playlist>> = playlistDao.getAllPlaylistsByName()

    suspend fun getAllSongs(): List<Song> = musicDao.getAllSongs()

    suspend fun saveSongs(songs: List<Song>) {
        for (song in songs) musicDao.insert(song)
    }

    suspend fun savePlaylist(playlist: Playlist) {
        playlistDao.insert(playlist)
    }

    suspend fun deleteSong(song: Song) {
        musicDao.delete(song)
    }

    suspend fun deletePlaylist(playlist: Playlist) {
        playlistDao.delete(playlist)
    }

    suspend fun updateSongs(songs: List<Song>) {
        for (song in songs) musicDao.updateSong(song)
    }

    suspend fun getSongsByAlbumIdOrderByTrack(albumId: String): List<Song> {
        return musicDao.getSongsByAlbumIdOrderByTrack(albumId)
    }

    fun getSongsByAlbumId(albumId: String): LiveData<List<Song>> = musicDao.getSongsByAlbumId(albumId)

    fun getSongsByArtist(artist: String): LiveData<List<Song>> = musicDao.getSongsByArtist(artist)

    suspend fun getAllPlaylists(): List<Playlist> = playlistDao.getAllPlaylists()

    fun updatePlaylist(playlists: List<Playlist>){
        for (playlist in playlists) playlistDao.updatePlaylist(playlist)
    }

    fun increaseSongPlaysBySongId(songId: Long) {
        musicDao.increaseSongPlaysBySongId(songId)
    }

    suspend fun findSongById(songId: Long): Song? = musicDao.findSongById(songId)

    suspend fun findRandomSong(): Song? = musicDao.findRandomSong()

    fun findPlaylistByNameLiveData(name: String): LiveData<Playlist?> = playlistDao.findPlaylistByNameLiveData(name)

    suspend fun findPlaylistByName(name: String): Playlist? = playlistDao.findPlaylistByName(name)
}
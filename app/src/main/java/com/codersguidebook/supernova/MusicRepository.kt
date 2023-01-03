package com.codersguidebook.supernova

import androidx.lifecycle.LiveData
import com.codersguidebook.supernova.entities.Artist
import com.codersguidebook.supernova.entities.Playlist
import com.codersguidebook.supernova.entities.Song

class MusicRepository(private val musicDao: MusicDao, private val playlistDao: PlaylistDao) {

    val allSongs: LiveData<List<Song>> = musicDao.getAllSongsOrderByTitle()
    val allArtists: LiveData<List<Artist>> = musicDao.getAllArtists()
    val mostPlayedSongsById: LiveData<List<Long>> = musicDao.getMostPlayedSongsById()
    val allPlaylists: LiveData<List<Playlist>> = playlistDao.getAllPlaylistsOrderByName()

    suspend fun getAllSongs(): List<Song> = musicDao.getAllSongs()

    suspend fun saveSongs(songs: List<Song>) {
        for (song in songs) musicDao.insert(song)
    }

    suspend fun savePlaylist(playlist: Playlist) = playlistDao.insert(playlist)

    suspend fun deleteSong(song: Song) = musicDao.delete(song)

    suspend fun deletePlaylist(playlist: Playlist) = playlistDao.delete(playlist)

    suspend fun updateSongs(songs: List<Song>) {
        for (song in songs) musicDao.update(song)
    }

    suspend fun getSongsByAlbumIdOrderByTrack(albumId: String): List<Song> {
        return musicDao.getSongsByAlbumIdOrderByTrack(albumId)
    }

    fun getSongsByAlbumId(albumId: String): LiveData<List<Song>> = musicDao.getSongsByAlbumIdOrderByTrackLiveData(albumId)

    fun getSongsByArtist(artist: String): LiveData<List<Song>> = musicDao.getSongsByArtist(artist)

    suspend fun getSongPlaysByArtist(artistName: String): Int = musicDao.getSongPlaysByArtist(artistName)

    suspend fun getAllPlaylists(): List<Playlist> = playlistDao.getAllPlaylists()

    fun updatePlaylist(playlists: List<Playlist>){
        for (playlist in playlists) playlistDao.update(playlist)
    }

    fun increaseSongPlaysBySongId(songId: Long) = musicDao.increaseSongPlaysBySongId(songId)

    suspend fun getSongById(songId: Long): Song? = musicDao.getSongById(songId)

    suspend fun getRandomSong(): Song? = musicDao.getRandomSong()

    fun getPlaylistByNameLiveData(name: String): LiveData<Playlist?> = playlistDao.getPlaylistByNameLiveData(name)

    suspend fun getPlaylistByName(name: String): Playlist? = playlistDao.getPlaylistByName(name)
}
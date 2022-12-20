package com.codersguidebook.supernova

import androidx.lifecycle.LiveData
import com.codersguidebook.supernova.entities.Artist
import com.codersguidebook.supernova.entities.Playlist
import com.codersguidebook.supernova.entities.Song

class MusicRepository(private val musicDao: MusicDao, private val playlistDao: PlaylistDao) {

    val allSongs: LiveData<List<Song>> = musicDao.getAlphabetizedSongs()
    val allArtists: LiveData<List<Artist>> = musicDao.getAlphabetizedArtists()
    val mostPlayedSongsById: LiveData<List<Long>> = musicDao.findMostPlayedSongsById()
    val allPlaylists: LiveData<List<Playlist>> = playlistDao.getAllPlaylists()

    suspend fun insertSongs(songs: List<Song>) {
        for (s in songs) musicDao.insert(s)
    }

    suspend fun insertPlaylist(playlist: Playlist) {
        playlistDao.insert(playlist)
    }

    suspend fun deleteSong(song: Song) {
        musicDao.delete(song)
    }

    suspend fun deletePlaylist(playlist: Playlist) {
        playlistDao.delete(playlist)
    }

    suspend fun updateMusicInfo(songs: List<Song>) {
        for (s in songs) {
            musicDao.updateSongInfo(s)
        }
    }

    suspend fun getSongByAlbumId(albumID: String): List<Song> {
        return musicDao.getSongByAlbumId(albumID)
    }

    fun updatePlaylist(playlists: List<Playlist>){
        for (p in playlists) {
            playlistDao.updatePlaylist(p)
        }
    }

    fun increaseSongPlaysBySongId(songId: Long) {
        musicDao.increaseSongPlaysBySongId(songId)
    }
}
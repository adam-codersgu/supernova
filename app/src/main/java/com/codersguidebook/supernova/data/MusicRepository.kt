package com.codersguidebook.supernova.data

import androidx.lifecycle.LiveData
import com.codersguidebook.supernova.entities.Artist
import com.codersguidebook.supernova.entities.Playlist
import com.codersguidebook.supernova.entities.Song
import com.codersguidebook.supernova.entities.SongPlays

class MusicRepository(private val musicDao: MusicDao, private val playlistDao: PlaylistDao,
    private val songPlaysDao: SongPlaysDao) {

    val allSongs: LiveData<List<Song>> = musicDao.getSongsOrderByTitle()
    val allArtists: LiveData<List<Artist>> = musicDao.getArtists()
    val mostPlayedSongsById: LiveData<List<Long>> = musicDao.getSongIdsOrderBySongPlays()
    val allPlaylists: LiveData<List<Playlist>> = playlistDao.getAllPlaylistsOrderByName()

    suspend fun getAllSongs(): List<Song> = musicDao.getSongs()

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

    suspend fun getAllUserPlaylists(): List<Playlist> = playlistDao.getAllUserPlaylists()

    fun updatePlaylists(playlists: List<Playlist>){
        for (playlist in playlists) playlistDao.update(playlist)
    }

    suspend fun increaseSongPlaysBySongId(songId: Long) {
        val songPlaysEntry = songPlaysDao.getPlaysBySongIdAndDate(songId)
        if (songPlaysEntry == null) {
            songPlaysDao.insert(SongPlays(0, songId, qtyOfPlays = 1))
        } else {
            songPlaysDao.update(songPlaysEntry.copy(qtyOfPlays = songPlaysEntry.qtyOfPlays + 1))
        }
    }

    suspend fun getSongById(songId: Long): Song? = musicDao.getSongById(songId)

    suspend fun getRandomSong(): Song? = musicDao.getRandomSong()

    fun getPlaylistByNameLiveData(name: String): LiveData<Playlist?> = playlistDao.getPlaylistByNameLiveData(name)

    suspend fun getPlaylistById(id: Int): Playlist? = playlistDao.getPlaylistById(id)

    suspend fun getPlaylistByName(name: String): Playlist? = playlistDao.getPlaylistByName(name)
}
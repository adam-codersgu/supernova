package com.codersguidebook.supernova.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import com.codersguidebook.supernova.entities.Artist
import com.codersguidebook.supernova.entities.Playlist
import com.codersguidebook.supernova.entities.Song
import com.codersguidebook.supernova.entities.SongPlays
import java.time.LocalDate

class MusicRepository(private val musicDao: MusicDao, private val playlistDao: PlaylistDao,
    private val songPlaysDao: SongPlaysDao) {

    val allSongs: LiveData<List<Song>> = musicDao.getSongsOrderByTitle()
    val allArtists: LiveData<List<Artist>> = musicDao.getArtists()
    val allPlaylists: LiveData<List<Playlist>> = playlistDao.getAllPlaylistsOrderByName()

    private val mostPlayedPlaylistStartDate = MutableLiveData<Long>()
    val mostPlayedSongsById: LiveData<List<Long>> = mostPlayedPlaylistStartDate.switchMap {
            day -> songPlaysDao.getMostPlayedSongsSinceDay(day = day)
    }

    fun setMostPlayedPlaylistStartDate(timeframe: String) {
        val newDate = getEpochDayByTimeframe(timeframe)
        if (newDate != mostPlayedPlaylistStartDate.value) {
            mostPlayedPlaylistStartDate.postValue(newDate)
        }
    }

    private fun getEpochDayByTimeframe(timeframe: String): Long {
        return when (timeframe) {
            "Last Week" -> LocalDate.now().minusWeeks(1)
            "Last Month" -> LocalDate.now().minusMonths(1)
            "Last Year" -> LocalDate.now().minusYears(1)
            else -> LocalDate.ofEpochDay(0L)
        }.toEpochDay()
    }

    suspend fun getAllSongs(): List<Song> = musicDao.getSongs()

    suspend fun saveSongs(songs: List<Song>) {
        for (song in songs) musicDao.insert(song)
    }

    suspend fun savePlaylist(playlist: Playlist) = playlistDao.insert(playlist)

    suspend fun deleteSong(song: Song) {
        musicDao.delete(song)
        songPlaysDao.deleteBySongId(song.songId)
    }

    suspend fun deletePlaylist(playlist: Playlist) = playlistDao.delete(playlist)

    suspend fun updateSongs(songs: List<Song>) {
        for (song in songs) musicDao.update(song)
    }

    suspend fun getSongsByAlbumIdOrderByTrack(albumId: String): List<Song> {
        return musicDao.getSongsByAlbumIdOrderByTrack(albumId)
    }

    fun getSongsByAlbumId(albumId: String): LiveData<List<Song>> = musicDao.getSongsByAlbumIdOrderByTrackLiveData(albumId)

    fun getSongsByArtist(artist: String): LiveData<List<Song>> = musicDao.getSongsByArtist(artist)

    suspend fun getSongPlaysByArtist(artistName: String): Int {
        val songIds = musicDao.getSongIdsByArtist(artistName)
        return songPlaysDao.getSongPlaysWhereSongIdIn(songIds)
    }

    suspend fun getSongPlaysBySongIdsAndTimeframe(songIds: List<Long>, timeframe: String) =
        songPlaysDao.getSongPlaysBySongIdsAndDay(songIds, getEpochDayByTimeframe(timeframe))

    suspend fun getAllPlaylists(): List<Playlist> = playlistDao.getAllPlaylists()

    suspend fun getAllUserPlaylists(): List<Playlist> = playlistDao.getAllUserPlaylists()

    fun updatePlaylists(playlists: List<Playlist>){
        for (playlist in playlists) playlistDao.update(playlist)
    }

    suspend fun increaseSongPlaysBySongId(songId: Long) {
        val songPlaysEntry = songPlaysDao.getPlaysBySongIdAndEpochDays(songId)
        if (songPlaysEntry == null) {
            songPlaysDao.insert(SongPlays(0, songId, qtyOfPlays = 1))
        } else {
            songPlaysDao.update(songPlaysEntry.copy(qtyOfPlays = songPlaysEntry.qtyOfPlays + 1))
        }
    }

    suspend fun getSongById(songId: Long): Song? = musicDao.getSongById(songId)

    suspend fun savePlaybackProgress(mediaId: Long, playbackPosition: Int) {
        musicDao.savePlaybackProgress(mediaId, playbackPosition)
    }

    suspend fun getRandomSong(): Song? = musicDao.getRandomSong()

    fun getPlaylistByNameLiveData(name: String): LiveData<Playlist?> = playlistDao.getPlaylistByNameLiveData(name)

    suspend fun getPlaylistById(id: Int): Playlist? = playlistDao.getPlaylistById(id)

    suspend fun getPlaylistByName(name: String): Playlist? = playlistDao.getPlaylistByName(name)
}
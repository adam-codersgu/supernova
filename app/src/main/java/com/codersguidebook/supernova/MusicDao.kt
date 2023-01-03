package com.codersguidebook.supernova

import androidx.lifecycle.LiveData
import androidx.room.*
import com.codersguidebook.supernova.entities.Artist
import com.codersguidebook.supernova.entities.Song

@Dao
interface MusicDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(song: Song)

    @Delete
    suspend fun delete(song: Song)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(song: Song)

    @Query("SELECT * from music_library")
    suspend fun getAllSongs(): List<Song>

    @Query("SELECT song_artist, count(*) FROM music_library GROUP BY song_artist")
    fun getAllArtists(): LiveData<List<Artist>>

    @Query("SELECT * from music_library ORDER BY song_title")
    fun getAllSongsOrderByTitle(): LiveData<List<Song>>

    @Query("SELECT * from music_library WHERE song_album_id = :albumId ORDER BY song_track")
    suspend fun getSongsByAlbumIdOrderByTrack(albumId: String): List<Song>

    @Query("SELECT * FROM music_library WHERE song_album_id = :albumId ORDER BY song_track")
    fun getSongsByAlbumIdOrderByTrackLiveData(albumId: String): LiveData<List<Song>>

    @Query("SELECT SUM(song_plays) FROM music_library WHERE song_artist = :artistName")
    suspend fun getSongPlaysByArtist(artistName: String): Int

    @Query("SELECT * FROM music_library WHERE song_title LIKE :search OR song_artist LIKE :search OR song_album_name LIKE :search LIMIT :limit")
    suspend fun getSongsLikeSearch(search: String, limit: Int = 100): List<Song>

    @Query("SELECT song_artist, count(*) FROM music_library WHERE song_artist LIKE :search GROUP BY song_artist LIMIT :limit")
    suspend fun getArtistsLikeSearch(search: String, limit: Int = 10): List<Artist>

    @Query("SELECT * FROM music_library WHERE song_artist = :artist ORDER BY song_title")
    fun getSongsByArtist(artist: String): LiveData<List<Song>>

    @Query("SELECT songId FROM music_library WHERE song_plays > 0 ORDER BY song_plays DESC LIMIT :limit")
    fun getMostPlayedSongsById(limit: Int = 30): LiveData<List<Long>>

    @Query("UPDATE music_library SET song_plays = song_plays + 1 WHERE songId = :songId")
    fun increaseSongPlaysBySongId(songId: Long)

    @Query("SELECT * FROM music_library WHERE songId = :songId")
    suspend fun getSongById(songId: Long): Song?

    @Query("SELECT * FROM music_library ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomSong(): Song?
}
package com.codersguidebook.supernova

import androidx.lifecycle.LiveData
import androidx.room.*
import com.codersguidebook.supernova.entities.Artist
import com.codersguidebook.supernova.entities.Song

@Dao
interface MusicDao {

    @Query("SELECT song_artist, count(*) FROM music_library GROUP BY song_artist")
    fun getAlphabetizedArtists(): LiveData<List<Artist>>

    @Query("SELECT * from music_library ORDER BY song_title ASC")
    fun getAlphabetizedSongs(): LiveData<List<Song>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(song: Song)

    @Query("SELECT * from music_library WHERE song_album_id LIKE :albumID LIMIT 1")
    suspend fun doesAlbumIDExist(albumID: String): List<Song>

    @Query("SELECT * FROM music_library WHERE song_title LIKE :search OR song_artist LIKE :search OR song_album_name LIKE :search LIMIT 100")
    suspend fun findBySearchSongs(search: String): List<Song>

    @Query("SELECT song_artist, count(*) FROM music_library WHERE song_artist LIKE :search GROUP BY song_artist LIMIT 10")
    suspend fun findBySearchArtists(search: String): List<Artist>

    @Query("SELECT * FROM music_library WHERE song_album_id LIKE :albumID ORDER BY song_track ASC")
    fun findAlbumSongs(albumID: String): LiveData<List<Song>>

    @Query("SELECT * FROM music_library WHERE song_artist LIKE :artistName ORDER BY song_title ASC")
    fun findArtistsSongs(artistName: String): LiveData<List<Song>>

    @Query("SELECT * FROM music_library WHERE song_plays > 0 ORDER BY song_plays DESC LIMIT 30")
    fun findMostPlayed(): LiveData<List<Song>>

    @Query("UPDATE music_library SET song_plays = song_plays + 1 WHERE songId = :songId")
    fun increaseSongPlaysBySongId(songId: Long)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateSongInfo(song: Song)

    @Delete
    suspend fun delete(song: Song)
}
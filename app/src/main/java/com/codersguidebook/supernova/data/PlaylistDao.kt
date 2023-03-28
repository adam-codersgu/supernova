package com.codersguidebook.supernova.data

import androidx.lifecycle.LiveData
import androidx.room.*
import com.codersguidebook.supernova.entities.Playlist

@Dao
interface PlaylistDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(playlist: Playlist)

    @Delete
    suspend fun delete(playlist: Playlist)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(playlist: Playlist)

    @Query("SELECT * from playlists")
    suspend fun getAllPlaylists(): List<Playlist>

    @Query("SELECT * from playlists WHERE is_default_playlist = false")
    suspend fun getAllUserPlaylists(): List<Playlist>

    @Query("SELECT * from playlists ORDER BY playlist_name")
    fun getAllPlaylistsOrderByName(): LiveData<List<Playlist>>

    @Query("SELECT * FROM playlists WHERE playlistId = :id")
    suspend fun getPlaylistById(id: Int): Playlist?

    @Query("SELECT * FROM playlists WHERE playlist_name = :name")
    suspend fun getPlaylistByName(name: String): Playlist?

    @Query("SELECT * FROM playlists WHERE playlist_name = :name")
    fun getPlaylistByNameLiveData(name: String): LiveData<Playlist?>

    @Query("SELECT * FROM playlists WHERE playlist_name LIKE :search LIMIT :limit")
    suspend fun getPlaylistsLikeSearch(search: String, limit: Int = 10): List<Playlist>
}
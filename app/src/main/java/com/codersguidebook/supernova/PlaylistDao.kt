package com.codersguidebook.supernova

import androidx.lifecycle.LiveData
import androidx.room.*
import com.codersguidebook.supernova.entities.Playlist

@Dao
interface PlaylistDao {
    @Delete
    suspend fun delete(playlist: Playlist)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(playlist: Playlist)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updatePlaylist(playlist: Playlist)

    @Transaction
    @Query("SELECT * from playlist_table ORDER BY playlist_name ASC")
    fun getAllPlaylists(): LiveData<List<Playlist>>

    @Query("SELECT * FROM playlist_table WHERE playlist_name = :playlistName")
    fun findPlaylist(playlistName: String): LiveData<Playlist?>

    @Query("SELECT * FROM playlist_table WHERE playlist_name LIKE :search LIMIT 10")
    suspend fun findBySearchPlaylists(search: String): List<Playlist>
}
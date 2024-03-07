package com.codersguidebook.supernova.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.codersguidebook.supernova.entities.SongPlays
import com.codersguidebook.supernova.entities.TotalPlaysForSong

@Dao
interface SongPlaysDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(songPlays: SongPlays)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(songPlays: SongPlays)

    @Query("DELETE FROM SongPlays WHERE song_id = :songId")
    suspend fun deleteBySongId(songId: Long)

    @Query("SELECT song_id, sum(qtyOfPlays) FROM SongPlays GROUP BY song_id " +
            "ORDER BY sum(qtyOfPlays) LIMIT :limit")
    suspend fun getMostPlayedSongs(limit: Int = 30): List<TotalPlaysForSong>
}
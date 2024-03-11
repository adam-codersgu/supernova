package com.codersguidebook.supernova.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.codersguidebook.supernova.entities.SongPlays
import java.time.LocalDate

@Dao
interface SongPlaysDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(songPlays: SongPlays)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(songPlays: SongPlays)

    @Query("SELECT * FROM SongPlays WHERE song_id = :songId AND epochDays = :day")
    suspend fun getPlaysBySongIdAndEpochDays(songId: Long,
                                             day: Long = LocalDate.now().toEpochDay()): SongPlays?

    @Query("DELETE FROM SongPlays WHERE song_id = :songId")
    suspend fun deleteBySongId(songId: Long)

    @Query("SELECT song_id FROM SongPlays WHERE epochDays >= :day " +
            "GROUP BY song_id " +
            "ORDER BY sum(qtyOfPlays) LIMIT :limit")
    fun getMostPlayedSongsSinceDay(limit: Int = 30, day: Long): LiveData<List<Long>>
}
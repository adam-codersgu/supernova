package com.codersguidebook.supernova.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.MapColumn
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

    @Query("SELECT * FROM SongPlays WHERE songId = :songId AND epochDays = :day")
    suspend fun getPlaysBySongIdAndEpochDays(songId: Long,
                                             day: Long = LocalDate.now().toEpochDay()): SongPlays?

    @Query("SELECT songId, qtyOfPlays FROM SongPlays WHERE songId IN (:songIds)")
    suspend fun getSongPlaysBySongIds(songIds: List<Long>): Map<@MapColumn(columnName = "songId") String,
            @MapColumn(columnName = "qtyOfPlays") Int>

    @Query("SELECT SUM(qtyOfPlays) FROM SongPlays WHERE songId IN (:songIds)")
    suspend fun getSongPlaysWhereSongIdIn(songIds: List<Long>): Int

    @Query("DELETE FROM SongPlays WHERE songId = :songId")
    suspend fun deleteBySongId(songId: Long)

    @Query("SELECT songId FROM SongPlays WHERE epochDays >= :day " +
            "GROUP BY songId " +
            "ORDER BY sum(qtyOfPlays) LIMIT :limit")
    fun getMostPlayedSongsSinceDay(limit: Int = 30, day: Long): LiveData<List<Long>>
}
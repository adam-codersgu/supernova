package com.codersguidebook.supernova.entities

import androidx.room.ColumnInfo

/**
 * Data class used for mapping database queries that retrieve the total number of plays for a given song.
 */
data class TotalPlaysForSong(
    @ColumnInfo(name = "song_id") val songId: Long,
    @ColumnInfo(name = "sum(qtyOfPlays)") val qtyOfPlays: Int
)
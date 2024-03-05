package com.codersguidebook.supernova.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity
data class SongPlays(
    @PrimaryKey val songPlaysId: Long,
    val songId: Long,
    val date: LocalDate = LocalDate.now(),
    // qtyOfPlays refers to the number of plays on the given date
    var qtyOfPlays: Int = 0
)

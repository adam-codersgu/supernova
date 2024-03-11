package com.codersguidebook.supernova.entities

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import java.time.LocalDate

@Entity
@Parcelize
data class SongPlays(
    @PrimaryKey val songPlaysId: Long,
    @ColumnInfo(name = "song_id") val songId: Long,
    val epochDays: Long = LocalDate.now().toEpochDay(),
    // qtyOfPlays refers to the number of plays on the given date
    var qtyOfPlays: Int = 0
) : Parcelable

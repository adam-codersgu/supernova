package com.codersguidebook.supernova.entities

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Entity
@Parcelize
data class SongPlays(
    @PrimaryKey val songPlaysId: Long,
    @ColumnInfo(name = "song_id") val songId: Long,
    // TODO: May potentially need to use the date in Long or String format (e.g. unix date or date.toString())
    val date: LocalDate = LocalDate.now(),
    // qtyOfPlays refers to the number of plays on the given date
    var qtyOfPlays: Int = 0
) : Parcelable {

    val dateFormatted : String
        get() = date.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))

}

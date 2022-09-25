package com.codersguidebook.supernova.entities

import androidx.room.ColumnInfo

data class Artist(
    @ColumnInfo(name = "song_artist") val artistName: String?,
    @ColumnInfo(name = "count(*)") val songCount: Int?
)
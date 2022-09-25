package com.codersguidebook.supernova.entities

import androidx.room.ColumnInfo

/**
 * Data class used for mapping database queries that retrieve artist information.
 */
data class Artist(
    @ColumnInfo(name = "song_artist") val artistName: String?,
    @ColumnInfo(name = "count(*)") val songCount: Int?
)
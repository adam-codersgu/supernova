package com.codersguidebook.supernova.entities

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "playlist_table")
data class Playlist(
    @PrimaryKey(autoGenerate = true) val playlistID: Int,
    @ColumnInfo(name = "playlist_name") var name: String,
    @ColumnInfo(name = "playlist_songs") var songs: String?,
    @ColumnInfo(name = "default_playlist") var isDefault: Boolean
) : Parcelable
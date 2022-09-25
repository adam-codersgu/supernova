package com.codersguidebook.supernova.entities

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

/**
 * Data class used for mapping playlist entries in the database.
 */
@Parcelize
@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true) val playlistID: Int,
    @ColumnInfo(name = "playlist_name") var name: String,
    @ColumnInfo(name = "playlist_songs") var songs: String?,
    @ColumnInfo(name = "is_default_playlist") var isDefault: Boolean
) : Parcelable
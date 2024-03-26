package com.codersguidebook.supernova.entities

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

/** Data class for mapping a song's metadata to a database table called music_library. */
@Parcelize
@Entity(tableName = "music_library")
data class Song(
    @PrimaryKey val songId: Long,
    @ColumnInfo(name = "song_track") var track: Int,
    @ColumnInfo(name = "song_title") var title: String?,
    @ColumnInfo(name = "song_artist") var artist: String?,
    @ColumnInfo(name = "song_album_name") var albumName: String?,
    @ColumnInfo(name = "song_album_id") val albumId: String,
    @ColumnInfo(name = "song_year") var year: String,
    @ColumnInfo(name = "song_favourite") var isFavourite: Boolean = false,
    @ColumnInfo(name = "remember_progress") var rememberProgress: Boolean = false,
    @ColumnInfo(name = "playback_progress") var playbackProgress: Long = 0L
) : Parcelable {

    fun resetProgress() {
        this.playbackProgress = 0L
    }
}
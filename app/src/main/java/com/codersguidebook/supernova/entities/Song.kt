package com.codersguidebook.supernova.entities

import android.content.ContentUris
import android.os.Bundle
import android.os.Parcelable
import android.provider.MediaStore
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.codersguidebook.supernova.params.MediaServiceConstants.Companion.ALBUM_ID
import com.codersguidebook.supernova.params.MediaServiceConstants.Companion.MEDIA_ID
import com.codersguidebook.supernova.params.MediaServiceConstants.Companion.QUEUE_ID
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

    private fun getMetadata(queueId: Int): MediaMetadata {
        val extras = Bundle().apply {
            putString(ALBUM_ID, this@Song.albumId)
            putString(MEDIA_ID, this@Song.songId.toString())
            putInt(QUEUE_ID, queueId)
        }
        return MediaMetadata.Builder()
            .setAlbumTitle(this@Song.albumName)
            .setArtist(this@Song.artist)
            .setExtras(extras)
            .setTitle(this@Song.title)
            .build()
    }

    fun getMediaItem(queueId: Int): MediaItem {
        return MediaItem.Builder()
            .setMediaId(this@Song.songId.toString())
            .setMediaMetadata(getMetadata(queueId))
            .build()
    }
}
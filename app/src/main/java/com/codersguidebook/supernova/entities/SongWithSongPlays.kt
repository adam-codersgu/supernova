package com.codersguidebook.supernova.entities

import androidx.room.Embedded
import androidx.room.Relation

data class SongWithSongPlays(
    @Embedded
    val song: Song,
    @Relation(
        parentColumn = "songId",
        entityColumn = "song_id"
    )
    val songPlays: List<SongPlays>
)
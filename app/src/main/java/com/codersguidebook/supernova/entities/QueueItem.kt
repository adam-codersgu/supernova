package com.codersguidebook.supernova.entities

// FIXME
@Deprecated("Should deprecate this class and use MediaSession.QueueItem instead")
data class QueueItem(
    val queueID: Int,
    val song: Song
)
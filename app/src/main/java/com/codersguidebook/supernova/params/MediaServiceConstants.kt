package com.codersguidebook.supernova.params

/**
 * Class containing identifying values for custom commands, actions and messages
 * regarding the media browser service
 **/
class MediaServiceConstants {
    companion object {
        const val ACTION_PLAY = "play"
        const val ACTION_PAUSE = "pause"
        const val ACTION_NEXT = "next"
        const val ACTION_PREVIOUS = "previous"
        const val CHUNK_SIZE = 25
        const val LOAD_PLAY_QUEUE_CHUNK = "loadPlayQueueChunk"
        @Deprecated("Refactor LOAD_SONGS to ADD_SONGS_TO_PLAY_QUEUE")
        const val LOAD_SONGS = "loadSongs"
        const val MOVE_QUEUE_ITEM = "moveQueueItem"
        const val REMOVE_QUEUE_ITEM_BY_ID = "removeQueueItemById"
        const val RESTORE_PLAY_QUEUE = "restorePlayQueue"
        const val SET_REPEAT_MODE = "setRepeatMode"
        const val SET_SHUFFLE_MODE = "setShuffleMode"
        const val UPDATE_QUEUE_ITEM = "updateQueueItem"
    }
}
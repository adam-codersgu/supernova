package com.codersguidebook.supernova.params

/** Class containing identifying values for custom commands sent to the media browser service **/
class MediaServiceConstants {
    companion object {
        const val LOAD_SONGS = "loadSongs"
        const val REMOVE_QUEUE_ITEM_BY_ID = "removeQueueItemById"
        const val RESTORE_PLAY_QUEUE = "restorePlayQueue"
        const val SET_REPEAT_MODE = "setRepeatMode"
        const val SET_SHUFFLE_MODE = "setShuffleMode"
        const val UPDATE_QUEUE_ITEM = "updateQueueItem"
    }
}
package com.codersguidebook.supernova.params

/**
 * Class containing identifying values for custom commands, actions and messages
 * regarding the media browser service.
 **/
class MediaServiceConstants {
    companion object {
        const val ACTION_PLAY = "play"
        const val ACTION_PAUSE = "pause"
        const val ACTION_NEXT = "next"
        const val ACTION_PREVIOUS = "previous"
        const val MOVE_QUEUE_ITEM = "moveQueueItem"
        const val NO_ACTION = -1
        const val NOTIFICATION_CHANNEL_ID = "supernova"
        const val REMOVE_QUEUE_ITEM_BY_ID = "removeQueueItemById"
        const val SET_REPEAT_MODE = "setRepeatMode"
        const val SET_SHUFFLE_MODE = "setShuffleMode"
        const val SONG_DELETED = 0
        const val SONG_UPDATED = 1
        const val UPDATE_QUEUE_ITEM = "updateQueueItem"
    }
}
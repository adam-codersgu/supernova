package com.codersguidebook.supernova.params

/**
 * Class containing identifying values for custom commands, actions and messages
 * regarding the media browser service.
 **/
// TODO - MIGRATE THIS TO AN OBJECT AND USE PUBLIC CONST VALUES
class MediaServiceConstants {
    companion object {
        const val ALBUM_ID = "albumId"
        const val ORDER_ID = "orderId"
        const val REMEMBER_PROGRESS = "rememberProgress"
        const val NO_ACTION = -1
        const val NOTIFICATION_CHANNEL_ID = "supernova"
        const val SKIP_TO_NEXT = "skipToNext"
        const val SKIP_TO_PREV = "skipToPrev"
        const val SONG_DELETED = 0
        const val SONG_SAVED = 1
    }
}
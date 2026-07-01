package com.codersguidebook.supernova.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.codersguidebook.supernova.params.MediaServiceConstants.Companion.NOTIFICATION_CHANNEL_ID

object NotificationHelper {

    private const val NOTIFICATION_CHANNEL_DESCRIPTION = "All app notifications"
    private const val NOTIFICATION_CHANNEL_NAME = "Notifications"

    /** Create a channel for displaying application notifications */
    fun createChannelForMediaPlayerNotification(context: Context) {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = NOTIFICATION_CHANNEL_DESCRIPTION
            setSound(null, null)
            setShowBadge(false)
        }
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}
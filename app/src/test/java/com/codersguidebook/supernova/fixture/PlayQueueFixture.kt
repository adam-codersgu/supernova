package com.codersguidebook.supernova.fixture

import androidx.media3.common.MediaItem
import io.mockk.mockk

object PlayQueueFixture {

    fun getPlayQueue(length: Int = 1): List<MediaItem> {
        val playQueue = mutableListOf<MediaItem>()
        for (i in 1..length) {
            playQueue.add(mockk<MediaItem>(relaxed = true))
        }
        return playQueue
    }
}
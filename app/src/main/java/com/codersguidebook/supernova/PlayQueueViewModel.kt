package com.codersguidebook.supernova

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import kotlin.math.max

class PlayQueueViewModel : ViewModel() {
    var playQueue = MutableLiveData<MutableList<MediaItem>>()
    var currentQueueItemIndex = MutableLiveData<Int>()
    var currentlyPlayingSongMetadata = MutableLiveData<MediaMetadata?>()
    var isPlaying = MutableLiveData(false)
    var pendingSeekToInstruction = MutableLiveData<Long?>()
    var playbackDuration = MutableLiveData<Int>()
    var playbackPosition = MutableLiveData<Int>()

    fun getCurrentSongMediaId(): Long? {
        return if (playQueue.value!!.isEmpty()) {
            null
        } else {
            playQueue.value!![currentQueueItemIndex.value!!].mediaId.toLong()
        }
    }

    fun playQueueContainsMoreThanOneSong(): Boolean {
        return (playQueue.value?.size ?: return false) > 1
    }

    fun isUpcomingSongsInThePlayQueue(): Boolean {
        val currentIndex = currentQueueItemIndex.value
        val currentSize = playQueue.value?.size

        return if (currentIndex == null || currentSize == null) {
            false
        } else {
            currentIndex < (currentSize - 1)
        }
    }

    fun removeAllOccurrencesOfSong(mediaId: String) {
        val occurrencesBeforeCurrentlyPlayingIndex = playQueue.value?.filterIndexed { index, mediaItem ->
            index < (currentQueueItemIndex.value ?: return) && mediaItem.mediaId == mediaId
        }?.size ?: 0

        playQueue.value?.removeAll { mediaItem ->
            mediaItem.mediaId == mediaId
        }

        if (occurrencesBeforeCurrentlyPlayingIndex > 0) {
            currentQueueItemIndex.postValue(max(0,
                (currentQueueItemIndex.value ?: return) -
                        occurrencesBeforeCurrentlyPlayingIndex))
        }
    }
}
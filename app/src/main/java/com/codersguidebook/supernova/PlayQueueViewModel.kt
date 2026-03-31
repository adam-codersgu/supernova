package com.codersguidebook.supernova

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

class PlayQueueViewModel : ViewModel() {
    var playQueue = MutableLiveData<List<MediaItem>>()
    var currentQueueItemIndex = MutableLiveData<Int>()
    var currentlyPlayingSongMetadata = MutableLiveData<MediaMetadata?>()
    var isPlaying = MutableLiveData(false)
    // TODO REVIEW THE USAGE OF ALL PENDING VARIABLES AND SEE IF YOU CAN REMOVE
    var pendingExpectedMetadata = MutableLiveData<String?>()
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
}
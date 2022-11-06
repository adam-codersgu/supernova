package com.codersguidebook.supernova

import android.support.v4.media.session.MediaSessionCompat.QueueItem
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class PlayQueueViewModel : ViewModel() {
    var playQueue = MutableLiveData<List<QueueItem>>()
    var currentQueueItemId = MutableLiveData<Long>()
    var playbackDuration = MutableLiveData<Int>()
    var playbackPosition = MutableLiveData<Int>()
    var refreshPlayQueue = MutableLiveData(false)
    // TODO: Refactor to playback state PLAYBACK_STATE -> for consistency and easy updating
    var isPlaying = MutableLiveData<Boolean>()

    fun getCurrentQueueItem(): QueueItem? {
        return playQueue.value?.find { it.queueId == currentQueueItemId.value }
    }
}
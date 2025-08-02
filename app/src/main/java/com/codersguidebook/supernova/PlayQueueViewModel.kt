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
    var pendingExpectedMetadata = MutableLiveData<String?>()
    var pendingPlayInstruction = MutableLiveData<Boolean?>()
    var pendingSeekToInstruction = MutableLiveData<Long?>()
    var pendingShuffleInstruction = MutableLiveData<Boolean?>()
    var pendingSkipToInstruction = MutableLiveData<Int?>()
    var playbackDuration = MutableLiveData<Int>()
    var playbackPosition = MutableLiveData<Int>()
}
package com.codersguidebook.supernova

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

class PlayQueueViewModel : ViewModel() {
    var playQueue = MutableLiveData<List<MediaItem>>()
    var currentQueueItemId = MutableLiveData<Int>()
    var currentlyPlayingSongMetadata = MutableLiveData<MediaMetadata?>()
    var isPlaying = MutableLiveData(false)
    var playbackDuration = MutableLiveData<Int>()
    var playbackPosition = MutableLiveData<Int>()
}
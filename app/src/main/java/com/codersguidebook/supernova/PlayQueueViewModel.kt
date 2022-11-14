package com.codersguidebook.supernova

import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat.QueueItem
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class PlayQueueViewModel : ViewModel() {
    var playQueue = MutableLiveData<List<QueueItem>>()
    var currentQueueItemId = MutableLiveData<Long>()
    var currentlyPlayingSongMetadata = MutableLiveData<MediaMetadataCompat?>()
    var playbackDuration = MutableLiveData<Int>()
    var playbackPosition = MutableLiveData<Int>()
    var refreshPlayQueue = MutableLiveData(false)
    var isPlaying = MutableLiveData<Boolean>()
}
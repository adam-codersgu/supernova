package com.codersguidebook.supernova

import android.support.v4.media.session.MediaSessionCompat.QueueItem
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.codersguidebook.supernova.entities.Song

class PlayQueueViewModel : ViewModel() {
    // TODO: Need to assess whether each property is still required
    var currentPlayQueue = MutableLiveData<List<QueueItem>>()
    var currentPlaybackDuration = MutableLiveData<Int>()
    var currentPlaybackPosition = MutableLiveData<Int>()
    var currentQueueItemId = MutableLiveData<Long>()
    var currentlyPlayingSong = MutableLiveData<Song?>()
    var isPlaying = MutableLiveData<Boolean>()
}
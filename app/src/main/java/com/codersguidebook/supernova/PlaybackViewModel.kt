package com.codersguidebook.supernova

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.codersguidebook.supernova.entities.QueueItem
import com.codersguidebook.supernova.entities.Song

class PlaybackViewModel : ViewModel() {
    var currentPlayQueue = MutableLiveData<List<QueueItem>>()
    var currentPlaybackDuration = MutableLiveData<Int>()
    var currentPlaybackPosition = MutableLiveData<Int>()
    var currentlyPlayingQueueID = MutableLiveData<Int>()
    var currentlyPlayingSong = MutableLiveData<Song?>()
    var isPlaying = MutableLiveData<Boolean>()
}
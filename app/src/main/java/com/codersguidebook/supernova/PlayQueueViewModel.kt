package com.codersguidebook.supernova

import android.util.Log

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.max

class PlayQueueViewModel : ViewModel() {
    var playQueue = MutableLiveData<List<MediaItem>>()
    var currentQueueItemIndex = MutableLiveData<Int>()
    var currentlyPlayingSongMetadata = MutableLiveData<MediaMetadata?>()
    var isPlaying = MutableLiveData(false)
    var pendingSeekToInstruction = MutableLiveData<Long?>()
    var playbackDuration = MutableLiveData<Int>()
    var playbackPosition = MutableLiveData<Int>()

    private var deletingSongs = false
    private var pendingSongsToDelete = mutableListOf<String>()

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

    fun removeAllOccurrencesOfSong(mediaId: String) = viewModelScope.launch(Dispatchers.Main) {
        pendingSongsToDelete.add(mediaId)

        if (deletingSongs) {
            return@launch
        }

        deletingSongs = true

        removeOccurrences()

        deletingSongs = false
    }

    private fun removeOccurrences() {
        if (pendingSongsToDelete.isEmpty()) return
        do {
            val mediaId = pendingSongsToDelete[0]
            pendingSongsToDelete.removeAt(0)

            val occurrencesBeforeCurrentlyPlayingIndex = playQueue.value?.filterIndexed { index, mediaItem ->
                index < (currentQueueItemIndex.value ?: return) && mediaItem.mediaId == mediaId
            }?.size ?: 0

            Log.i("DEBUG", "Removing media ID $mediaId from the play queue")
            val newPlayQueue = playQueue.value?.toMutableList() ?: return
            newPlayQueue.removeAll { mediaItem ->
                mediaItem.mediaId == mediaId
            }
            playQueue.value = newPlayQueue

            if (occurrencesBeforeCurrentlyPlayingIndex > 0) {
                currentQueueItemIndex.value = max(0,
                    (currentQueueItemIndex.value ?: return) -
                            occurrencesBeforeCurrentlyPlayingIndex)
            }
        } while (pendingSongsToDelete.isNotEmpty())
    }
}
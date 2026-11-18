package com.codersguidebook.supernova

import androidx.fragment.app.DialogFragment
import com.codersguidebook.supernova.entities.Song
import kotlinx.coroutines.Job

interface MainActivityFragments {

    fun fastForward()
    fun fastRewind()
    fun hideKeyboard()
    fun hideStatusBars(hide: Boolean)
    fun iconifySearchView()
    fun notifyQueueItemMoved(oldIndex: Int, newIndex: Int)
    fun openAddToPlaylistDialog(songs: List<Song>): Job
    fun openDialog(dialog: DialogFragment)
    fun playNewPlayQueue(songs: List<Song>, startIndex: Int = 0, shuffle: Boolean = false)
    fun playPauseControl()
    fun seekTo(position: Long)
    fun skipBack()
    fun skipForward()
    fun toggleRepeatMode(): Int
    fun toggleShuffleMode(): Boolean
    fun updateSongs(songs: List<Song>)
}
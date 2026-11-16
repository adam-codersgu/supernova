package com.codersguidebook.supernova

import com.codersguidebook.supernova.entities.Song
import kotlinx.coroutines.Job

interface MainActivityFragments {

    fun fastForward()
    fun fastRewind()
    fun hideStatusBars(hide: Boolean)
    fun openAddToPlaylistDialog(songs: List<Song>): Job
    fun playPauseControl()
    fun seekTo(position: Long)
    fun skipBack()
    fun skipForward()
    fun toggleRepeatMode(): Int
    fun toggleShuffleMode(): Boolean
    fun updateSongs(songs: List<Song>)
}
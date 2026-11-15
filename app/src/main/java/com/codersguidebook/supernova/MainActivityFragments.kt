package com.codersguidebook.supernova

import com.codersguidebook.supernova.entities.Song

interface MainActivityFragments {

    fun fastForward()
    fun fastRewind()
    fun playPauseControl()
    fun skipBack()
    fun skipForward()
    fun updateSongs(songs: List<Song>)
}
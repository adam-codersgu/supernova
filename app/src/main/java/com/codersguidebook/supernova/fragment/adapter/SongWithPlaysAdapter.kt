package com.codersguidebook.supernova.fragment.adapter

import com.codersguidebook.supernova.entities.Song

interface SongWithPlaysAdapter {

    val songIdsAndPlays: HashMap<Long, Int>
        get() = hashMapOf()

    fun refreshSongPlays(newSongPlays: Map<Long, Int>)

    fun getSongIndicesToRefresh(newSongPlays: Map<Long, Int>, songs: List<Song>): MutableList<Int> {
        val songIdsToRefresh = mutableListOf<Long>()
        for ((songId, qtyOfPlays) in newSongPlays) {
            if (qtyOfPlays != songIdsAndPlays[songId]) {
                songIdsToRefresh.add(songId)
            }
        }

        loadSongPlays(newSongPlays)

        val songIndicesToRefresh = mutableListOf<Int>()
        for (songId in songIdsToRefresh) {
            songIndicesToRefresh.add(songs.indexOfFirst { it.songId == songId })
        }
        songIndicesToRefresh.sort()

        return songIndicesToRefresh
    }

    private fun loadSongPlays(songPlays: Map<Long, Int>) {
        songIdsAndPlays.clear()
        songIdsAndPlays.putAll(songPlays)
    }
}
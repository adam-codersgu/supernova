package com.codersguidebook.supernova.fragment.adapter

import com.codersguidebook.supernova.entities.Song

interface SongWithPlaysAdapter {

    fun refreshSongPlays(newSongPlays: Map<Long, Int>)

    fun getSongIndicesToRefresh(existingSongPlays: HashMap<Long, Int>, newSongPlays: Map<Long, Int>, songs: List<Song>): MutableList<Int> {
        val songIdsToRefresh = mutableListOf<Long>()
        for ((songId, qtyOfPlays) in newSongPlays) {
            if (qtyOfPlays != existingSongPlays[songId]) {
                songIdsToRefresh.add(songId)
            }
        }

        loadSongPlays(existingSongPlays, newSongPlays)

        val songIndicesToRefresh = mutableListOf<Int>()
        for (songId in songIdsToRefresh) {
            songIndicesToRefresh.add(songs.indexOfFirst { it.songId == songId })
        }
        songIndicesToRefresh.sort()

        return songIndicesToRefresh
    }

    private fun loadSongPlays(existingSongPlays: HashMap<Long, Int>, newSongPlays: Map<Long, Int>) {
        existingSongPlays.clear()
        existingSongPlays.putAll(newSongPlays)
    }
}
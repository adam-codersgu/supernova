package com.codersguidebook.supernova.recyclerview.adapter

import androidx.recyclerview.widget.RecyclerView
import com.codersguidebook.supernova.MainActivity
import com.codersguidebook.supernova.entities.Song

abstract class SongAdapter(activity: MainActivity): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    val songs = mutableListOf<Song>()

    override fun getItemCount() = songs.size

    /**
     * Handle updates to the content of the RecyclerView. The below method will determine what
     * changes are required when an element/elements is/are changed, inserted, or deleted.
     *
     * @param index - The index of the current iteration through the up-to-date content list.
     * @param song - The Song object that should be displayed at the index.
     */
    abstract fun processLoopIteration(index: Int, song: Song)
}
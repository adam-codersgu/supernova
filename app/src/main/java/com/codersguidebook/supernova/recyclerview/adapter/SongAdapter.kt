package com.codersguidebook.supernova.recyclerview.adapter

import androidx.recyclerview.widget.RecyclerView
import com.codersguidebook.supernova.entities.Song

abstract class SongAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    val songs = mutableListOf<Song>()

    override fun getItemCount() = songs.size

    /**
     * Handle updates to the content of the RecyclerView. The below method will determine what
     * changes are required when an element/elements is/are changed, inserted, or deleted.
     *
     * @param index - The index of the current iteration through the up-to-date content list.
     * @param song - The Song object that should be displayed at the index.
     */
    fun processLoopIteration(index: Int, song: Song) {
        val recyclerViewIndex = getRecyclerViewIndex(index)
        when {
            index >= songs.size -> {
                songs.add(song)
                notifyItemInserted(recyclerViewIndex)
            }
            song.songId != songs[index].songId -> {
                var numberOfItemsRemoved = 0
                do {
                    songs.removeAt(index)
                    ++numberOfItemsRemoved
                } while (index < songs.size &&
                    song.songId != songs[index].songId)

                when {
                    numberOfItemsRemoved == 1 -> notifyItemRemoved(recyclerViewIndex)
                    numberOfItemsRemoved > 1 -> notifyItemRangeRemoved(recyclerViewIndex, numberOfItemsRemoved)
                }

                processLoopIteration(index, song)
            }
            song != songs[index] -> {
                songs[index] = song
                notifyItemChanged(recyclerViewIndex)
            }
        }
    }

    /**
     * Convenience method for retrieving the target index of RecyclerView element updates.
     * RecyclerViews that contain a header will need to add + 1 to the index.
     *
     * @param index - The index of the target RecyclerView element.
     * @return The index at which updates should be applied, accommodating for any headers.
     * Default - The supplied index.
     */
    open fun getRecyclerViewIndex(index: Int): Int = index
}
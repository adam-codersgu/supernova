package com.codersguidebook.supernova.fragment.adapter

import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.codersguidebook.supernova.MainActivity
import com.codersguidebook.supernova.R
import com.codersguidebook.supernova.dialogs.SongOptions
import com.codersguidebook.supernova.entities.Song

abstract class SongAdapter(private val activity: MainActivity): Adapter() {
    val songs = mutableListOf<Song>()

    open inner class ViewHolderSong(itemView: View) : RecyclerView.ViewHolder(itemView) {

        internal var mArtwork = itemView.findViewById(R.id.artwork) as ImageView?
        internal var mTitle = itemView.findViewById(R.id.title) as TextView
        internal var mSubtitle = itemView.findViewById(R.id.subtitle) as TextView
        internal var mMenu = itemView.findViewById(R.id.menu) as ImageButton?

        init {
            itemView.rootView.isClickable = true
            itemView.rootView.setOnClickListener {
                activity.playNewPlayQueue(songs, layoutPosition)
            }

            itemView.rootView.setOnLongClickListener {
                activity.openDialog(SongOptions(songs[layoutPosition]))
                return@setOnLongClickListener true
            }

            mMenu?.setOnClickListener {
                activity.openDialog(SongOptions(songs[layoutPosition]))
            }
        }
    }

    override fun getItemCount() = songs.size

    /**
     * Handle updates to the content of the RecyclerView. The below method will determine what
     * changes are required when an element/elements is/are changed, inserted, or deleted.
     *
     * @param index The index of the current iteration through the up-to-date content list.
     * @param song The Song object that should be displayed at the index.
     */
    override fun processLoopIteration(index: Int, song: Song) {
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
     * Handle updates to the content of the RecyclerView. The below method will determine what
     * changes are required when an element/elements is/are changed, inserted, or deleted.
     * This enhanced process loop iteration method assumes each song can only appear once.
     *
     * @param newSongs The new list of Song objects that should be displayed.
     */
    fun processNewSongs(newSongs: List<Song>) {
        for ((index, song) in newSongs.withIndex()) {
            val recyclerViewIndex = getRecyclerViewIndex(index)
            when {
                index >= songs.size -> {
                    songs.add(song)
                    notifyItemInserted(recyclerViewIndex)
                }
                song.songId != songs[index].songId -> {
                    // Find if the song has been moved elsewhere
                    val newIndex = newSongs.indexOfFirst { it.songId == song.songId }

                    if (newIndex != -1) {
                        val songMetadataChanged = song == songs[newIndex]
                        songs.removeAt(index)
                        songs.add(newIndex, song)

                        val newRecyclerViewIndex = getRecyclerViewIndex(newIndex)
                        if (songMetadataChanged) {
                            notifyItemRemoved(recyclerViewIndex)
                            notifyItemInserted(newRecyclerViewIndex)
                        } else {
                            notifyItemMoved(recyclerViewIndex, newRecyclerViewIndex)
                        }
                    } else {
                        // The song is no longer present. Remove it and all other deleted
                        // songs that immediately followed it in the list.
                        var numberOfItemsRemoved = 0
                        do {
                            songs.removeAt(index)
                            ++numberOfItemsRemoved
                        } while (index < songs.size &&
                            newSongs.find { it.songId == songs[index].songId } == null)

                        when {
                            numberOfItemsRemoved == 1 -> notifyItemRemoved(recyclerViewIndex)
                            numberOfItemsRemoved > 1 -> notifyItemRangeRemoved(recyclerViewIndex,
                                numberOfItemsRemoved)
                        }
                    }
                }
                song != songs[index] -> {
                    songs[index] = song
                    notifyItemChanged(recyclerViewIndex)
                }
            }
        }

        if (songs.size > newSongs.size) {
            val numberItemsToRemove = songs.size - newSongs.size
            repeat(numberItemsToRemove) { songs.removeLast() }
            notifyItemRangeRemoved(getRecyclerViewIndex(newSongs.size), numberItemsToRemove)
        }
    }
}
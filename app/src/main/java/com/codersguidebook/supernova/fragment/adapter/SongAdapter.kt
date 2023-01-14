package com.codersguidebook.supernova.fragment.adapter

import android.util.Log
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
        // TODO: On resume, log the details of the incoming song, and the song that currently exists at that index (if exists)
        //  Then, log which branch of the when block is triggered (have a log in an else block also to catch unmatched cases)
        for ((index, song) in newSongs.withIndex()) {
            val recyclerViewIndex = getRecyclerViewIndex(index)
            when {
                index >= songs.size -> {
                    // fixme: note the logs that we have here
                    Log.e("DEBUGGING SongAdapter", "When block branch 1")
                    songs.add(song)
                    notifyItemInserted(recyclerViewIndex)
                }
                song.songId != songs[index].songId -> {
                    Log.e("DEBUGGING SongAdapter", "When block branch 2")
                    // fixme: update other areas of the application that use an algo like this

                    // Check if the song is a new entry to the list
                    val songIsNewEntry = songs.find { it.songId == song.songId } == null
                    if (songIsNewEntry) {
                        songs.add(index, song)
                        notifyItemInserted(recyclerViewIndex)
                        continue
                    }

                    // Check if song(s) has/have been removed from the list
                    val songIsRemoved = newSongs.find { it.songId == songs[index].songId } == null
                    if (songIsRemoved) {
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

                        // Check if removing the song(s) has fixed the list
                        if (song.songId == songs[index].songId) continue
                    }

                    // Check if the song has been moved earlier in the list
                    val oldIndex = songs.indexOfFirst { it.songId == song.songId }
                    if (oldIndex != -1 && oldIndex > index) {
                        songs.removeAt(oldIndex)
                        songs.add(index, song)
                        notifyItemMoved(getRecyclerViewIndex(oldIndex), recyclerViewIndex)
                        continue
                    }

                    // Check if the song(s) has been moved later in the list
                    var newIndex = newSongs.indexOfFirst { it.songId == songs[index].songId }
                    if (newIndex != -1) {
                        do {
                            songs.removeAt(index)

                            if (newIndex <= songs.size) {
                                songs.add(newIndex, song)
                                notifyItemMoved(recyclerViewIndex, getRecyclerViewIndex(newIndex))
                            } else {
                                notifyItemRemoved(recyclerViewIndex)
                            }

                            // See if further songs need to be moved
                            newIndex = newSongs.indexOfFirst { it.songId == songs[index].songId }
                        } while (index < songs.size &&
                            song.songId != songs[index].songId &&
                            newIndex != -1)

                        // Check if moving the song(s) has fixed the list
                        if (song.songId == songs[index].songId) continue
                        else {
                            songs.add(index, song)
                            notifyItemInserted(recyclerViewIndex)
                        }
                    }
                }
                song != songs[index] -> {
                    Log.e("DEBUGGING SongAdapter", "When block branch 3")
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
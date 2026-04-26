package com.codersguidebook.supernova.fragment.adapter

import android.view.View
import com.codersguidebook.supernova.MainActivity
import com.codersguidebook.supernova.dialogs.SongOptions
import com.codersguidebook.supernova.entities.Song
import com.codersguidebook.supernova.fragment.BaseDialogFragment
import com.codersguidebook.supernova.fragment.adapter.viewholder.ViewHolderArtworkWithMenu

abstract class SongAdapter(private val activity: MainActivity): Adapter() {
    val songs = mutableListOf<Song>()

    open inner class ViewHolderSong(itemView: View) : ViewHolderArtworkWithMenu(itemView) {

        override fun getActivity(): MainActivity {
            return activity
        }

        override fun rootViewAction() {
            activity.playNewPlayQueue(songs, layoutPosition)
        }

        override fun getOptionsDialog(): BaseDialogFragment {
            return SongOptions(songs[layoutPosition])
        }
    }

    override fun getItemCount() = songs.size

    override fun itemsEqual(item1: Any, item2: Any): Boolean {
        return (item1 as Song).songId == (item2 as Song).songId
    }

    override fun itemShouldBeUpdated(item: Any, index: Int): Boolean {
        return (item as Song) != (items[index] as Song)
    }
}
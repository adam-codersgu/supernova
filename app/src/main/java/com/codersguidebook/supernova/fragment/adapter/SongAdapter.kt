package com.codersguidebook.supernova.fragment.adapter

import android.view.View
import com.codersguidebook.supernova.MainActivity
import com.codersguidebook.supernova.dialogs.SongOptions
import com.codersguidebook.supernova.entities.Song
import com.codersguidebook.supernova.fragment.BaseDialogFragment
import com.codersguidebook.supernova.fragment.adapter.viewholder.ViewHolderArtworkWithMenu

abstract class SongAdapter(private val activity: MainActivity): Adapter() {

    open inner class ViewHolderSong(itemView: View) : ViewHolderArtworkWithMenu(itemView) {

        override fun getActivity(): MainActivity {
            return activity
        }

        @Suppress("UNCHECKED_CAST")
        override fun rootViewAction() {
            activity.playNewPlayQueue((items as List<Song>), layoutPosition)
        }

        override fun getOptionsDialog(): BaseDialogFragment {
            return SongOptions((items[layoutPosition] as Song))
        }
    }

    override fun itemsEqual(item1: Any, item2: Any): Boolean {
        return (item1 as Song).songId == (item2 as Song).songId
    }

    override fun itemShouldBeUpdated(item: Any, index: Int): Boolean {
        return (item as Song) != (items[index] as Song)
    }
}
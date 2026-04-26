package com.codersguidebook.supernova.fragment.adapter

import android.view.View
import com.codersguidebook.supernova.MainActivity
import com.codersguidebook.supernova.dialogs.SongOptions
import com.codersguidebook.supernova.entities.Artist
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

    open fun removeSong(index: Int) {
        songs.removeAt(index)
    }

    override fun itemsEqual(item: Any, index: Int): Boolean {
        return (item as Song).songId == (items[index] as Song).songId
    }

    override fun findItem(item: Any): Any? {
        val what = (Any as Song).songId == (item as Song).songId
        return items.find {
            (it as Song).songId == (item as Song).songId
        }
    }

    override fun findItemIndex(item: Any): Int {
        return items.indexOfFirst {
            (it as Song).songId == (item as Song).songId
        }
    }

    override fun itemShouldBeUpdated(item: Any, index: Int): Boolean {
        return (item as Song) != (items[index] as Song)
    }
}
package com.codersguidebook.supernova.fragment.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.codersguidebook.supernova.MainActivity
import com.codersguidebook.supernova.R

abstract class SongWithHeaderAdapter(activity: MainActivity): SongAdapter(activity) {

    companion object {
        const val HEADER = 1
        const val SONG = 2
    }

    open inner class ViewHolderHeader(itemView: View) : RecyclerView.ViewHolder(itemView) {

        internal var mArtwork = itemView.findViewById<View>(R.id.artwork) as ImageView
        internal var mTitle = itemView.findViewById<View>(R.id.title) as TextView
        internal var mArtist = itemView.findViewById<View>(R.id.subtitle) as TextView
        internal var mSongCount = itemView.findViewById<View>(R.id.subtitle2) as TextView
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) HEADER
        else SONG
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == HEADER) ViewHolderHeader(
            LayoutInflater.from(parent.context).inflate(R.layout.header, parent, false)
        ) else ViewHolderSong(
            LayoutInflater.from(parent.context).inflate(R.layout.item_with_menu, parent, false)
        )
    }

    override fun getItemCount() = songs.size + 1

    override fun getRecyclerViewIndex(index: Int): Int = index + 1
}
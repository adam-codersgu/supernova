package com.codersguidebook.supernova.fragment.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.codersguidebook.supernova.MainActivity
import com.codersguidebook.supernova.R
import com.codersguidebook.supernova.fragment.adapter.viewholder.ViewHolderHeader

abstract class SongWithHeaderAdapter(activity: MainActivity): SongAdapter(activity) {

    companion object {
        const val HEADER = 1
        const val SONG = 2
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

    override fun getItemCount() = items.size + 1

    override fun getRecyclerViewIndex(index: Int): Int = index + 1
}
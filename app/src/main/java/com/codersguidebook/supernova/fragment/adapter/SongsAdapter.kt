package com.codersguidebook.supernova.fragment.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.codersguidebook.supernova.MainActivity
import com.codersguidebook.supernova.R
import com.codersguidebook.supernova.utils.ImageHandlingHelper
import com.codersguidebook.supernova.views.RecyclerViewScrollbar

class SongsAdapter(private val activity: MainActivity): SongAdapter(activity),
    RecyclerViewScrollbar.ValueLabelListener {

    override fun getValueLabelText(position: Int): String {
        return songs[position].title[0].uppercase()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ViewHolderSong(
            LayoutInflater.from(parent.context).inflate(R.layout.item_with_artwork_preview, parent, false)
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        holder as ViewHolderSong
        val current = songs[position]

        ImageHandlingHelper.loadImageByAlbumId(activity.application, current.albumId, holder.mArtwork!!)

        holder.mTitle.text = current.title
        holder.mSubtitle.text = current.artist
    }
}
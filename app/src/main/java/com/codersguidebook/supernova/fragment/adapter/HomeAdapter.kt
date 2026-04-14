package com.codersguidebook.supernova.fragment.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.codersguidebook.supernova.MainActivity
import com.codersguidebook.supernova.R
import com.codersguidebook.supernova.dialogs.SongOptions
import com.codersguidebook.supernova.fragment.BaseDialogFragment
import com.codersguidebook.supernova.fragment.adapter.viewholder.ViewHolder
import com.codersguidebook.supernova.utils.DimensionsHelper
import com.codersguidebook.supernova.utils.ImageHandlingHelper

open class HomeAdapter(private val activity: MainActivity): SongAdapter(activity) {

    open inner class ViewHolderHome(itemView: View) : ViewHolder(itemView) {

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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ViewHolderHome(LayoutInflater.from(parent.context)
            .inflate(R.layout.small_preview, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        holder as ViewHolderHome
        holder.itemView.rootView.layoutParams.width = DimensionsHelper.convertToDp(activity, 100f)

        val current = songs[position]

        ImageHandlingHelper.loadImageByAlbumId(activity.application, current.albumId,
            holder.mArtwork
        )

        holder.mTitle.text = current.title ?: activity.getString(R.string.default_title)
        holder.mSubtitle.text = current.artist ?: activity.getString(R.string.default_artist)
    }
}
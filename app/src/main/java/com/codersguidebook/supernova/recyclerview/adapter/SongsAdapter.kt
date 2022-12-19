package com.codersguidebook.supernova.recyclerview.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.codersguidebook.supernova.MainActivity
import com.codersguidebook.supernova.R
import com.codersguidebook.supernova.SongOptions
import com.codersguidebook.supernova.utils.ImageHandlingHelper
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView

class SongsAdapter(private val activity: MainActivity): SongAdapter(),
    FastScrollRecyclerView.SectionedAdapter {

    override fun getSectionName(position: Int): String {
        return songs[position].title[0].uppercase()
    }

    inner class ViewHolderSong(itemView: View) : RecyclerView.ViewHolder(itemView) {

        internal var mArtwork = itemView.findViewById<View>(R.id.artwork) as ImageView
        internal var mTitle = itemView.findViewById<View>(R.id.title) as TextView
        internal var mArtist = itemView.findViewById<View>(R.id.subtitle) as TextView
        private var mMenu = itemView.findViewById<ImageButton>(R.id.menu)

        init {
            itemView.rootView.isClickable = true
            itemView.rootView.setOnClickListener {
                activity.playNewPlayQueue(songs, layoutPosition)
            }

            itemView.rootView.setOnLongClickListener{
                activity.openDialog(SongOptions(songs[layoutPosition]))
                return@setOnLongClickListener true
            }

            mMenu.setOnClickListener {
                activity.openDialog(SongOptions(songs[layoutPosition]))
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ViewHolderSong(
            LayoutInflater.from(parent.context).inflate(R.layout.item_with_artwork_preview, parent, false)
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        holder as ViewHolderSong
        val current = songs[position]

        ImageHandlingHelper.loadImageByAlbumId(activity.application, current.albumId, holder.mArtwork)

        holder.mTitle.text = current.title
        holder.mArtist.text = current.artist
    }
}
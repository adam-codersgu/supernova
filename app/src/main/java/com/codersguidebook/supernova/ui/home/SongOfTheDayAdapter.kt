package com.codersguidebook.supernova.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.codersguidebook.supernova.MainActivity
import com.codersguidebook.supernova.R
import com.codersguidebook.supernova.SongOptions
import com.codersguidebook.supernova.fragment.adapter.SongAdapter
import com.codersguidebook.supernova.utils.ImageHandlingHelper

class SongOfTheDayAdapter(private val mainActivity: MainActivity): SongAdapter() {

    // TODO: Perhaps add header pane that displays the no content layout if no contents in the adapter
    //  binding.songOfTheDayNoContent.isVisible = true

    inner class ViewHolderSong(itemView: View) :
        RecyclerView.ViewHolder(itemView),
        View.OnClickListener {

        internal var mArtwork = itemView.findViewById<View>(R.id.largeSongArtwork) as ImageView
        internal var mTitle = itemView.findViewById<View>(R.id.largeTitle) as TextView
        internal var mArtist = itemView.findViewById<View>(R.id.largeSubtitle) as TextView
        internal var mAlbum = itemView.findViewById<View>(R.id.largeSubtitle2) as TextView

        init {
            itemView.isClickable = true
            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener{
                mainActivity.openDialog(SongOptions(songs[layoutPosition]))
                return@setOnLongClickListener true
            }
        }

        override fun onClick(view: View) {
            mainActivity.playNewPlayQueue(songs, layoutPosition)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ViewHolderSong(LayoutInflater.from(parent.context).inflate(R.layout.large_preview, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        holder as ViewHolderSong

        val song = songs[position]

        ImageHandlingHelper.loadImageByAlbumId(mainActivity.application, song.albumId, holder.mArtwork)
        holder.mTitle.text = song.title
        holder.mArtist.text = song.artist
        holder.mAlbum.text = song.albumName
    }
}
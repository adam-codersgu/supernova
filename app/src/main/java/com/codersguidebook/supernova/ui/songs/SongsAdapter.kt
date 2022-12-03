package com.codersguidebook.supernova.ui.songs

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
import com.codersguidebook.supernova.entities.Song
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView

class SongsAdapter(private val activity: MainActivity):
    RecyclerView.Adapter<SongsAdapter.SongsViewHolder>(), FastScrollRecyclerView.SectionedAdapter {
    var songs = mutableListOf<Song>()

    override fun getSectionName(position: Int): String {
        return songs[position].title[0].toUpperCase().toString()
    }

    inner class SongsViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView),
        View.OnClickListener {

        internal var mArtwork = itemView.findViewById<View>(R.id.artwork) as ImageView
        internal var mTitle = itemView.findViewById<View>(R.id.title) as TextView
        internal var mArtist = itemView.findViewById<View>(R.id.subtitle) as TextView
        internal var mBtnMenu = itemView.findViewById<ImageButton>(R.id.menu)

        init {
            itemView.isClickable = true
            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener{
                activity.openDialog(SongOptions(songs[layoutPosition]))
                return@setOnLongClickListener true
            }
        }

        override fun onClick(view: View) {
            activity.playNewPlayQueue(songs, layoutPosition)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongsViewHolder {
        return SongsViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.preview, parent, false))
    }

    override fun onBindViewHolder(holder: SongsViewHolder, position: Int) {
        val current = songs[position]

        activity.insertArtwork(current.albumId, holder.mArtwork)

        holder.mTitle.text = current.title
        holder.mArtist.text = current.artist
        holder.mBtnMenu.setOnClickListener {
            activity.openDialog(SongOptions(current))
        }
    }

    override fun getItemCount() = songs.size
}
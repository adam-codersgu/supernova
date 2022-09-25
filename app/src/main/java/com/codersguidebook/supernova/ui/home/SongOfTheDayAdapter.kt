package com.codersguidebook.supernova.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.codersguidebook.supernova.*
import com.codersguidebook.supernova.entities.Song

class SongOfTheDayAdapter(private val mainActivity: MainActivity):
    RecyclerView.Adapter<SongOfTheDayAdapter.SongsViewHolder>() {
    var song: Song? = null

    inner class SongsViewHolder(itemView: View) :
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
                mainActivity.openDialog(SongOptions(song!!))
                return@setOnLongClickListener true
            }
        }

        override fun onClick(view: View) {
            mainActivity.playNewSongs(listOf(song!!), 0, false)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongsViewHolder {
        return SongsViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.large_preview, parent, false))
    }

    override fun onBindViewHolder(holder: SongsViewHolder, position: Int) {
        mainActivity.insertArtwork(song?.albumID, holder.mArtwork)
        holder.mTitle.text = song?.title
        holder.mArtist.text = song?.artist
        holder.mAlbum.text = song?.album
    }

    internal fun changeItem(newSong: Song) {
        if (song == null) {
            song = newSong
            notifyItemInserted(0)
        } else {
            song = newSong
            notifyItemChanged(0)
        }
    }

    override fun getItemCount() = if (song != null) 1
    else 0
}
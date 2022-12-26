package com.codersguidebook.supernova.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.codersguidebook.supernova.MainActivity
import com.codersguidebook.supernova.R
import com.codersguidebook.supernova.SongOptions
import com.codersguidebook.supernova.fragment.adapter.SongAdapter
import com.codersguidebook.supernova.utils.ImageHandlingHelper

class MostPlayedAdapter(private val activity: MainActivity): SongAdapter() {

    inner class ViewHolderSong(itemView: View) :
        RecyclerView.ViewHolder(itemView),
        View.OnClickListener {

        internal var mArtwork = itemView.findViewById<View>(R.id.smallSongArtwork) as ImageView
        internal var mTitle = itemView.findViewById<View>(R.id.smallSongTitle) as TextView
        internal var mArtist = itemView.findViewById<View>(R.id.smallSongArtistOrCount) as TextView
        internal var mPlays = itemView.findViewById<View>(R.id.smallSongPlays) as TextView

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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ViewHolderSong(LayoutInflater.from(parent.context).inflate(R.layout.small_song_preview, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        holder as ViewHolderSong

        val current = songs[position]
        holder.mPlays.isVisible = true

        when (position) {
            0 -> {
                holder.mTitle.setTextColor(ContextCompat.getColor(activity, R.color.gold))
                holder.mArtist.setTextColor(ContextCompat.getColor(activity, R.color.gold60))
                holder.mPlays.setTextColor(ContextCompat.getColor(activity, R.color.gold60))
            }
            1 -> {
                holder.mTitle.setTextColor(ContextCompat.getColor(activity, R.color.silver))
                holder.mArtist.setTextColor(ContextCompat.getColor(activity, R.color.silver60))
                holder.mPlays.setTextColor(ContextCompat.getColor(activity, R.color.silver60))
            }
            2 -> {
                holder.mTitle.setTextColor(ContextCompat.getColor(activity, R.color.bronze))
                holder.mArtist.setTextColor(ContextCompat.getColor(activity, R.color.bronze60))
                holder.mPlays.setTextColor(ContextCompat.getColor(activity, R.color.bronze60))
            }
            else -> {
                holder.mTitle.setTextColor(ContextCompat.getColor(activity, android.R.color.white))
                holder.mArtist.setTextColor(ContextCompat.getColor(activity, R.color.onSurface60))
                holder.mPlays.setTextColor(ContextCompat.getColor(activity, R.color.onSurface60))
            }
        }

        ImageHandlingHelper.loadImageByAlbumId(activity.application, current.albumId, holder.mArtwork)

        holder.mTitle.text = current.title
        holder.mArtist.text = current.artist

        val plays = current.plays
        holder.mPlays.text = if (plays == 1) {
            activity.getString(R.string.one_play)
        } else {
            activity.getString(R.string.n_plays, plays)
        }
    }
}
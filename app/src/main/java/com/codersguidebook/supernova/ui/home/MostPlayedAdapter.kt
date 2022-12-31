package com.codersguidebook.supernova.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.codersguidebook.supernova.MainActivity
import com.codersguidebook.supernova.R
import com.codersguidebook.supernova.fragment.adapter.HomeAdapter
import com.codersguidebook.supernova.utils.ImageHandlingHelper

class MostPlayedAdapter(private val activity: MainActivity) : HomeAdapter(activity) {

    inner class ViewHolderMostPlayedSong(itemView: View) : HomeAdapter.ViewHolderSong(itemView) {

        internal var mPlays = itemView.findViewById<View>(R.id.plays) as TextView

        init {
            mPlays.isVisible = true
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ViewHolderMostPlayedSong(LayoutInflater.from(parent.context)
            .inflate(R.layout.small_home_song_with_plays, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        holder as ViewHolderMostPlayedSong
        val current = songs[position]

        ImageHandlingHelper.loadImageByAlbumId(activity.application, current.albumId, holder.mArtwork)

        holder.mTitle.text = current.title
        holder.mArtist.text = current.artist

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

        val plays = current.plays
        holder.mPlays.text = if (plays == 1) {
            activity.getString(R.string.one_play)
        } else {
            activity.getString(R.string.n_plays, plays)
        }
    }
}
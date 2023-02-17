package com.codersguidebook.supernova.fragment.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.codersguidebook.supernova.MainActivity
import com.codersguidebook.supernova.R
import com.codersguidebook.supernova.entities.Song
import com.codersguidebook.supernova.utils.ImageHandlingHelper
import com.google.android.material.color.MaterialColors

class MostPlayedAdapter(private val activity: MainActivity) : HomeAdapter(activity) {

    inner class ViewHolderMostPlayedSong(itemView: View) : ViewHolderSong(itemView) {

        internal var mPlays = itemView.findViewById(R.id.plays) as TextView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ViewHolderMostPlayedSong(LayoutInflater.from(parent.context)
            .inflate(R.layout.small_preview_with_plays, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        holder as ViewHolderMostPlayedSong
        val current = songs[position]

        ImageHandlingHelper.loadImageByAlbumId(activity.application, current.albumId, holder.mArtwork!!)

        holder.mTitle.text = current.title ?: activity.getString(R.string.default_title)
        holder.mSubtitle.text = current.artist ?: activity.getString(R.string.default_artist)

        val primaryText = when (position) {
            0 -> ContextCompat.getColor(activity, R.color.gold)
            1 -> ContextCompat.getColor(activity, R.color.silver)
            2 -> ContextCompat.getColor(activity, R.color.bronze)
            else -> MaterialColors.getColor(activity, R.attr.colorOnSurface, Color.LTGRAY)
        }
        val secondaryText = MaterialColors.compositeARGBWithAlpha(primaryText, 153)

        holder.mTitle.setTextColor(primaryText)
        holder.mSubtitle.setTextColor(secondaryText)
        holder.mPlays.setTextColor(secondaryText)

        val plays = current.plays
        holder.mPlays.text = if (plays == 1) {
            activity.getString(R.string.one_play)
        } else {
            activity.getString(R.string.n_plays, plays)
        }
    }

    override fun processNewSongs(newSongs: List<Song>) {
        val positionsToUpdate = mutableListOf<Int>()
        for ((index, song) in newSongs.withIndex()) {
            if (index >= songs.size) break
            val currentSong =  songs[index]

            if (index < 3 && (song.title != currentSong.title || song.artist != currentSong.title
                    || song.plays != currentSong.plays)) {
                positionsToUpdate.add(index)
            }
        }

        super.processNewSongs(newSongs)

        for (position in positionsToUpdate) notifyItemChanged(position)
    }
}
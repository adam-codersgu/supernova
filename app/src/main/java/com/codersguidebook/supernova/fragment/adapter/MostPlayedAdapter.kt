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
import kotlin.math.min

class MostPlayedAdapter(private val activity: MainActivity) : HomeAdapter(activity) {

    private val songIdsAndPlays = hashMapOf<Long, Int>()

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
            else -> MaterialColors.getColor(activity, com.google.android.material.R.attr.colorOnSurface, Color.LTGRAY)
        }
        val secondaryText = MaterialColors.compositeARGBWithAlpha(primaryText, 153)

        holder.mTitle.setTextColor(primaryText)
        holder.mSubtitle.setTextColor(secondaryText)
        holder.mPlays.setTextColor(secondaryText)

        val plays = songIdsAndPlays[current.songId] ?: 0
        holder.mPlays.text = if (plays == 1) {
            activity.getString(R.string.one_play)
        } else {
            activity.getString(R.string.n_plays, plays)
        }
    }

    fun addNewListOfSongs(newSongs: List<Song>, songPlays: Map<Long, Int>) {
        if (songs.isNotEmpty()) {
            val songsQty = songs.size
            songs.clear()
            notifyItemRangeRemoved(0, songsQty)
        }

        songs.addAll(newSongs)
        loadSongPlays(songPlays)
        notifyItemRangeInserted(0, songs.size)
    }

    fun refreshSongPlays(newSongPlays: Map<Long, Int>) {
        val songIdsToRefresh = mutableListOf<Long>()
        for ((songId, qtyOfPlays) in newSongPlays) {
            if (qtyOfPlays != songIdsAndPlays[songId]) {
                songIdsToRefresh.add(songId)
            }
        }

        loadSongPlays(newSongPlays)

        if (songIdsToRefresh.isEmpty()) return

        val songIndicesToRefresh = mutableListOf<Int>()
        for (songId in songIdsToRefresh) {
            songIndicesToRefresh.add(songs.indexOfFirst { it.songId == songId })
        }
        songIndicesToRefresh.sort()

        val rangeOfIndicesAffected = songIndicesToRefresh[songIndicesToRefresh.size - 1] - songIndicesToRefresh[0]
        val numberOfItemsToChange = if (songIndicesToRefresh[0] < 3 && rangeOfIndicesAffected < 3) {
            min(3, songIndicesToRefresh.size - 1 - songIndicesToRefresh[0])
        } else rangeOfIndicesAffected
        notifyItemRangeChanged(songIndicesToRefresh[0], numberOfItemsToChange)
    }

    private fun loadSongPlays(songPlays: Map<Long, Int>) {
        songIdsAndPlays.clear()
        songIdsAndPlays.putAll(songPlays)
    }
}
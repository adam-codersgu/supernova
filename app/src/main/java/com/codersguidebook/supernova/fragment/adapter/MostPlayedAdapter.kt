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

    private val songAndPlaysPairs = mutableListOf<Pair<Song, Int>>()

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

        val plays = songAndPlaysPairs.find { it.first.songId == current.songId }?.second ?: 0
        holder.mPlays.text = if (plays == 1) {
            activity.getString(R.string.one_play)
        } else {
            activity.getString(R.string.n_plays, plays)
        }
    }

    fun addNewListOfSongs(songsWithPlays: List<Pair<Song, Int>>) {
        if (songs.isNotEmpty()) {
            val songsQty = songs.size
            songs.clear()
            notifyItemRangeRemoved(0, songsQty)
        }

        songs.addAll(songsWithPlays.map {it.first})
        loadSongsWithPlays(songsWithPlays)
        notifyItemRangeInserted(0, songs.size)
    }

    fun refreshSongsWithPlays(songsWithPlays: List<Pair<Song, Int>>) {
        var index = 0
        val indicesToUpdate = mutableListOf<Int>()

        while (index < listOf(songsWithPlays.size, songAndPlaysPairs.size, songs.size).min()) {
            if (songsWithPlays[index].second != songAndPlaysPairs[index].second) {
                indicesToUpdate.add(index)
            }
            ++index
        }

        loadSongsWithPlays(songsWithPlays)

        if (indicesToUpdate.size == 1) {
            notifyItemChanged(indicesToUpdate[0])
        } else if (indicesToUpdate.size > 1) {
            notifyItemRangeChanged(indicesToUpdate[0],
                indicesToUpdate[indicesToUpdate.size - 1] - indicesToUpdate[0])
        }
    }

    private fun loadSongsWithPlays(songsWithPlays: List<Pair<Song, Int>>) {
        songAndPlaysPairs.clear()
        songAndPlaysPairs.addAll(songsWithPlays)
    }
}
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

class MostPlayedAdapter(private val activity: MainActivity) : HomeAdapter(activity),
    SongWithPlaysAdapter {

    private val songIdsAndPlays = hashMapOf<Long, Int>()

    inner class ViewHolderMostPlayedSong(itemView: View) : ViewHolderHome(itemView) {

        internal var mPlays: TextView = itemView.findViewById(R.id.plays)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ViewHolderMostPlayedSong(LayoutInflater.from(parent.context)
            .inflate(R.layout.small_preview_with_plays, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        holder as ViewHolderMostPlayedSong
        val current = items[position] as Song

        ImageHandlingHelper.loadImageByAlbumId(activity.application, current.albumId,
            holder.mArtwork
        )

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

    @Suppress("UNCHECKED_CAST")
    override fun refreshSongPlays(newSongPlays: Map<Long, Int>) {
        val songIndicesToRefresh = getSongIndicesToRefresh(songIdsAndPlays, newSongPlays, (items as List<Song>))
        val rangeOfIndicesAffected = songIndicesToRefresh[songIndicesToRefresh.size - 1] + 1 - songIndicesToRefresh[0]
        val numberOfItemsToChange = if (songIndicesToRefresh[0] < 3) min(3, rangeOfIndicesAffected)
        else rangeOfIndicesAffected
        notifyItemRangeChanged(songIndicesToRefresh[0], numberOfItemsToChange)
    }
}
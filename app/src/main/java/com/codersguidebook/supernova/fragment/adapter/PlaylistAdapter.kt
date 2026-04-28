package com.codersguidebook.supernova.fragment.adapter

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.codersguidebook.supernova.MainActivity
import com.codersguidebook.supernova.R
import com.codersguidebook.supernova.dialogs.PlaylistSongOptions
import com.codersguidebook.supernova.entities.Playlist
import com.codersguidebook.supernova.entities.Song
import com.codersguidebook.supernova.fragment.BaseDialogFragment
import com.codersguidebook.supernova.fragment.adapter.viewholder.ViewHolderHeaderArtworkGrid
import com.codersguidebook.supernova.ui.playlist.PlaylistFragment
import com.codersguidebook.supernova.utils.DimensionsHelper
import com.codersguidebook.supernova.utils.ImageHandlingHelper
import com.google.android.material.color.MaterialColors
import kotlin.math.min

class PlaylistAdapter(private val fragment: PlaylistFragment,
                      private val activity: MainActivity): SongWithHeaderAdapter(activity) {
    var showHandles = false
    var playlist: Playlist? = null
    private val songIdsAndPlays = hashMapOf<Long, Int>()

    inner class ViewHolderSongWithHandle(itemView: View) : ViewHolderSong(itemView) {

        internal var mPlays: TextView = itemView.findViewById(R.id.plays)

        init {
            itemView.setOnLongClickListener {
                if (!showHandles) {
                    openDialog()
                }
                return@setOnLongClickListener true
            }
        }

        @Suppress("UNCHECKED_CAST")
        override fun rootViewAction() {
            activity.playNewPlayQueue((items as List<Song>), layoutPosition - 1)
        }

        override fun getOptionsDialog(): BaseDialogFragment {
            return PlaylistSongOptions((items[layoutPosition - 1] as Song), layoutPosition - 1, playlist!!)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == HEADER) ViewHolderHeaderArtworkGrid(
            LayoutInflater.from(parent.context).inflate(R.layout.header, parent, false)
        ) else ViewHolderSongWithHandle(
            LayoutInflater.from(parent.context).inflate(R.layout.playlist_song, parent, false)
        )
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder.itemViewType) {
            HEADER -> {
                holder as ViewHolderHeaderArtworkGrid

                holder.itemView.setBackgroundColor(ContextCompat.getColor(activity, R.color.preview_background))

                if (!ImageHandlingHelper.loadImageByPlaylist(activity.application,
                        playlist ?: return, holder.mArtwork)) {
                    val albumIds = items.map { (it as Song).albumId }.distinct().shuffled()

                    when {
                        albumIds.size > 1 -> {
                            holder.mArtwork.isGone = true
                            holder.mArtworkGrid.isVisible = true
                            ImageHandlingHelper.loadImageByAlbumId(activity.application,
                                albumIds[0], holder.mArtwork1)
                            ImageHandlingHelper.loadImageByAlbumId(activity.application,
                                albumIds[1], holder.mArtwork2)
                            if (albumIds.size > 2) ImageHandlingHelper.loadImageByAlbumId(activity.application,
                                albumIds[2], holder.mArtwork3)
                            if (albumIds.size > 3)  ImageHandlingHelper.loadImageByAlbumId(activity.application,
                                albumIds[3], holder.mArtwork4)
                        }
                        items.isNotEmpty() -> {
                            holder.mArtwork.isVisible = true
                            holder.mArtworkGrid.isGone = true
                            ImageHandlingHelper.loadImageByAlbumId(activity.application,
                                albumIds[0], holder.mArtwork)
                        }
                    }
                }

                if (items.isNotEmpty()){
                    holder.mTitle.text = playlist?.name
                    holder.mSubtitle2.text = if (items.size == 1) {
                        activity.getString(R.string.displayed_song)
                    } else {
                        activity.getString(R.string.displayed_songs, items.size)
                    }
                    holder.mSubtitle.isGone = true
                }
            }
            SONG -> {
                holder as ViewHolderSongWithHandle
                val current = items[position - 1] as Song

                val params = holder.mArtwork.layoutParams as MarginLayoutParams
                val onSurfaceColour = MaterialColors.getColor(activity, com.google.android.material.R.attr.colorOnSurface, Color.LTGRAY)
                if (showHandles) {
                    holder.mArtwork.setColorFilter(MaterialColors
                        .compositeARGBWithAlpha(onSurfaceColour, 153))
                    params.width = activity.resources.getDimension(R.dimen.handle_width).toInt()
                    params.marginStart = DimensionsHelper.convertToDp(activity, 13f)
                    Glide.with(fragment)
                        .load(R.drawable.ic_drag_handle)
                        .into(holder.mArtwork)
                    holder.mArtwork.setOnTouchListener { _, event ->
                        if (event.actionMasked == MotionEvent.ACTION_DOWN) fragment.startDragging(holder)
                        return@setOnTouchListener true
                    }
                } else {
                    params.width = activity.resources.getDimension(R.dimen.artwork_preview_width).toInt()
                    params.marginStart = 0
                    holder.mArtwork.clearColorFilter()
                    holder.mArtwork.setOnTouchListener { _, _ -> return@setOnTouchListener false }
                    ImageHandlingHelper.loadImageByAlbumId(activity.application,
                        current.albumId, holder.mArtwork
                    )
                }
                holder.mArtwork.layoutParams = params

                holder.mTitle.text = current.title ?: activity.getString(R.string.default_title)
                holder.mSubtitle.text = current.artist ?: activity.getString(R.string.default_artist)

                if (playlist?.name == activity.getString(R.string.most_played)) {
                    holder.mPlays.isVisible = true
                    val plays = songIdsAndPlays[current.songId] ?: 0
                    holder.mPlays.text = if (plays == 1) {
                        activity.getString(R.string.one_play)
                    } else {
                        activity.getString(R.string.n_plays, plays)
                    }

                    val primaryText = when (position) {
                        1 -> ContextCompat.getColor(activity, R.color.gold)
                        2 -> ContextCompat.getColor(activity, R.color.silver)
                        3 -> ContextCompat.getColor(activity, R.color.bronze)
                        else -> onSurfaceColour
                    }
                    val secondaryText = MaterialColors.compositeARGBWithAlpha(primaryText, 153)

                    holder.mTitle.setTextColor(primaryText)
                    holder.mSubtitle.setTextColor(secondaryText)
                    holder.mPlays.setTextColor(secondaryText)
                    holder.mMenu.setColorFilter(secondaryText)
                }
            }
        }
    }

    internal fun manageHandles(applyHandles: Boolean){
        this.showHandles = applyHandles
        notifyItemRangeChanged(1, items.size)
    }

    fun refreshSongPlays(newSongPlays: Map<Long, Int>) {
        val songIdsToRefresh = mutableListOf<Long>()
        for ((songId, qtyOfPlays) in newSongPlays) {
            if (qtyOfPlays != songIdsAndPlays[songId]) {
                songIdsToRefresh.add(songId)
            }
        }

        if (songIdsToRefresh.isEmpty()) return

        val songIndicesToRefresh = mutableListOf<Int>()
        for (songId in songIdsToRefresh) {
            songIndicesToRefresh.add(items.indexOfFirst { (it as Song).songId == songId })
        }
        songIndicesToRefresh.sort()

        loadSongPlays(newSongPlays)

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

    override fun itemChangedCallback(index: Int) {
        val maxItemCount = min(items.size, 3)
        notifyItemRangeChanged(index, maxItemCount - index)
    }
}
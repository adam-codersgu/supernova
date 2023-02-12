package com.codersguidebook.supernova.fragment.adapter

import android.annotation.SuppressLint
import android.app.ActionBar.LayoutParams
import android.graphics.Color
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.MarginLayoutParams
import android.widget.GridLayout
import android.widget.ImageView
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
import com.codersguidebook.supernova.ui.playlist.PlaylistFragment
import com.codersguidebook.supernova.utils.DimensionsHelper
import com.codersguidebook.supernova.utils.ImageHandlingHelper
import com.google.android.material.color.MaterialColors

class PlaylistAdapter(private val fragment: PlaylistFragment,
                      private val activity: MainActivity): SongWithHeaderAdapter(activity) {
    private var showHandles = false
    var playlist: Playlist? = null

    inner class ViewHolderPlaylistHeader(itemView: View) : ViewHolderHeader(itemView) {

        internal var mArtworkGrid = itemView.findViewById(R.id.imageGrid) as GridLayout
        internal var mArtwork1 = itemView.findViewById(R.id.artwork1) as ImageView
        internal var mArtwork2 = itemView.findViewById(R.id.artwork2) as ImageView
        internal var mArtwork3 = itemView.findViewById(R.id.artwork3) as ImageView
        internal var mArtwork4 = itemView.findViewById(R.id.artwork4) as ImageView
    }

    inner class ViewHolderSongWithHandle(itemView: View) : ViewHolderSong(itemView) {

        internal var mPlays = itemView.findViewById(R.id.plays) as TextView

        init {
            itemView.rootView.setOnClickListener {
                activity.playNewPlayQueue(songs, layoutPosition - 1)
            }

            itemView.setOnLongClickListener {
                if (!showHandles) playlist?.let {
                    activity.openDialog(PlaylistSongOptions(songs, layoutPosition - 1, it))
                }
                return@setOnLongClickListener true
            }

            mMenu?.setOnClickListener {
                playlist?.let {
                    activity.openDialog(PlaylistSongOptions(songs, layoutPosition - 1, it))
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == HEADER) ViewHolderPlaylistHeader(
            LayoutInflater.from(parent.context).inflate(R.layout.header, parent, false)
        ) else ViewHolderSongWithHandle(
            LayoutInflater.from(parent.context).inflate(R.layout.playlist_song, parent, false)
        )
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder.itemViewType) {
            HEADER -> {
                holder as ViewHolderPlaylistHeader

                holder.itemView.setBackgroundColor(ContextCompat.getColor(activity, R.color.preview_background))

                if (!ImageHandlingHelper.loadImageByPlaylist(activity.application,
                        playlist ?: return, holder.mArtwork)) {
                    val albumIds = songs.map { it.albumId }.distinct().shuffled()

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
                        songs.isNotEmpty() -> ImageHandlingHelper.loadImageByAlbumId(activity.application,
                            albumIds[0], holder.mArtwork)
                    }
                }

                if (songs.isNotEmpty()){
                    holder.mTitle.text = playlist?.name
                    holder.mSongCount.text = if (songs.size == 1) {
                        activity.getString(R.string.displayed_song)
                    } else {
                        activity.getString(R.string.displayed_songs, songs.size)
                    }
                    holder.mArtist.isGone = true
                }
            }
            SONG -> {
                holder as ViewHolderSongWithHandle
                val current = songs[position -1]

                val onSurfaceColour = MaterialColors.getColor(activity, R.attr.colorOnSurface, Color.LTGRAY)
                if (showHandles) {
                    holder.mArtwork!!.setColorFilter(MaterialColors
                        .compositeARGBWithAlpha(onSurfaceColour, 153))
                    val params = holder.mArtwork!!.layoutParams as MarginLayoutParams
                    params.width = activity.resources.getDimension(R.dimen.handle_width).toInt()
                    params.marginStart = DimensionsHelper.convertToDp(activity, 13f)
                    holder.mArtwork!!.layoutParams = params
                    /* holder.mArtwork!!.layoutParams = LayoutParams(activity.resources
                        .getDimension(R.dimen.handle_width).toInt(), MATCH_PARENT).apply {
                        marginStart = DimensionsHelper.convertToDp(activity, 130f)
                        marginEnd = DimensionsHelper.convertToDp(activity, 13f)
                    } */
                    Glide.with(fragment)
                        .load(R.drawable.ic_drag_handle)
                        .into(holder.mArtwork!!)
                    holder.mArtwork!!.setOnTouchListener { _, event ->
                        if (event.actionMasked == MotionEvent.ACTION_DOWN) fragment.startDragging(holder)
                        return@setOnTouchListener true
                    }
                } else {
                    holder.mArtwork!!.layoutParams = LayoutParams(activity.resources
                        .getDimension(R.dimen.artwork_preview_width).toInt(), MATCH_PARENT).apply {
                        marginStart = 0
                    }
                    holder.mArtwork!!.clearColorFilter()
                    holder.mArtwork!!.setOnTouchListener { _, _ ->
                        return@setOnTouchListener true
                    }
                    ImageHandlingHelper.loadImageByAlbumId(activity.application,
                        current.albumId, holder.mArtwork!!)
                }

                holder.mTitle.text = current.title ?: activity.getString(R.string.default_title)
                holder.mSubtitle.text = current.artist ?: activity.getString(R.string.default_artist)

                if (playlist?.name == activity.getString(R.string.most_played)) {
                    holder.mPlays.isVisible = true
                    val plays = current.plays
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
                    holder.mMenu?.setColorFilter(secondaryText)
                }
            }
        }
    }

    internal fun manageHandles(applyHandles: Boolean){
        this.showHandles = applyHandles
        notifyItemRangeChanged(1, songs.size)
    }

    /**
     * Handle updates to the content of the RecyclerView. The below method will determine what
     * changes are required when an element/elements is/are changed, inserted, or deleted.
     * N.B. Playlist adapter uses a different update methodology to other areas of the app
     * because only the Playlist adapter may potentially have to handle duplicate identical
     * versions of the same song. For this reason, handling element moves is not feasible
     * as each element could appear more than once with no distinguishing characteristics.
     *
     * @param index The index of the current iteration through the up-to-date content list.
     * @param song The Song object that should be displayed at the index.
     */
    fun processLoopIteration(index: Int, song: Song) {
        val recyclerViewIndex = getRecyclerViewIndex(index)
        when {
            index >= songs.size -> {
                songs.add(song)
                notifyItemInserted(recyclerViewIndex)
            }
            song.songId != songs[index].songId -> {
                var numberOfItemsRemoved = 0
                do {
                    songs.removeAt(index)
                    ++numberOfItemsRemoved
                } while (index < songs.size &&
                    song.songId != songs[index].songId)

                when {
                    numberOfItemsRemoved == 1 -> notifyItemRemoved(recyclerViewIndex)
                    numberOfItemsRemoved > 1 -> notifyItemRangeRemoved(recyclerViewIndex, numberOfItemsRemoved)
                }

                processLoopIteration(index, song)
            }
            song != songs[index] -> {
                songs[index] = song
                notifyItemChanged(recyclerViewIndex)
            }
        }
    }
}
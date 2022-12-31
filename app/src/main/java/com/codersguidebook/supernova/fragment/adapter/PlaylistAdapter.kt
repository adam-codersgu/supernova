package com.codersguidebook.supernova.fragment.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.codersguidebook.supernova.MainActivity
import com.codersguidebook.supernova.PlaylistSongOptions
import com.codersguidebook.supernova.R
import com.codersguidebook.supernova.entities.Playlist
import com.codersguidebook.supernova.ui.playlist.PlaylistFragment
import com.codersguidebook.supernova.utils.ImageHandlingHelper

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
                holder as PlaylistAdapter.ViewHolderPlaylistHeader

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
                holder as PlaylistAdapter.ViewHolderSongWithHandle
                val current = songs[position -1]

                if (showHandles) {
                    holder.mArtwork!!.setColorFilter(ContextCompat
                        .getColor(fragment.requireActivity(), R.color.onSurface60))
                    holder.mArtwork!!.layoutParams.width = activity.resources
                        .getDimension(R.dimen.handle_width).toInt()
                    Glide.with(fragment)
                        .load(R.drawable.ic_drag_handle)
                        .into(holder.mArtwork!!)
                    holder.mArtwork!!.setOnTouchListener { _, event ->
                        if (event.actionMasked == MotionEvent.ACTION_DOWN) fragment.startDragging(holder)
                        return@setOnTouchListener true
                    }
                } else {
                    holder.mArtwork!!.layoutParams.width = activity.resources
                        .getDimension(R.dimen.artwork_preview_width).toInt()
                    holder.mArtwork!!.clearColorFilter()
                    holder.mArtwork!!.setOnTouchListener { _, _ ->
                        return@setOnTouchListener true
                    }
                    ImageHandlingHelper.loadImageByAlbumId(activity.application,
                        current.albumId, holder.mArtwork!!)
                }

                holder.mTitle.text = current.title
                holder.mSubtitle.text = current.artist

                if (playlist?.name == activity.getString(R.string.most_played)) {
                    holder.mPlays.isVisible = true
                    val plays = current.plays
                    holder.mPlays.text = if (plays == 1) {
                        activity.getString(R.string.one_play)
                    } else {
                        activity.getString(R.string.n_plays, plays)
                    }
                    val textColour = when (position) {
                        1 -> R.color.gold
                        2 -> R.color.silver
                        3 -> R.color.bronze
                        else -> android.R.color.white
                    }
                    val textColour60 = when (position) {
                        1 -> R.color.gold60
                        2 -> R.color.silver60
                        3 -> R.color.bronze60
                        else -> R.color.onSurface60
                    }

                    holder.mTitle.setTextColor(ContextCompat.getColor(activity, textColour))
                    holder.mSubtitle.setTextColor(ContextCompat.getColor(activity, textColour60))
                    holder.mPlays.setTextColor(ContextCompat.getColor(activity, textColour60))
                    holder.mMenu?.setColorFilter(ContextCompat.getColor(activity, textColour60))
                }
            }
        }
    }

    internal fun manageHandles(applyHandles: Boolean){
        this.showHandles = applyHandles
        notifyItemRangeChanged(1, songs.size)
    }
}
package com.codersguidebook.supernova.recyclerview.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.codersguidebook.supernova.AlbumOptions
import com.codersguidebook.supernova.MainActivity
import com.codersguidebook.supernova.R
import com.codersguidebook.supernova.ui.artist.ArtistFragmentDirections
import com.codersguidebook.supernova.utils.ImageHandlingHelper

class ArtistAdapter(private val activity: MainActivity): SongAdapter() {

    var plays = 0

    companion object {
        const val HEADER = 1
        const val ALL_SONGS = 2
        const val ALBUM = 3
    }

    inner class ViewHolderHeader(itemView: View) : RecyclerView.ViewHolder(itemView) {

        internal var mArtwork = itemView.findViewById<View>(R.id.largeSongArtwork) as ImageView
        internal var mArtworkGrid = itemView.findViewById(R.id.imageGrid) as GridLayout
        internal var mArtwork1 = itemView.findViewById<View>(R.id.artwork1) as ImageView
        internal var mArtwork2 = itemView.findViewById<View>(R.id.artwork2) as ImageView
        internal var mArtwork3 = itemView.findViewById<View>(R.id.artwork3) as ImageView
        internal var mArtwork4 = itemView.findViewById<View>(R.id.artwork4) as ImageView
        internal var mArtist = itemView.findViewById<View>(R.id.largeTitle) as TextView
        internal var mAlbumCount = itemView.findViewById<View>(R.id.largeSubtitle) as TextView
        internal var mArtistPlays = itemView.findViewById<View>(R.id.largeSubtitle2) as TextView
    }

    inner class ViewHolderAllSongs(itemView: View) : RecyclerView.ViewHolder(itemView) {
        init {
            itemView.isClickable = true
            itemView.setOnClickListener {
                if (songs.isEmpty()) return@setOnClickListener
                val action = ArtistFragmentDirections.actionSelectArtistSongs(songs[0].artist)
                it.findNavController().navigate(action)
            }
        }
    }

    inner class ViewHolderAlbum(itemView: View) : RecyclerView.ViewHolder(itemView) {

        internal var mArtwork = itemView.findViewById<View>(R.id.artwork) as ImageView
        internal var mTitle = itemView.findViewById<View>(R.id.title) as TextView
        internal var mYear = itemView.findViewById<View>(R.id.subtitle) as TextView
        private var mMenu = itemView.findViewById<ImageButton>(R.id.menu)

        init {
            itemView.rootView.isClickable = true
            itemView.rootView.setOnClickListener {
                val action = ArtistFragmentDirections.actionSelectAlbum(songs[layoutPosition - 2].albumId)
                it.findNavController().navigate(action)
            }

            itemView.rootView.setOnLongClickListener{
                activity.openDialog(AlbumOptions(songs[layoutPosition - 2].albumId))
                return@setOnLongClickListener true
            }

            mMenu.setOnClickListener {
                activity.openDialog(AlbumOptions(songs[layoutPosition - 2].albumId))
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (position) {
            0 -> HEADER
            1 -> ALL_SONGS
            else -> ALBUM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            HEADER -> ViewHolderHeader(
                LayoutInflater.from(parent.context).inflate(R.layout.large_preview, parent, false)
            )
            ALL_SONGS -> ViewHolderAllSongs(
                LayoutInflater.from(parent.context).inflate(R.layout.all_songs, parent, false)
            )
            else -> ViewHolderAlbum(
                LayoutInflater.from(parent.context).inflate(R.layout.item_with_artwork_preview, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder.itemViewType) {
            HEADER -> {
                holder as ViewHolderHeader

                holder.itemView.setBackgroundColor(ContextCompat.getColor(activity, R.color.preview_background))

                val albumIds = songs.map { it.albumId }

                when {
                    albumIds.size == 1 -> ImageHandlingHelper.loadImageByAlbumId(activity.application,
                        albumIds[0], holder.mArtwork)
                    albumIds.size > 1 -> {
                        holder.mArtwork.isGone = true
                        holder.mArtworkGrid.isVisible = true
                        val shuffledAlbumIds = albumIds.shuffled()
                        ImageHandlingHelper.loadImageByAlbumId(activity.application,
                            shuffledAlbumIds[0], holder.mArtwork1)
                        ImageHandlingHelper.loadImageByAlbumId(activity.application,
                            shuffledAlbumIds[1], holder.mArtwork2)
                        if (albumIds.size > 2) ImageHandlingHelper.loadImageByAlbumId(activity.application,
                            shuffledAlbumIds[2], holder.mArtwork3)
                        if (albumIds.size > 3) ImageHandlingHelper.loadImageByAlbumId(activity.application,
                            shuffledAlbumIds[3], holder.mArtwork4)
                    }
                }

                if (songs.isNotEmpty()){
                    holder.mArtist.text = songs[0].artist
                    holder.mAlbumCount.text = if (songs.size == 1) {
                        activity.getString(R.string.one_album)
                    } else {
                        activity.getString(R.string.n_albums, songs.size)
                    }

                    holder.mArtistPlays.text = if (plays == 1) {
                        activity.getString(R.string.played_one_time)
                    } else {
                        activity.getString(R.string.played_n_times, plays)
                    }
                }
            }
            ALBUM -> {
                holder as ViewHolderAlbum

                val current = songs[position -2]

                ImageHandlingHelper.loadImageByAlbumId(activity.application,
                    current.albumId, holder.mArtwork)

                holder.mTitle.text = current.albumName
                holder.mYear.text = current.year
            }
        }
    }

    override fun getItemCount() = songs.size + 2

    override fun getRecyclerViewIndex(index: Int): Int = index + 2
}
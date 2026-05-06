package com.codersguidebook.supernova.fragment.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.codersguidebook.supernova.MainActivity
import com.codersguidebook.supernova.R
import com.codersguidebook.supernova.dialogs.AlbumOptions
import com.codersguidebook.supernova.entities.Song
import com.codersguidebook.supernova.fragment.BaseDialogFragment
import com.codersguidebook.supernova.fragment.adapter.viewholder.ViewHolderHeaderArtworkGrid
import com.codersguidebook.supernova.fragment.adapter.viewholder.ViewHolderArtworkWithMenu
import com.codersguidebook.supernova.ui.artist.ArtistFragmentDirections
import com.codersguidebook.supernova.utils.ImageHandlingHelper

class ArtistAdapter(private val activity: MainActivity): SongAdapter(activity) {

    var plays = 0

    companion object {
        const val HEADER = 1
        const val ALL_SONGS = 2
        const val ALBUM = 3
    }

    inner class ViewHolderAllSongs(itemView: View) : RecyclerView.ViewHolder(itemView) {
        init {
            itemView.isClickable = true
            itemView.setOnClickListener {
                if (items.isEmpty()) return@setOnClickListener
                val action = ArtistFragmentDirections.actionSelectArtistSongs(
                    (items[0] as Song).artist ?: activity.getString(R.string.default_artist))
                it.findNavController().navigate(action)
            }
        }
    }

    inner class ViewHolderAlbum(itemView: View) : ViewHolderArtworkWithMenu(itemView) {

        override fun getActivity(): MainActivity {
            return activity
        }

        override fun rootViewAction() {
            val action = ArtistFragmentDirections.actionSelectAlbum((items[layoutPosition - 2] as Song).albumId)
            itemView.findNavController().navigate(action)
        }

        override fun getOptionsDialog(): BaseDialogFragment {
            return AlbumOptions((items[layoutPosition - 2] as Song).albumId)
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
            HEADER -> ViewHolderHeaderArtworkGrid(
                LayoutInflater.from(parent.context).inflate(R.layout.header, parent, false)
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
                holder as ViewHolderHeaderArtworkGrid

                holder.itemView.setBackgroundColor(ContextCompat.getColor(activity, R.color.preview_background))

                val albumIds = items.map { (it as Song).albumId }

                when {
                    albumIds.size == 1 -> {
                        holder.mArtwork.isVisible = true
                        holder.mArtworkGrid.isGone = true
                        ImageHandlingHelper.loadImageByAlbumId(activity.application,
                            albumIds[0], holder.mArtwork)
                    }
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

                if (items.isNotEmpty()){
                    holder.mTitle.text = (items[0] as Song).artist ?: activity.getString(R.string.default_artist)
                    holder.mSubtitle.text = if (items.size == 1) {
                        activity.getString(R.string.one_album)
                    } else {
                        activity.getString(R.string.n_albums, items.size)
                    }

                    holder.mSubtitle2.text = if (plays == 1) {
                        activity.getString(R.string.played_one_time)
                    } else {
                        activity.getString(R.string.played_n_times, plays)
                    }
                }
            }
            ALBUM -> {
                holder as ViewHolderAlbum

                val current = items[position -2] as Song

                ImageHandlingHelper.loadImageByAlbumId(activity.application,
                    current.albumId, holder.mArtwork)

                holder.mTitle.text = current.albumName ?: activity.getString(R.string.default_album)
                holder.mSubtitle.text = current.year
            }
        }
    }

    override fun getItemCount() = items.size + 2

    override fun getRecyclerViewIndex(index: Int): Int = index + 2
}
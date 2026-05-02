package com.codersguidebook.supernova.fragment.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.codersguidebook.supernova.MainActivity
import com.codersguidebook.supernova.R
import com.codersguidebook.supernova.dialogs.PlaylistOptions
import com.codersguidebook.supernova.entities.Playlist
import com.codersguidebook.supernova.fragment.BaseDialogFragment
import com.codersguidebook.supernova.fragment.adapter.viewholder.ViewHolderArtwork
import com.codersguidebook.supernova.ui.playlists.PlaylistsFragmentDirections
import com.codersguidebook.supernova.utils.ImageHandlingHelper
import com.codersguidebook.supernova.utils.PlaylistHelper

class PlaylistsAdapter(private val activity: MainActivity): Adapter() {

    inner class ViewHolderPlaylist(itemView: View) : ViewHolderArtwork(itemView) {

        override fun getActivity(): MainActivity {
            return activity
        }

        override fun rootViewAction() {
            val action = PlaylistsFragmentDirections.actionSelectPlaylist((items[layoutPosition] as Playlist).name)
            itemView.findNavController().navigate(action)
        }

        override fun getOptionsDialog(): BaseDialogFragment {
            return PlaylistOptions((items[layoutPosition] as Playlist))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ViewHolderPlaylist(LayoutInflater.from(parent.context).inflate(R.layout.small_preview, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        holder as ViewHolderPlaylist
        val playlist = items[position] as Playlist

        holder.mTitle.text = playlist.name

        val playlistSongIds = PlaylistHelper.extractSongIds(playlist.songs)
        if (!ImageHandlingHelper.loadImageByPlaylist(activity.application, playlist, holder.mArtwork)) {
            activity.loadRandomArtworkBySongIds(playlistSongIds, holder.mArtwork)
        }

        val songCountInt = playlistSongIds.size
        holder.mSubtitle.text = if (songCountInt == 1) activity.getString(R.string.displayed_song)
        else activity.getString(R.string.displayed_songs, songCountInt)
    }

    override fun itemsEqual(item1: Any, item2: Any): Boolean {
        return (item1 as Playlist).playlistId == (item2 as Playlist).playlistId
    }

    override fun itemShouldBeUpdated(item: Any, index: Int): Boolean {
        return (item as Playlist) != (items[index] as Playlist)
    }
}
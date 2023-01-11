package com.codersguidebook.supernova.fragment.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.codersguidebook.supernova.MainActivity
import com.codersguidebook.supernova.R
import com.codersguidebook.supernova.dialogs.PlaylistOptions
import com.codersguidebook.supernova.entities.Playlist
import com.codersguidebook.supernova.ui.playlists.PlaylistsFragmentDirections
import com.codersguidebook.supernova.utils.ImageHandlingHelper
import com.codersguidebook.supernova.utils.PlaylistHelper

class PlaylistsAdapter(private val activity: MainActivity): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var playlists = mutableListOf<Playlist>()

    inner class ViewHolderPlaylist(itemView: View) : RecyclerView.ViewHolder(itemView) {

        internal var mArtwork = itemView.findViewById<View>(R.id.smallSongArtwork) as ImageView
        internal var mPlaylistName = itemView.findViewById<View>(R.id.smallSongTitle) as TextView
        internal var mSongCount = itemView.findViewById<View>(R.id.smallSongArtistOrCount) as TextView

        init {
            itemView.isClickable = true
            itemView.setOnClickListener {
                val action = PlaylistsFragmentDirections.actionSelectPlaylist(playlists[layoutPosition].name)
                it.findNavController().navigate(action)
            }
            itemView.setOnLongClickListener{
                activity.openDialog(PlaylistOptions(playlists[layoutPosition]))
                return@setOnLongClickListener true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ViewHolderPlaylist(LayoutInflater.from(parent.context).inflate(R.layout.small_recycler_grid_preview, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        holder as ViewHolderPlaylist
        val playlist = playlists[position]

        holder.mPlaylistName.text = playlist.name

        val playlistSongIds = PlaylistHelper.extractSongIds(playlist.songs)
        if (!ImageHandlingHelper.loadImageByPlaylist(activity.application, playlist, holder.mArtwork)) {
            activity.loadRandomArtworkBySongIds(playlistSongIds, holder.mArtwork)
        }

        val songCountInt = playlistSongIds.size
        holder.mSongCount.text = if (songCountInt == 1) activity.getString(R.string.displayed_song)
        else activity.getString(R.string.displayed_songs, songCountInt)
    }

    /**
     * Handle updates to the content of the RecyclerView. The below method will determine what
     * changes are required when an element/elements is/are changed, inserted, or deleted.
     * This enhanced process loop iteration method assumes each playlist can only appear once.
     *
     * @param newPlaylists The new list of Playlist objects that should be displayed.
     */
    fun processNewPlaylists(newPlaylists: List<Playlist>) {
        for ((index, playlist) in newPlaylists.withIndex()) {
            when {
                index >= playlists.size -> {
                    playlists.add(playlist)
                    notifyItemInserted(index)
                }
                playlist.playlistId != playlists[index].playlistId -> {
                    var numberOfItemsRemoved = 0
                    do {
                        playlists.removeAt(index)
                        ++numberOfItemsRemoved
                    } while (index < playlists.size &&
                        playlist.playlistId != playlists[index].playlistId)

                    when {
                        numberOfItemsRemoved == 1 -> notifyItemRemoved(index)
                        numberOfItemsRemoved > 1 -> notifyItemRangeRemoved(index, numberOfItemsRemoved)
                    }
                }
                playlist != newPlaylists[index] -> {
                    playlists[index] = playlist
                    notifyItemChanged(index)
                }
            }
        }

        if (playlists.size > newPlaylists.size) {
            val numberItemsToRemove = playlists.size - newPlaylists.size
            repeat(numberItemsToRemove) { playlists.removeLast() }
            notifyItemRangeRemoved(newPlaylists.size, numberItemsToRemove)
        }
    }

    override fun getItemCount() = playlists.size
}
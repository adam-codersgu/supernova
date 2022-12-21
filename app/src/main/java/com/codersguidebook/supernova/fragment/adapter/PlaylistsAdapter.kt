package com.codersguidebook.supernova.fragment.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.codersguidebook.supernova.MainActivity
import com.codersguidebook.supernova.PlaylistOptions
import com.codersguidebook.supernova.R
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
        val current = playlists[position]

        holder.mPlaylistName.text = current.name

        val playlistSongIDs = PlaylistHelper.extractSongIds(current.songs)
        // FIXME: Maybe find another way to handle artwork for playlists with no songs
        if (!ImageHandlingHelper.loadImageByPlaylist(activity.application,
                current, holder.mArtwork) && playlistSongIDs.isNotEmpty()) {
            ImageHandlingHelper.loadImageByAlbumId(activity.application,
                activity.findAlbumIdBySongId(playlistSongIDs[0]), holder.mArtwork)
        }

        val songCountInt = playlistSongIDs.size
        holder.mSongCount.text = if (songCountInt == 1) activity.getString(R.string.displayed_song)
        else activity.getString(R.string.displayed_songs, songCountInt)
    }

    fun processLoopIteration(index: Int, playlist: Playlist) {
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

                processLoopIteration(index, playlist)
            }
            playlist.name != playlists[index].name ||
                    playlist.songs != playlists[index].songs -> {
                playlists[index] = playlist
                notifyItemChanged(index)
            }
        }
    }

    override fun getItemCount() = playlists.size
}
package com.codersguidebook.supernova.ui.playlists

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.codersguidebook.supernova.*
import com.codersguidebook.supernova.entities.Playlist
import java.util.*

class PlaylistsAdapter(private val mainActivity: MainActivity):
    RecyclerView.Adapter<PlaylistsAdapter.PlaylistsViewHolder>() {
    var playlists = mutableListOf<Playlist>()

    inner class PlaylistsViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView),
        View.OnClickListener {

        internal var mArtwork = itemView.findViewById<View>(R.id.smallSongArtwork) as ImageView
        internal var mPlaylistName = itemView.findViewById<View>(R.id.smallSongTitle) as TextView
        internal var mPlaylistSongCount = itemView.findViewById<View>(R.id.smallSongArtistOrCount) as TextView

        init {
            itemView.isClickable = true
            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener{
                mainActivity.openDialog(PlaylistOptions(playlists[layoutPosition]))
                return@setOnLongClickListener true
            }
        }

        override fun onClick(view: View) {
            val action = PlaylistsFragmentDirections.actionSelectPlaylist(playlists[layoutPosition].name)
            view.findNavController().navigate(action)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistsViewHolder {
        return PlaylistsViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.small_recycler_grid_preview, parent, false))
    }

    override fun onBindViewHolder(holder: PlaylistsViewHolder, position: Int) {
        val current = playlists[position]

        holder.mPlaylistName.text = current.name

        val playlistSongIDs = mainActivity.extractPlaylistSongIds(current.songs)
        // FIXME: Maybe find another way to handle artwork for playlists with no songs
        if (!mainActivity.insertPlaylistArtwork(current, holder.mArtwork) && playlistSongIDs.isNotEmpty()) {
            mainActivity.insertArtwork(mainActivity.findFirstSongArtwork(playlistSongIDs[0]), holder.mArtwork)
        }

        // determine how to present songCount
        val songCountInt = playlistSongIDs.size
        val songCount = if (songCountInt == 1) "$songCountInt song"
        else "$songCountInt songs"

        holder.mPlaylistSongCount.text = songCount
    }

    internal fun updatePlaylists(playlistList: List<Playlist>) {
        when {
            playlistList.size > playlists.size -> {
                val difference = playlistList.filterNot {
                    playlists.contains(it)
                }
                for (p in difference) {
                    playlists.add(p)
                    playlists = playlists.sortedBy { song ->
                        song.name.toUpperCase(Locale.ROOT)
                    }.toMutableList()
                    val index = playlists.indexOfFirst {
                        it.playlistId== p.playlistId
                    }
                    if (index != -1) {
                        notifyItemInserted(index)
                        notifyItemChanged(index)
                    }
                }
            }
            playlistList.size < playlists.size -> {
                val difference = playlists.filterNot {
                    playlistList.contains(it)
                }
                for (s in difference) {
                    val index = playlists.indexOfFirst {
                        it.playlistId == s.playlistId
                    }
                    if (index != -1) {
                        playlists.removeAt(index)
                        notifyItemRemoved(index)
                        notifyItemChanged(index)
                    }
                }
            }
        }
    }

    override fun getItemCount() = playlists.size
}
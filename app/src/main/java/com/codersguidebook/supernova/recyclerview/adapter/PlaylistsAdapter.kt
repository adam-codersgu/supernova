package com.codersguidebook.supernova.recyclerview.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.codersguidebook.supernova.*
import com.codersguidebook.supernova.entities.Artist
import com.codersguidebook.supernova.entities.Playlist
import com.codersguidebook.supernova.ui.playlists.PlaylistsFragmentDirections
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView
import java.util.*

class PlaylistsAdapter(private val activity: MainActivity): RecyclerView.Adapter<RecyclerView.ViewHolder>(),
    FastScrollRecyclerView.SectionedAdapter {
    var playlists = mutableListOf<Playlist>()

    inner class ViewHolderPlaylist(itemView: View) : RecyclerView.ViewHolder(itemView) {

        internal var mArtwork = itemView.findViewById<View>(R.id.smallSongArtwork) as ImageView
        internal var mPlaylistName = itemView.findViewById<View>(R.id.smallSongTitle) as TextView
        internal var mPlaylistSongCount = itemView.findViewById<View>(R.id.smallSongArtistOrCount) as TextView

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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderPlaylist {
        return ViewHolderPlaylist(LayoutInflater.from(parent.context).inflate(R.layout.small_recycler_grid_preview, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolderPlaylist, position: Int) {
        val current = playlists[position]

        holder.mPlaylistName.text = current.name

        val playlistSongIDs = activity.extractPlaylistSongIds(current.songs)
        // FIXME: Maybe find another way to handle artwork for playlists with no songs
        if (!activity.insertPlaylistArtwork(current, holder.mArtwork) && playlistSongIDs.isNotEmpty()) {
            activity.insertArtwork(activity.findFirstSongArtwork(playlistSongIDs[0]), holder.mArtwork)
        }

        // determine how to present songCount
        val songCountInt = playlistSongIDs.size
        val songCount = if (songCountInt == 1) "$songCountInt song"
        else "$songCountInt songs"

        holder.mPlaylistSongCount.text = songCount
    }

    fun processLoopIteration(index: Int, playlist: Playlist) {
        when {
            index >= artists.size -> {
                artists.add(artist)
                notifyItemInserted(index)
            }
            artist.artistName != artists[index].artistName -> {
                var numberOfItemsRemoved = 0
                do {
                    artists.removeAt(index)
                    ++numberOfItemsRemoved
                } while (index < artists.size &&
                    artist.artistName != artists[index].artistName)

                when {
                    numberOfItemsRemoved == 1 -> notifyItemRemoved(index)
                    numberOfItemsRemoved > 1 -> notifyItemRangeRemoved(index, numberOfItemsRemoved)
                }

                processLoopIteration(index, artist)
            }
            artist.songCount != artists[index].songCount -> {
                artists[index] = artist
                notifyItemChanged(index)
            }
        }
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
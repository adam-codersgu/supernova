package com.codersguidebook.supernova.fragment.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.codersguidebook.supernova.MainActivity
import com.codersguidebook.supernova.R
import com.codersguidebook.supernova.dialogs.AlbumOptions
import com.codersguidebook.supernova.dialogs.ArtistOptions
import com.codersguidebook.supernova.dialogs.PlaylistOptions
import com.codersguidebook.supernova.entities.Artist
import com.codersguidebook.supernova.entities.Playlist
import com.codersguidebook.supernova.entities.Song
import com.codersguidebook.supernova.params.SearchTypeConstants.Companion.ALBUM
import com.codersguidebook.supernova.params.SearchTypeConstants.Companion.ARTIST
import com.codersguidebook.supernova.params.SearchTypeConstants.Companion.PLAYLIST
import com.codersguidebook.supernova.params.SearchTypeConstants.Companion.TRACK
import com.codersguidebook.supernova.ui.albums.AlbumsFragmentDirections
import com.codersguidebook.supernova.ui.artists.ArtistsFragmentDirections
import com.codersguidebook.supernova.ui.search.SearchFragmentDirections
import com.codersguidebook.supernova.utils.ImageHandlingHelper
import com.codersguidebook.supernova.utils.PlaylistHelper

class SearchAdapter(private val activity: MainActivity): SongAdapter(activity) {

    var itemType = TRACK

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ViewHolderSong(
            LayoutInflater.from(parent.context).inflate(R.layout.item_with_artwork_preview, parent, false)
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        holder as ViewHolderSong

        when (itemType) {
            TRACK, ALBUM -> {
                val current = items[position] as Song

                holder.mArtwork.isVisible = true
                ImageHandlingHelper.loadImageByAlbumId(activity.application, current.albumId, holder.mArtwork)
                holder.mSubtitle.text = current.artist ?: activity.getString(R.string.default_artist)

                if (itemType == TRACK) {
                    holder.mTitle.text = current.title ?: activity.getString(R.string.default_title)
                } else {
                    holder.mTitle.text = current.albumName ?: activity.getString(R.string.default_album)
                    holder.mMenu.setOnClickListener {
                        activity.openDialog(AlbumOptions(current.albumId))
                    }
                    holder.itemView.setOnClickListener {
                        val action = AlbumsFragmentDirections.actionSelectAlbum(current.albumId)
                        it.findNavController().navigate(action)
                    }

                    holder.itemView.setOnLongClickListener{
                        activity.openDialog(AlbumOptions(current.albumId))
                        return@setOnLongClickListener true
                    }
                }
            }

            ARTIST -> {
                val current = items[position] as Artist

                holder.mArtwork.isGone = true
                holder.mTitle.text = current.artistName ?: activity.getString(R.string.default_artist)

                val songCountInt = current.songCount
                holder.mSubtitle.text = if (songCountInt == 1) {
                    activity.getString(R.string.displayed_song)
                } else {
                    activity.getString(R.string.displayed_songs, songCountInt)
                }

                holder.mMenu.setOnClickListener {
                    activity.openDialog(ArtistOptions(current.artistName ?: ""))
                }

                holder.itemView.setOnClickListener {
                    val action = ArtistsFragmentDirections.actionSelectArtist(current.artistName ?: "")
                    it.findNavController().navigate(action)
                }

                holder.itemView.setOnLongClickListener{
                    activity.openDialog(ArtistOptions(current.artistName ?: ""))
                    return@setOnLongClickListener true
                }
            }

            PLAYLIST -> {
                val current = items[position] as Playlist

                holder.mArtwork.isVisible = true

                val playlistSongIds = PlaylistHelper.extractSongIds(current.songs)
                if (!ImageHandlingHelper.loadImageByPlaylist(activity.application, current, holder.mArtwork)) {
                    activity.loadRandomArtworkBySongIds(playlistSongIds, holder.mArtwork)
                }

                holder.mTitle.text = current.name

                val songCountInt = playlistSongIds.size
                holder.mSubtitle.text = if (songCountInt == 1) {
                    activity.getString(R.string.displayed_song)
                } else {
                    activity.getString(R.string.displayed_songs, songCountInt)
                }

                holder.mMenu.setOnClickListener {
                    activity.openDialog(PlaylistOptions(current))
                }

                holder.itemView.setOnClickListener {
                    val action = SearchFragmentDirections.actionSelectPlaylist(current.name)
                    it.findNavController().navigate(action)
                }

                holder.itemView.setOnLongClickListener{
                    activity.openDialog(PlaylistOptions(current))
                    return@setOnLongClickListener true
                }
            }
        }
    }

    /** Clear the contents of the RecyclerView */
    fun clearRecyclerView() {
        val itemCount = itemCount
        items.clear()
        notifyItemRangeRemoved(0, itemCount)
    }

    override fun itemsEqual(item1: Any, item2: Any): Boolean {
        return when (itemType) {
            TRACK, ALBUM -> (item1 as Song).songId == (item2 as Song).songId
            ARTIST -> (item1 as Playlist).playlistId == (item2 as Playlist).playlistId
            else -> (item1 as Playlist).playlistId == (item2 as Playlist).playlistId
        }
    }
}
package com.codersguidebook.supernova.fragment.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.codersguidebook.supernova.*
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

    val albums = mutableListOf<Song>()
    val artists = mutableListOf<Artist>()
    val playlists = mutableListOf<Playlist>()
    var itemType = TRACK

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ViewHolderSong(
            LayoutInflater.from(parent.context).inflate(R.layout.item_with_artwork_preview, parent, false)
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        holder as ViewHolderSong

        when (itemType) {
            TRACK -> {
                val current = songs[position]

                holder.mArtwork?.isVisible = true
                ImageHandlingHelper.loadImageByAlbumId(activity.application, current.albumId, holder.mArtwork!!)
                holder.mTitle.text = current.title
                holder.mSubtitle.text = current.artist
            }

            ALBUM -> {
                val current = albums[position]

                holder.mArtwork?.isVisible = true
                ImageHandlingHelper.loadImageByAlbumId(activity.application, current.albumId, holder.mArtwork!!)
                holder.mTitle.text = current.albumName
                holder.mSubtitle.text = current.artist
                holder.mMenu?.setOnClickListener {
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

            ARTIST -> {
                val current = artists[position]

                holder.mArtwork?.isGone = true
                holder.mTitle.text = current.artistName

                val songCountInt = current.songCount
                holder.mSubtitle.text = if (songCountInt == 1) {
                    activity.getString(R.string.displayed_song)
                } else {
                    activity.getString(R.string.displayed_songs, songCountInt)
                }

                holder.mMenu?.setOnClickListener {
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
                val current = playlists[position]

                holder.mArtwork?.isVisible = true

                val playlistSongIds = PlaylistHelper.extractSongIds(current.songs)
                if (!ImageHandlingHelper.loadImageByPlaylist(activity.application, current, holder.mArtwork!!)) {
                    activity.loadRandomArtworkBySongIds(playlistSongIds, holder.mArtwork!!)
                }

                holder.mTitle.text = current.name

                val songCountInt = playlistSongIds.size
                holder.mSubtitle.text = if (songCountInt == 1) {
                    activity.getString(R.string.displayed_song)
                } else {
                    activity.getString(R.string.displayed_songs, songCountInt)
                }

                holder.mMenu?.setOnClickListener {
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

    override fun getItemCount() = when (itemType) {
        TRACK -> songs.size
        ALBUM -> albums.size
        ARTIST -> artists.size
        PLAYLIST -> playlists.size
        else -> 0
    }

    /**
     * Handle updates to the content of the RecyclerView. The below method will determine what
     * changes are required when an element/elements is/are changed, inserted, or deleted.
     *
     * @param index - The index of the current iteration through the up-to-date content list.
     * @param song - The Song object that should be displayed at the index.
     */
    fun processLoopIterationSong(index: Int, song: Song) {
        when {
            index >= songs.size -> {
                songs.add(song)
                notifyItemInserted(index)
            }
            song.songId != songs[index].songId -> {
                var numberOfItemsRemoved = 0
                do {
                    songs.removeAt(index)
                    ++numberOfItemsRemoved
                } while (index < songs.size &&
                    song.songId != songs[index].songId)

                when {
                    numberOfItemsRemoved == 1 -> notifyItemRemoved(index)
                    numberOfItemsRemoved > 1 -> notifyItemRangeRemoved(index, numberOfItemsRemoved)
                }

                processLoopIterationSong(index, song)
            }
            song != songs[index] -> {
                songs[index] = song
                notifyItemChanged(index)
            }
        }
    }

    /**
     * Handle updates to the content of the RecyclerView. The below method will determine what
     * changes are required when an element/elements is/are changed, inserted, or deleted.
     *
     * @param index - The index of the current iteration through the up-to-date content list.
     * @param album - A Song object associated with the album that should be displayed at the index.
     */
    fun processLoopIterationAlbum(index: Int, album: Song) {
        when {
            index >= albums.size -> {
                albums.add(album)
                notifyItemInserted(index)
            }
            album.albumId != albums[index].albumId -> {
                var numberOfItemsRemoved = 0
                do {
                    albums.removeAt(index)
                    ++numberOfItemsRemoved
                } while (index < albums.size &&
                    album.albumId != albums[index].albumId)

                when {
                    numberOfItemsRemoved == 1 -> notifyItemRemoved(index)
                    numberOfItemsRemoved > 1 -> notifyItemRangeRemoved(index, numberOfItemsRemoved)
                }

                processLoopIterationAlbum(index, album)
            }
            album != albums[index] -> {
                albums[index] = album
                notifyItemChanged(index)
            }
        }
    }

    /**
     * Handle updates to the content of the RecyclerView. The below method will determine what
     * changes are required when an element/elements is/are changed, inserted, or deleted.
     *
     * @param index - The index of the current iteration through the up-to-date content list.
     * @param artist - The Artist object that should be displayed at the index.
     */
    fun processLoopIterationArtist(index: Int, artist: Artist) {
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

                processLoopIterationArtist(index, artist)
            }
            artist != artists[index] -> {
                artists[index] = artist
                notifyItemChanged(index)
            }
        }
    }

    /**
     * Handle updates to the content of the RecyclerView. The below method will determine what
     * changes are required when an element/elements is/are changed, inserted, or deleted.
     *
     * @param index - The index of the current iteration through the up-to-date content list.
     * @param playlist - The Playlist object that should be displayed at the index.
     */
    fun processLoopIterationPlaylist(index: Int, playlist: Playlist) {
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

                processLoopIterationPlaylist(index, playlist)
            }
            playlist != playlists[index] -> {
                playlists[index] = playlist
                notifyItemChanged(index)
            }
        }
    }
}
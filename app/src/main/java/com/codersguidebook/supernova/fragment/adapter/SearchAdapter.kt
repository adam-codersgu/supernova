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

    /** Clear the contents of the RecyclerView */
    fun clearRecyclerView() {
        val itemCount = itemCount
        songs.clear()
        artists.clear()
        albums.clear()
        playlists.clear()
        notifyItemRangeRemoved(0, itemCount)
    }

    /**
     * Handle updates to the content of the RecyclerView. The below method will determine what
     * changes are required when an element/elements is/are changed, inserted, or deleted.
     *
     * @param songsByAlbum The list of Song objects that album details should be extracted from.
     */
    fun processNewAlbums(songsByAlbum: List<Song>) {
        for ((index, song) in songsByAlbum.withIndex()) {
            when {
                index >= albums.size -> {
                    albums.add(song)
                    notifyItemInserted(index)
                }
                song.songId != albums[index].songId -> {
                    // Check if the album is a new entry to the list
                    val songIsNewEntry = albums.find { it.songId == song.songId } == null
                    if (songIsNewEntry) {
                        albums.add(index, song)
                        notifyItemInserted(index)
                        continue
                    }

                    // Check if album(s) has/have been removed from the list
                    val songIsRemoved = songsByAlbum.find { it.songId == albums[index].songId } == null
                    if (songIsRemoved) {
                        var numberOfItemsRemoved = 0
                        do {
                            albums.removeAt(index)
                            ++numberOfItemsRemoved
                        } while (index < albums.size &&
                            songsByAlbum.find { it.songId == albums[index].songId } == null)

                        when {
                            numberOfItemsRemoved == 1 -> notifyItemRemoved(index)
                            numberOfItemsRemoved > 1 -> notifyItemRangeRemoved(index,
                                numberOfItemsRemoved)
                        }

                        // Check if removing the album(s) has fixed the list
                        if (song.songId == albums[index].songId) continue
                    }

                    // Check if the album has been moved earlier in the list
                    val oldIndex = albums.indexOfFirst { it.songId == song.songId }
                    if (oldIndex != -1 && oldIndex > index) {
                        albums.removeAt(oldIndex)
                        albums.add(index, song)
                        notifyItemMoved(oldIndex, index)
                        continue
                    }

                    // Check if the album(s) has been moved later in the list
                    var newIndex = songsByAlbum.indexOfFirst { it.songId == albums[index].songId }
                    if (newIndex != -1) {
                        do {
                            albums.removeAt(index)

                            if (newIndex <= albums.size) {
                                albums.add(newIndex, song)
                                notifyItemMoved(index, newIndex)
                            } else {
                                notifyItemRemoved(index)
                            }

                            // See if further albums need to be moved
                            newIndex = songsByAlbum.indexOfFirst { it.songId == albums[index].songId }
                        } while (index < albums.size &&
                            song.songId != albums[index].songId &&
                            newIndex != -1)

                        // Check if moving the album(s) has fixed the list
                        if (song.songId == albums[index].songId) continue
                        else {
                            albums.add(index, song)
                            notifyItemInserted(index)
                        }
                    }
                }
                song != albums[index] -> {
                    albums[index] = song
                    notifyItemChanged(index)
                }
            }
        }

        if (albums.size > songsByAlbum.size) {
            val numberItemsToRemove = albums.size - songsByAlbum.size
            repeat(numberItemsToRemove) { albums.removeLast() }
            notifyItemRangeRemoved(songsByAlbum.size, numberItemsToRemove)
        }
    }

    /**
     * Handle updates to the content of the RecyclerView. The below method will determine what
     * changes are required when an element/elements is/are changed, inserted, or deleted.
     * This enhanced process loop iteration method assumes each artist can only appear once.
     *
     * @param newArtists The new list of Artist objects that should be displayed.
     */
    fun processNewArtists(newArtists: List<Artist>) {
        for ((index, artist) in newArtists.withIndex()) {
            when {
                index >= artists.size -> {
                    artists.add(artist)
                    notifyItemInserted(index)
                }
                artist.artistName != artists[index].artistName -> {
                    // Check if the artist is a new entry to the list
                    val artistIsNewEntry = artists.find { it.artistName == artist.artistName } == null
                    if (artistIsNewEntry) {
                        artists.add(index, artist)
                        notifyItemInserted(index)
                        continue
                    }

                    // Check if artist(s) has/have been removed from the list
                    val artistIsRemoved = newArtists.find { it.artistName == artists[index].artistName } == null
                    if (artistIsRemoved) {
                        var numberOfItemsRemoved = 0
                        do {
                            artists.removeAt(index)
                            ++numberOfItemsRemoved
                        } while (index < artists.size &&
                            newArtists.find { it.artistName == artists[index].artistName } == null)

                        when {
                            numberOfItemsRemoved == 1 -> notifyItemRemoved(index)
                            numberOfItemsRemoved > 1 -> notifyItemRangeRemoved(index,
                                numberOfItemsRemoved)
                        }

                        // Check if removing the artist(s) has fixed the list
                        if (artist.artistName == artists[index].artistName) continue
                    }

                    // Check if the artist has been moved earlier in the list
                    val oldIndex = artists.indexOfFirst { it.artistName == artist.artistName }
                    if (oldIndex != -1 && oldIndex > index) {
                        artists.removeAt(oldIndex)
                        artists.add(index, artist)
                        notifyItemMoved(oldIndex, index)
                        continue
                    }

                    // Check if the artist(s) has been moved later in the list
                    var newIndex = newArtists.indexOfFirst { it.artistName == artists[index].artistName }
                    if (newIndex != -1) {
                        do {
                            artists.removeAt(index)

                            if (newIndex <= artists.size) {
                                artists.add(newIndex, artist)
                                notifyItemMoved(index, newIndex)
                            } else {
                                notifyItemRemoved(index)
                            }

                            // See if further artists need to be moved
                            newIndex = newArtists.indexOfFirst { it.artistName == artists[index].artistName }
                        } while (index < artists.size &&
                            artist.artistName != artists[index].artistName &&
                            newIndex != -1)

                        // Check if moving the artist(s) has fixed the list
                        if (artist.artistName == artists[index].artistName) continue
                        else {
                            artists.add(index, artist)
                            notifyItemInserted(index)
                        }
                    }
                }
                artist.songCount != artists[index].songCount -> {
                    artists[index] = artist
                    notifyItemChanged(index)
                }
            }
        }

        if (artists.size > newArtists.size) {
            val numberItemsToRemove = artists.size - newArtists.size
            repeat(numberItemsToRemove) { artists.removeLast() }
            notifyItemRangeRemoved(newArtists.size, numberItemsToRemove)
        }
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
                    // Check if the playlist is a new entry to the list
                    val playlistIsNewEntry = playlists.find { it.playlistId == playlist.playlistId } == null
                    if (playlistIsNewEntry) {
                        playlists.add(index, playlist)
                        notifyItemInserted(index)
                        continue
                    }

                    // Check if playlist(s) has/have been removed from the list
                    val playlistIsRemoved = newPlaylists.find { it.playlistId == playlists[index].playlistId } == null
                    if (playlistIsRemoved) {
                        var numberOfItemsRemoved = 0
                        do {
                            playlists.removeAt(index)
                            ++numberOfItemsRemoved
                        } while (index < playlists.size &&
                            newPlaylists.find { it.playlistId == playlists[index].playlistId } == null)

                        when {
                            numberOfItemsRemoved == 1 -> notifyItemRemoved(index)
                            numberOfItemsRemoved > 1 -> notifyItemRangeRemoved(index,
                                numberOfItemsRemoved)
                        }

                        // Check if removing the playlist(s) has fixed the list
                        if (playlist.playlistId == playlists[index].playlistId) continue
                    }

                    // Check if the playlist has been moved earlier in the list
                    val oldIndex = playlists.indexOfFirst { it.playlistId == playlist.playlistId }
                    if (oldIndex != -1 && oldIndex > index) {
                        playlists.removeAt(oldIndex)
                        playlists.add(index, playlist)
                        notifyItemMoved(oldIndex, index)
                        continue
                    }

                    // Check if the playlist(s) has been moved later in the list
                    var newIndex = newPlaylists.indexOfFirst { it.playlistId == playlists[index].playlistId }
                    if (newIndex != -1) {
                        do {
                            playlists.removeAt(index)

                            if (newIndex <= playlists.size) {
                                playlists.add(newIndex, playlist)
                                notifyItemMoved(index, newIndex)
                            } else {
                                notifyItemRemoved(index)
                            }

                            // See if further playlists need to be moved
                            newIndex = newPlaylists.indexOfFirst { it.playlistId == playlists[index].playlistId }
                        } while (index < playlists.size &&
                            playlist.playlistId != playlists[index].playlistId &&
                            newIndex != -1)

                        // Check if moving the playlist(s) has fixed the list
                        if (playlist.playlistId == playlists[index].playlistId) continue
                        else {
                            playlists.add(index, playlist)
                            notifyItemInserted(index)
                        }
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
}
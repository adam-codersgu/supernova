package com.codersguidebook.supernova.ui.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.codersguidebook.supernova.*
import com.codersguidebook.supernova.entities.Artist
import com.codersguidebook.supernova.entities.Playlist
import com.codersguidebook.supernova.entities.Song
import com.codersguidebook.supernova.ui.albums.AlbumsFragmentDirections
import com.codersguidebook.supernova.ui.artists.ArtistsFragmentDirections

class SearchAdapter(private val mainActivity: MainActivity):
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var songs = mutableListOf<Song>()
    var albums = mutableListOf<Song>()
    var artists = mutableListOf<Artist>()
    var playlists = mutableListOf<Playlist>()
    var itemType = TRACK

    companion object {
        const val TRACK = 0
        const val ALBUM = 1
        const val ARTIST = 2
        const val PLAYLIST = 3
    }

    class ViewHolderItem(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        internal var mArtwork = itemView.findViewById<View>(R.id.artwork) as ImageView
        internal var mTitle = itemView.findViewById<View>(R.id.title) as TextView
        internal var mSubtitle = itemView.findViewById<View>(R.id.subtitle) as TextView
        internal var mMenu = itemView.findViewById<ImageButton>(R.id.menu)

        init {
            itemView.isClickable = true
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ViewHolderItem(LayoutInflater.from(parent.context).inflate(R.layout.preview, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        holder as ViewHolderItem

        when (itemType) {
            TRACK -> {
                val current = songs[position]

                holder.mArtwork.isVisible = true
                mainActivity.insertArtwork(current.albumId, holder.mArtwork)
                holder.mTitle.text = current.title
                holder.mSubtitle.text = current.artist
                holder.mMenu.setOnClickListener {
                    mainActivity.openDialog(SongOptions(current))
                }

                holder.itemView.setOnClickListener {
                    mainActivity.playListOfSongs(listOf(current), 0, false)
                }

                holder.itemView.setOnLongClickListener {
                    mainActivity.openDialog(SongOptions(current))
                    return@setOnLongClickListener true
                }
            }

            ALBUM -> {
                val current = albums[position]

                holder.mArtwork.isVisible = true
                mainActivity.insertArtwork(current.albumId, holder.mArtwork)
                holder.mTitle.text = current.albumName
                holder.mSubtitle.text = current.artist
                holder.mMenu.setOnClickListener {
                    mainActivity.openAlbumDialog(current.albumId)
                }

                holder.itemView.setOnClickListener {
                    val action = AlbumsFragmentDirections.actionSelectAlbum(current.albumId)
                    it.findNavController().navigate(action)
                }

                holder.itemView.setOnLongClickListener{
                    mainActivity.openAlbumDialog(current.albumId)
                    return@setOnLongClickListener true
                }
            }

            ARTIST -> {
                val current = artists[position]

                holder.mArtwork.isGone = true
                holder.mTitle.text = current.artistName

                // determine how to present songCount
                val songCountInt = current.songCount
                holder.mSubtitle.text = if (songCountInt == 1) "$songCountInt song"
                else "$songCountInt songs"

                holder.mMenu.setOnClickListener {
                    mainActivity.openDialog(ArtistOptions(current.artistName ?: ""))
                }

                holder.itemView.setOnClickListener {
                    val action = ArtistsFragmentDirections.actionSelectArtist(current.artistName ?: "")
                    it.findNavController().navigate(action)
                }

                holder.itemView.setOnLongClickListener{
                    mainActivity.openDialog(ArtistOptions(current.artistName ?: ""))
                    return@setOnLongClickListener true
                }
            }

            PLAYLIST -> {
                val current = playlists[position]

                holder.mArtwork.isVisible = true
                val playlistSongIDs= mainActivity.extractPlaylistSongIDs(current.songs)
                if (playlistSongIDs.isNotEmpty()){
                    val firstSongArtwork = mainActivity.findFirstSongArtwork(playlistSongIDs[0])
                    mainActivity.insertArtwork(firstSongArtwork, holder.mArtwork)
                }

                holder.mTitle.text = current.name

                // determine how to present songCount
                val songCountInt = playlistSongIDs.size
                holder.mSubtitle.text = if (songCountInt == 1) "$songCountInt song"
                else "$songCountInt songs"

                holder.mMenu.setOnClickListener {
                    mainActivity.openDialog(PlaylistOptions(current))
                }

                holder.itemView.setOnClickListener {
                    val action = SearchFragmentDirections.actionSelectPlaylist(current.name)
                    it.findNavController().navigate(action)
                }

                holder.itemView.setOnLongClickListener{
                    mainActivity.openDialog(PlaylistOptions(current))
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
}
package com.codersguidebook.supernova.fragment.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.codersguidebook.supernova.MainActivity
import com.codersguidebook.supernova.R
import com.codersguidebook.supernova.dialogs.AlbumOptions
import com.codersguidebook.supernova.entities.Song
import com.codersguidebook.supernova.ui.albums.AlbumsFragmentDirections
import com.codersguidebook.supernova.utils.ImageHandlingHelper

class AlbumsAdapter(private val activity: MainActivity): SongAdapter(activity)/*,
    FastScrollRecyclerView.SectionedAdapter {

    override fun getSectionName(position: Int): String {
        return songs[position].albumName[0].uppercase()
    }  */ {

    inner class ViewHolderAlbum(itemView: View) : RecyclerView.ViewHolder(itemView) {

        internal var mArtwork = itemView.findViewById<View>(R.id.artwork) as ImageView
        internal var mTitle = itemView.findViewById<View>(R.id.title) as TextView
        internal var mArtist = itemView.findViewById<View>(R.id.subtitle) as TextView
        private var mMenu = itemView.findViewById<ImageButton>(R.id.menu)

        init {
            itemView.isClickable = true
            itemView.setOnClickListener {
                val action = AlbumsFragmentDirections.actionSelectAlbum(songs[layoutPosition].albumId)
                it.findNavController().navigate(action)
            }

            itemView.setOnLongClickListener{
                activity.openDialog(AlbumOptions(songs[layoutPosition].albumId))
                return@setOnLongClickListener true
            }

            mMenu.setOnClickListener {
                activity.openDialog(AlbumOptions(songs[layoutPosition].albumId))
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ViewHolderAlbum(
            LayoutInflater.from(parent.context).inflate(R.layout.item_with_artwork_preview, parent, false)
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        holder as ViewHolderAlbum
        val current = songs[position]

        ImageHandlingHelper.loadImageByAlbumId(activity.application, current.albumId, holder.mArtwork)

        holder.mTitle.text = current.albumName
        holder.mArtist.text = current.artist
    }

    /**
     * Extract the list of unique of albums from a list of songs and display the album
     * metadata in the RecyclerView
     *
     * @param songList The list of Song objects that album details should be extracted from.
     */
    fun processAlbumsBySongs(songList: List<Song>) {
        val songsByAlbum = songList.distinctBy { song ->
            song.albumId
        }.sortedBy { song ->
            song.albumName.uppercase()
        }.toMutableList()

        if (songs.isEmpty()) {
            songs.addAll(songsByAlbum)
            notifyItemRangeInserted(0, songsByAlbum.size)
        } else {
            processNewSongs(songsByAlbum)
        }
    }
}
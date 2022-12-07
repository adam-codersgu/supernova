package com.codersguidebook.supernova.recyclerview.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.codersguidebook.supernova.AlbumOptions
import com.codersguidebook.supernova.MainActivity
import com.codersguidebook.supernova.R
import com.codersguidebook.supernova.entities.Song
import com.codersguidebook.supernova.ui.albums.AlbumsFragmentDirections
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView

class AlbumsAdapter(private val activity: MainActivity): RecyclerView.Adapter<RecyclerView.ViewHolder>(),
    FastScrollRecyclerView.SectionedAdapter {

    val albums = mutableListOf<Song>()

    override fun getSectionName(position: Int): String {
        return albums[position].albumName[0].uppercase()
    }

    inner class ViewHolderAlbum(itemView: View) : RecyclerView.ViewHolder(itemView) {

        internal var mArtwork = itemView.findViewById<View>(R.id.artwork) as ImageView
        internal var mTitle = itemView.findViewById<View>(R.id.title) as TextView
        internal var mArtist = itemView.findViewById<View>(R.id.artist) as TextView
        private var mMenu = itemView.findViewById<ImageButton>(R.id.menu)

        init {
            itemView.rootView.isClickable = true
            itemView.rootView.setOnClickListener {
                val action = AlbumsFragmentDirections.actionSelectAlbum(albums[layoutPosition].albumId)
                it.findNavController().navigate(action)
            }

            itemView.rootView.setOnLongClickListener{
                activity.openDialog(AlbumOptions(albums[layoutPosition].albumId))
                return@setOnLongClickListener true
            }

            mMenu.setOnClickListener {
                activity.openDialog(AlbumOptions(albums[layoutPosition].albumId))
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ViewHolderAlbum(
            LayoutInflater.from(parent.context).inflate(R.layout.song_with_artwork_preview, parent, false)
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        holder as ViewHolderAlbum
        val current = albums[position]

        activity.insertArtwork(current.albumId, holder.mArtwork)

        holder.mTitle.text = current.albumName
        holder.mArtist.text = current.artist
    }

    override fun getItemCount() = albums.size
}
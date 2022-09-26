package com.codersguidebook.supernova.ui.albums

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
import com.codersguidebook.supernova.entities.Song
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView

class AlbumsAdapter(private val mainActivity: MainActivity):
    RecyclerView.Adapter<AlbumsAdapter.AlbumsViewHolder>(), FastScrollRecyclerView.SectionedAdapter {
    var albums = mutableListOf<Song>()

    override fun getSectionName(position: Int): String {
        return albums[position].albumName[0].toUpperCase().toString()
    }

    inner class AlbumsViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView),
        View.OnClickListener {

        internal var mArtwork = itemView.findViewById<View>(R.id.artwork) as ImageView
        internal var mTitle = itemView.findViewById<View>(R.id.title) as TextView
        internal var mArtist = itemView.findViewById<View>(R.id.subtitle) as TextView
        internal var mBtnMenu = itemView.findViewById<ImageButton>(R.id.menu)

        init {
            itemView.isClickable = true
            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener{
                mainActivity.openAlbumDialog(albums[layoutPosition].albumId)
                return@setOnLongClickListener true
            }
        }

        override fun onClick(view: View) {
            val action = AlbumsFragmentDirections.actionSelectAlbum(albums[layoutPosition].albumId)
            view.findNavController().navigate(action)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumsViewHolder {
        return AlbumsViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.preview, parent, false))
    }

    override fun onBindViewHolder(holder: AlbumsViewHolder, position: Int) {
        val current = albums[position]

        holder.mTitle.text = current.albumName
        holder.mArtist.text = current.artist

        mainActivity.insertArtwork(current.albumId, holder.mArtwork)

        holder.mBtnMenu.setOnClickListener {
            mainActivity.openAlbumDialog(current.albumId)
        }
    }

    override fun getItemCount() = albums.size
}
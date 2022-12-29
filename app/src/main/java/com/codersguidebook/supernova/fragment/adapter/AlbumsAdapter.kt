package com.codersguidebook.supernova.fragment.adapter

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
import com.codersguidebook.supernova.utils.ImageHandlingHelper
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView

class AlbumsAdapter(private val activity: MainActivity): Adapter(),
    FastScrollRecyclerView.SectionedAdapter {

    val songsByAlbum = mutableListOf<Song>()

    override fun getSectionName(position: Int): String {
        return songsByAlbum[position].albumName[0].uppercase()
    }

    inner class ViewHolderAlbum(itemView: View) : RecyclerView.ViewHolder(itemView) {

        internal var mArtwork = itemView.findViewById<View>(R.id.artwork) as ImageView
        internal var mTitle = itemView.findViewById<View>(R.id.title) as TextView
        internal var mArtist = itemView.findViewById<View>(R.id.subtitle) as TextView
        private var mMenu = itemView.findViewById<ImageButton>(R.id.menu)

        init {
            itemView.isClickable = true
            itemView.setOnClickListener {
                val action = AlbumsFragmentDirections.actionSelectAlbum(songsByAlbum[layoutPosition].albumId)
                it.findNavController().navigate(action)
            }

            itemView.setOnLongClickListener{
                activity.openDialog(AlbumOptions(songsByAlbum[layoutPosition].albumId))
                return@setOnLongClickListener true
            }

            mMenu.setOnClickListener {
                activity.openDialog(AlbumOptions(songsByAlbum[layoutPosition].albumId))
            }
        }
    }

    override fun processLoopIteration(index: Int, song: Song) {
        val recyclerViewIndex = getRecyclerViewIndex(index)
        when {
            index >= songsByAlbum.size -> {
                songsByAlbum.add(song)
                notifyItemInserted(recyclerViewIndex)
            }
            song.albumId != songsByAlbum[index].albumId -> {
                var numberOfItemsRemoved = 0
                do {
                    songsByAlbum.removeAt(index)
                    ++numberOfItemsRemoved
                } while (index < songsByAlbum.size &&
                    song.songId != songsByAlbum[index].songId)

                when {
                    numberOfItemsRemoved == 1 -> notifyItemRemoved(recyclerViewIndex)
                    numberOfItemsRemoved > 1 -> notifyItemRangeRemoved(recyclerViewIndex, numberOfItemsRemoved)
                }

                processLoopIteration(index, song)
            }
            song.albumName != songsByAlbum[index].albumName || song.artist != songsByAlbum[index].artist -> {
                songsByAlbum[index] = song
                notifyItemChanged(recyclerViewIndex)
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
        val current = songsByAlbum[position]

        ImageHandlingHelper.loadImageByAlbumId(activity.application, current.albumId, holder.mArtwork)

        holder.mTitle.text = current.albumName
        holder.mArtist.text = current.artist
    }

    override fun getItemCount() = songsByAlbum.size
}
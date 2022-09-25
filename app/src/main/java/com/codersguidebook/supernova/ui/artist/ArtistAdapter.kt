package com.codersguidebook.supernova.ui.artist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.codersguidebook.supernova.MainActivity
import com.codersguidebook.supernova.R
import com.codersguidebook.supernova.entities.Song
import com.codersguidebook.supernova.ui.albums.AlbumsFragmentDirections
import java.util.*

class ArtistAdapter(private val mainActivity: MainActivity,
private val fragment: ArtistFragment):
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var albums = mutableListOf<Song>()
    var artistSongs = emptyList<Song>()

    companion object {
        private const val HEADER = 1
        private const val ALL_SONGS = 2
        private const val ALBUM = 3
    }

    inner class HeaderViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        internal var mArtwork = itemView.findViewById<View>(R.id.largeSongArtwork) as ImageView
        internal var mArtworkGrid = itemView.findViewById(R.id.imageGrid) as GridLayout
        internal var mArtwork1 = itemView.findViewById<View>(R.id.artwork1) as ImageView
        internal var mArtwork2 = itemView.findViewById<View>(R.id.artwork2) as ImageView
        internal var mArtwork3 = itemView.findViewById<View>(R.id.artwork3) as ImageView
        internal var mArtwork4 = itemView.findViewById<View>(R.id.artwork4) as ImageView
        internal var mArtist = itemView.findViewById<View>(R.id.largeTitle) as TextView
        internal var mAlbumCount = itemView.findViewById<View>(R.id.largeSubtitle) as TextView
        internal var mArtistPlays = itemView.findViewById<View>(R.id.largeSubtitle2) as TextView
    }

    inner class AllSongsViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView),
        View.OnClickListener {

        init {
            itemView.isClickable = true
            itemView.setOnClickListener(this)
        }

        override fun onClick(view: View) {
            fragment.viewSongs()
        }
    }

    inner class AlbumsViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView),
        View.OnClickListener {

        internal var mArtwork = itemView.findViewById<View>(R.id.artwork) as ImageView
        internal var mAlbumName = itemView.findViewById<View>(R.id.title) as TextView

        internal var mYear =
            itemView.findViewById<View>(
                R.id.subtitle
            ) as TextView

        internal var mBtnAlbumMenu = itemView.findViewById<ImageButton>(R.id.menu)

        init {
            itemView.isClickable = true
            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener{
                mainActivity.openAlbumDialog(albums[adapterPosition -2].albumID)
                return@setOnLongClickListener true
            }
        }

        override fun onClick(view: View) {
            val action = AlbumsFragmentDirections.actionSelectAlbum(albums[adapterPosition -2].albumID)
            view.findNavController().navigate(action)
        }
    }

    override fun getItemViewType(position: Int): Int {
        super.getItemViewType(position)
        return when (position) {
            0 -> HEADER
            1 -> ALL_SONGS
            else -> ALBUM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            HEADER -> HeaderViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.large_preview, parent, false))
            ALL_SONGS -> AllSongsViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.all_songs, parent, false))
            else -> AlbumsViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.preview, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder.itemViewType) {
            HEADER -> {
                holder as HeaderViewHolder

                holder.itemView.setBackgroundColor(ContextCompat.getColor(mainActivity, R.color.preview_background))

                val uniqueArtworks = albums.distinctBy {
                    it.albumID
                }

                when {
                    uniqueArtworks.size > 1 -> {
                        holder.mArtwork.isGone = true
                        holder.mArtworkGrid.isVisible = true
                        val shuffledArtworks = uniqueArtworks.shuffled()
                        mainActivity.insertArtwork(shuffledArtworks[0].albumID, holder.mArtwork1)
                        mainActivity.insertArtwork(shuffledArtworks[1].albumID, holder.mArtwork2)
                        if (uniqueArtworks.size > 2) mainActivity.insertArtwork(shuffledArtworks[2].albumID, holder.mArtwork3)
                        if (uniqueArtworks.size > 3) mainActivity.insertArtwork(shuffledArtworks[3].albumID, holder.mArtwork4)
                    }
                    uniqueArtworks.size == 1 -> mainActivity.insertArtwork(albums[0].albumID, holder.mArtwork)
                }

                if (albums.isNotEmpty()){
                    holder.mArtist.text = albums[0].artist
                    holder.mAlbumCount.text = if (albums.size == 1) "1 album"
                    else albums.size.toString() + " albums"
                    var plays = 0
                    for (s in artistSongs)  plays += s.plays
                    holder.mArtistPlays.text = if (plays == 1) "Played 1 time"
                    else "Played $plays times"
                }
            }
            ALBUM -> {
                holder as AlbumsViewHolder

                val current = albums[position -2]

                mainActivity.insertArtwork(current.albumID, holder.mArtwork)

                holder.mAlbumName.text = current.album
                holder.mYear.text = current.year
                holder.mBtnAlbumMenu.setOnClickListener {
                    mainActivity.openAlbumDialog(current.albumID)
                }
            }
        }
    }

    internal fun processAlbums(albumList: List<Song>) {
        when {
            albumList.size > albums.size -> {
                val difference = albumList.filterNot {
                    albums.contains(it)
                }
                for (s in difference) {
                    albums.add(s)
                    albums = albums.sortedBy { song ->
                        song.album.toUpperCase(Locale.ROOT)
                    }.toMutableList()
                    val index = albums.indexOfFirst {
                        it.albumID == s.albumID
                    }
                    if (index != -1) {
                        notifyItemInserted(index + 1)
                        notifyItemRangeChanged(index + 1, albums.size + 1)
                    }
                }
            }
            albumList.size < albums.size -> {
                val difference = albums.filterNot {
                    albumList.contains(it)
                }
                for (s in difference) {
                    val index = albums.indexOfFirst {
                        it.albumID == s.albumID
                    }
                    if (index != -1) {
                        albums.removeAt(index)
                        notifyItemRemoved(index + 1)
                        notifyItemRangeChanged(index + 1, albums.size + 1)
                    }
                }
            }
        }
    }

    override fun getItemCount() = albums.size + 2
}
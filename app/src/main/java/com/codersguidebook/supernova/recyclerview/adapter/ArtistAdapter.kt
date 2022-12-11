package com.codersguidebook.supernova.recyclerview.adapter

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
import com.codersguidebook.supernova.AlbumOptions
import com.codersguidebook.supernova.MainActivity
import com.codersguidebook.supernova.R
import com.codersguidebook.supernova.entities.Song
import com.codersguidebook.supernova.ui.artist.ArtistFragmentDirections

class ArtistAdapter(private val activity: MainActivity): Adapter() {

    val songsByAlbumByYear = mutableListOf<Song>()
    var plays = 0

    companion object {
        const val HEADER = 1
        const val ALL_SONGS = 2
        const val ALBUM = 3
    }

    inner class ViewHolderHeader(itemView: View) : RecyclerView.ViewHolder(itemView) {

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

    inner class ViewHolderAllSongs(itemView: View) : RecyclerView.ViewHolder(itemView) {
        init {
            itemView.isClickable = true
            itemView.setOnClickListener {
                if (songsByAlbumByYear.isEmpty()) return@setOnClickListener
                val action = ArtistFragmentDirections.actionSelectArtistSongs(songsByAlbumByYear[0].artist)
                it.findNavController().navigate(action)
            }
        }
    }

    inner class ViewHolderAlbum(itemView: View) : RecyclerView.ViewHolder(itemView) {

        internal var mArtwork = itemView.findViewById<View>(R.id.artwork) as ImageView
        internal var mTitle = itemView.findViewById<View>(R.id.title) as TextView
        internal var mYear = itemView.findViewById<View>(R.id.subtitle) as TextView
        private var mMenu = itemView.findViewById<ImageButton>(R.id.menu)

        init {
            itemView.rootView.isClickable = true
            itemView.rootView.setOnClickListener {
                val action = ArtistFragmentDirections.actionSelectAlbum(songsByAlbumByYear[layoutPosition - 2].albumId)
                it.findNavController().navigate(action)
            }

            itemView.rootView.setOnLongClickListener{
                activity.openDialog(AlbumOptions(songsByAlbumByYear[layoutPosition - 2].albumId))
                return@setOnLongClickListener true
            }

            mMenu.setOnClickListener {
                activity.openDialog(AlbumOptions(songsByAlbumByYear[layoutPosition - 2].albumId))
            }
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
            HEADER -> ViewHolderHeader(
                LayoutInflater.from(parent.context).inflate(R.layout.large_preview, parent, false)
            )
            ALL_SONGS -> ViewHolderAllSongs(
                LayoutInflater.from(parent.context).inflate(R.layout.all_songs, parent, false)
            )
            else -> ViewHolderAlbum(
                LayoutInflater.from(parent.context).inflate(R.layout.item_with_artwork_preview, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder.itemViewType) {
            HEADER -> {
                holder as ViewHolderHeader

                holder.itemView.setBackgroundColor(ContextCompat.getColor(activity, R.color.preview_background))

                val albumIds = songsByAlbumByYear.map { it.albumId }

                when {
                    albumIds.size == 1 -> activity.insertArtwork(albumIds[0], holder.mArtwork)
                    albumIds.size > 1 -> {
                        holder.mArtwork.isGone = true
                        holder.mArtworkGrid.isVisible = true
                        val shuffledAlbumIds = albumIds.shuffled()
                        activity.insertArtwork(shuffledAlbumIds[0], holder.mArtwork1)
                        activity.insertArtwork(shuffledAlbumIds[1], holder.mArtwork2)
                        if (albumIds.size > 2) activity.insertArtwork(shuffledAlbumIds[2], holder.mArtwork3)
                        if (albumIds.size > 3) activity.insertArtwork(shuffledAlbumIds[3], holder.mArtwork4)
                    }
                }

                if (songsByAlbumByYear.isNotEmpty()){
                    holder.mArtist.text = songsByAlbumByYear[0].artist
                    holder.mAlbumCount.text = if (songsByAlbumByYear.size == 1) {
                        activity.getString(R.string.one_album)
                    } else {
                        activity.getString(R.string.n_albums, songsByAlbumByYear.size)
                    }

                    holder.mArtistPlays.text = if (plays == 1) {
                        activity.getString(R.string.played_one_time)
                    } else {
                        activity.getString(R.string.played_n_times, plays)
                    }
                }
            }
            ALBUM -> {
                holder as ViewHolderAlbum

                val current = songsByAlbumByYear[position -2]

                activity.insertArtwork(current.albumId, holder.mArtwork)

                holder.mTitle.text = current.albumName
                holder.mYear.text = current.year
            }
        }
    }

    override fun processLoopIteration(index: Int, song: Song) {
        val recyclerViewIndex = getRecyclerViewIndex(index)
        when {
            index >= songsByAlbumByYear.size -> {
                songsByAlbumByYear.add(song)
                notifyItemInserted(recyclerViewIndex)
            }
            song.albumId != songsByAlbumByYear[index].albumId -> {
                var numberOfItemsRemoved = 0
                do {
                    songsByAlbumByYear.removeAt(index)
                    ++numberOfItemsRemoved
                } while (index < songsByAlbumByYear.size &&
                    song.songId != songsByAlbumByYear[index].songId)

                when {
                    numberOfItemsRemoved == 1 -> notifyItemRemoved(recyclerViewIndex)
                    numberOfItemsRemoved > 1 -> notifyItemRangeRemoved(recyclerViewIndex, numberOfItemsRemoved)
                }

                processLoopIteration(index, song)
            }
            song.albumName != songsByAlbumByYear[index].albumName || song.artist != songsByAlbumByYear[index].artist -> {
                songsByAlbumByYear[index] = song
                notifyItemChanged(recyclerViewIndex)
            }
        }
    }

    override fun getItemCount() = songsByAlbumByYear.size + 2

    override fun getRecyclerViewIndex(index: Int): Int = index + 2
}
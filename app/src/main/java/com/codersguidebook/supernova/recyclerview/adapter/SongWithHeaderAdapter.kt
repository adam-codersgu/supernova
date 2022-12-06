package com.codersguidebook.supernova.recyclerview.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.codersguidebook.supernova.MainActivity
import com.codersguidebook.supernova.R
import com.codersguidebook.supernova.SongOptions
import com.codersguidebook.supernova.entities.Song

abstract class SongWithHeaderAdapter(private val activity: MainActivity): SongAdapter(activity) {

    companion object {
        const val HEADER = 1
        const val SONG = 2
    }

    inner class ViewHolderHeader(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        internal var mArtwork = itemView.findViewById<View>(R.id.largeSongArtwork) as ImageView
        internal var mTitle = itemView.findViewById<View>(R.id.largeTitle) as TextView
        internal var mArtist = itemView.findViewById<View>(R.id.largeSubtitle) as TextView
        internal var mSongCount = itemView.findViewById<View>(R.id.largeSubtitle2) as TextView
    }

    open inner class ViewHolderSong(itemView: View) : RecyclerView.ViewHolder(itemView) {

        internal var mTitle = itemView.findViewById<View>(R.id.title) as TextView
        internal var mArtist = itemView.findViewById<View>(R.id.artist) as TextView
        private var mMenu = itemView.findViewById<ImageButton>(R.id.menu)
        private var songLayout = itemView.findViewById<ConstraintLayout>(R.id.songPreviewLayout)

        init {
            songLayout.isClickable = true
            songLayout.setOnClickListener {
                activity.playNewPlayQueue(songs, layoutPosition - 1)
            }
            songLayout.setOnLongClickListener{
                activity.openDialog(SongOptions(songs[layoutPosition - 1]))
                return@setOnLongClickListener true
            }

            mMenu.setOnClickListener {
                activity.openDialog(SongOptions(songs[layoutPosition - 1]))
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        super.getItemViewType(position)
        return if (position == 0) HEADER
        else SONG
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == HEADER) ViewHolderHeader(
            LayoutInflater.from(parent.context).inflate(
                R.layout.large_preview, parent, false
            )
        )
        else ViewHolderSong(
            LayoutInflater.from(parent.context).inflate(R.layout.song_preview, parent, false)
        )
    }

    override fun getItemCount() = songs.size + 1

    override fun processLoopIteration(index: Int, song: Song) {
        when {
            index >= songs.size -> {
                songs.add(song)
                notifyItemInserted(index + 1)
            }
            song.songId != songs[index].songId -> {
                var numberOfItemsRemoved = 0
                do {
                    songs.removeAt(index)
                    ++numberOfItemsRemoved
                } while (index < songs.size &&
                    song.songId != songs[index].songId)

                when {
                    numberOfItemsRemoved == 1 -> notifyItemRemoved(index + 1)
                    numberOfItemsRemoved > 1 -> notifyItemRangeRemoved(index + 1, numberOfItemsRemoved)
                }

                processLoopIteration(index, song)
            }
            song != songs[index] -> {
                songs[index] = song
                notifyItemChanged(index + 1)
            }
        }
    }
}
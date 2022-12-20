package com.codersguidebook.supernova.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.codersguidebook.supernova.MainActivity
import com.codersguidebook.supernova.R
import com.codersguidebook.supernova.SongOptions
import com.codersguidebook.supernova.entities.Song
import com.codersguidebook.supernova.utils.ImageHandlingHelper

class FavouritesAdapter(private val mainActivity: MainActivity):
    RecyclerView.Adapter<FavouritesAdapter.SongsViewHolder>() {
    var songs = mutableListOf<Song>()
    var previousSongs = mutableListOf<Song>()

    inner class SongsViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView),
        View.OnClickListener {

        internal var mArtwork = itemView.findViewById<View>(R.id.smallSongArtwork) as ImageView
        internal var mTitle = itemView.findViewById<View>(R.id.smallSongTitle) as TextView
        internal var mArtist = itemView.findViewById<View>(R.id.smallSongArtistOrCount) as TextView

        init {
            itemView.isClickable = true
            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener{
                mainActivity.openDialog(SongOptions(songs[layoutPosition]))
                return@setOnLongClickListener true
            }
        }

        override fun onClick(view: View) {
            mainActivity.playNewPlayQueue(songs, layoutPosition)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongsViewHolder {
        return SongsViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.small_song_preview, parent, false))
    }

    override fun onBindViewHolder(holder: SongsViewHolder, position: Int) {
        val current = songs[position]

        ImageHandlingHelper.loadImageByAlbumId(mainActivity.application, current.albumId, holder.mArtwork)

        holder.mTitle.text = current.title
        holder.mArtist.text = current.artist
    }

    internal fun processSongs(songList: List<Song>, added: Boolean) {
        if (added) {
            songs.add(0, songList[0])
            if (songs.size > 10) {
                songs.removeAt(songs.size - 1)
                notifyItemRemoved(songs.size - 1)
                notifyItemInserted(0)
            } else notifyItemInserted(0)
        } else {
            val difference = songs - songList
            for (s in difference) {
                val index = songs.indexOfFirst {
                    it.songId == s.songId
                }
                if (index != -1) {
                    songs.removeAt(index)
                    notifyItemRemoved(index)
                    notifyItemChanged(index)
                }
            }
        }
    }

    override fun getItemCount() = songs.size
}
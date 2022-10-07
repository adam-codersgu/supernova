package com.codersguidebook.supernova.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.codersguidebook.supernova.*
import com.codersguidebook.supernova.entities.Song

class RecentlyPlayedAdapter(private val mainActivity: MainActivity):
    RecyclerView.Adapter<RecentlyPlayedAdapter.SongsViewHolder>() {
    var songs = mutableListOf<Song>()

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
            mainActivity.playListOfSongs(songs, layoutPosition, false)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongsViewHolder {
        return SongsViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.small_song_preview, parent, false))
    }

    override fun onBindViewHolder(holder: SongsViewHolder, position: Int) {
        val current = songs[position]
        mainActivity.insertArtwork(current.albumId, holder.mArtwork)
        holder.mTitle.text = current.title
        holder.mArtist.text = current.artist
    }

    internal fun processSongs(songList: List<Song>) {
        try {
            for ((i, s) in songList.withIndex()) {
                if (s.songId != songs[i].songId) {
                    val index = songs.indexOfFirst {
                        it.songId == s.songId
                    }
                    if (index != -1) {
                        songs.removeAt(index)
                        songs.add(i, s)
                        notifyItemMoved(index, i)
                    } else {
                        songs.add(i, s)
                        if (songs.size > 10) {
                            songs.removeAt(songs.size - 1)
                            notifyItemRemoved(10)
                            notifyItemInserted(i)
                        } else {
                            notifyItemInserted(i)
                            notifyItemChanged(i)
                        }
                    }
                    break
                }
            }
        } catch (e: IndexOutOfBoundsException) {
            val song = songList[songList.size - 1]
            if (songs.size >= 10) {
                songs.removeAt(songs.size -1)
                songs.add(song)
                notifyItemChanged(songs.size - 1)
            } else {
                songs.add(song)
                notifyItemInserted(songs.size)
                notifyItemChanged(songs.size)
            }
        }
    }

    override fun getItemCount() = songs.size
}
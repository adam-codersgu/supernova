package com.codersguidebook.supernova.ui.artist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.codersguidebook.supernova.MainActivity
import com.codersguidebook.supernova.R
import com.codersguidebook.supernova.SongOptions
import com.codersguidebook.supernova.entities.Song
import java.util.*

class ArtistSongsAdapter(private val mainActivity: MainActivity):
    RecyclerView.Adapter<ArtistSongsAdapter.ArtistViewHolder>() {
    var songs = mutableListOf<Song>()

    inner class ArtistViewHolder(itemView: View) :
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
                mainActivity.openDialog(SongOptions(songs[layoutPosition]))
                return@setOnLongClickListener true
            }
        }

        override fun onClick(view: View) {
            mainActivity.playSongs(songs, layoutPosition)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArtistViewHolder {
        return ArtistViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.preview, parent, false))
    }

    override fun onBindViewHolder(holder: ArtistViewHolder, position: Int) {
        val current = songs[position]

        mainActivity.insertArtwork(current.albumId, holder.mArtwork)

        holder.mTitle.text = current.title
        holder.mArtist.text = current.artist
        holder.mBtnMenu.setOnClickListener {
            mainActivity.openDialog(SongOptions(current))
        }
    }

    internal fun processSongs(songList: List<Song>) {
        when {
            songList.size > songs.size -> {
                val difference = songList.filterNot {
                    songs.contains(it)
                }
                for (s in difference) {
                    songs.add(s)
                    songs = songs.sortedBy { song ->
                        song.title.toUpperCase(Locale.ROOT)
                    }.toMutableList()
                    val index = songs.indexOfFirst {
                        it.songId == s.songId
                    }
                    if (index != -1) {
                        notifyItemInserted(index)
                        notifyItemRangeChanged(index, songs.size)
                    }
                }
            }
            songList.size < songs.size -> {
                val difference = songs.filterNot {
                    songList.contains(it)
                }
                for (s in difference) {
                    val index = songs.indexOfFirst {
                        it.songId == s.songId
                    }
                    if (index != -1) {
                        songs.removeAt(index)
                        notifyItemRemoved(index)
                        notifyItemRangeChanged(index, songs.size)
                    }
                }
            }
        }
    }

    override fun getItemCount() = songs.size
}
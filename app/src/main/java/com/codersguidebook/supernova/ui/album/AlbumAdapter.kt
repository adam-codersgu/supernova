package com.codersguidebook.supernova.ui.album

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.codersguidebook.supernova.MainActivity
import com.codersguidebook.supernova.R
import com.codersguidebook.supernova.SongOptions
import com.codersguidebook.supernova.entities.Song

class AlbumAdapter(private val mainActivity: MainActivity):
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var songs = mutableListOf<Song>()
    var displayDiscNumbers = false

    companion object {
        private const val HEADER = 1
        private const val SONG = 2
    }

    inner class ViewHolderAlbumSummary(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        internal var mArtwork = itemView.findViewById<View>(R.id.largeSongArtwork) as ImageView
        internal var mTitle = itemView.findViewById<View>(R.id.largeTitle) as TextView
        internal var mArtist = itemView.findViewById<View>(R.id.largeSubtitle) as TextView
        internal var mSongCount = itemView.findViewById<View>(R.id.largeSubtitle2) as TextView
    }

    inner class ViewHolderSongs(itemView: View) :
        RecyclerView.ViewHolder(itemView),
        View.OnClickListener {

        internal var mDisc = itemView.findViewById<View>(R.id.discNumber) as TextView
        internal var mTrack = itemView.findViewById<View>(R.id.songTrack) as TextView
        internal var mTitle = itemView.findViewById<View>(R.id.title) as TextView
        internal var mArtist = itemView.findViewById<View>(R.id.artist) as TextView
        private var mBtnAlbumMenu = itemView.findViewById<ImageButton>(R.id.albumMenu)
        private var songLayout = itemView.findViewById<ConstraintLayout>(R.id.songPreviewLayout)

        init {
            songLayout.isClickable = true
            songLayout.setOnClickListener(this)
            songLayout.setOnLongClickListener{
                mainActivity.openDialog(SongOptions(songs[layoutPosition - 1]))
                return@setOnLongClickListener true
            }

            mBtnAlbumMenu.setOnClickListener {
                mainActivity.openDialog(SongOptions(songs[layoutPosition - 1]))
            }
        }

        override fun onClick(view: View) {
            mainActivity.playNewPlayQueue(songs, layoutPosition - 1)
        }
    }

    override fun getItemViewType(position: Int): Int {
        super.getItemViewType(position)
        return if (position == 0) HEADER
        else SONG
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == HEADER) ViewHolderAlbumSummary(LayoutInflater.from(parent.context).inflate(R.layout.large_preview, parent, false))
        else ViewHolderSongs(LayoutInflater.from(parent.context).inflate(R.layout.album_preview, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder.itemViewType) {

            HEADER -> {
                holder as ViewHolderAlbumSummary

                holder.itemView.setBackgroundColor(ContextCompat.getColor(mainActivity, R.color.preview_background))

                if (songs.isNotEmpty()){
                    mainActivity.insertArtwork(songs[0].albumId, holder.mArtwork)
                    holder.mTitle.text = songs[0].albumName
                    holder.mArtist.text = songs[0].artist
                    val songCountInt = songs.size
                    holder.mSongCount.text = if (songCountInt == 1) mainActivity.getString(R.string.displayed_song)
                    else mainActivity.getString(R.string.displayed_songs, songCountInt)
                }
            }

            SONG -> {
                holder as ViewHolderSongs
                val current = songs[position -1]

                holder.mDisc.isVisible = false
                if (displayDiscNumbers && songs.size > 1) {
                    if (position - 1 == 0 || songs[position -1].track.toString().substring(0, 1) !=
                        songs[position -2].track.toString().substring(0, 1)) {
                        holder.mDisc.isVisible = true
                        val disc = current.track.toString().substring(0, 1)
                        val text = mainActivity.getString(R.string.disc_number, disc)
                        holder.mDisc.text = text
                    }
                }

                holder.mTrack.text = current.track.toString().substring(1, 4).toInt().toString()
                holder.mTitle.text = current.title
                holder.mArtist.text = current.artist
            }
        }
    }

    override fun getItemCount() = 1 + songs.size
}
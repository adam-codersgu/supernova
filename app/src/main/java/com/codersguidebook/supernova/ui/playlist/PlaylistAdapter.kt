package com.codersguidebook.supernova.ui.playlist

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.codersguidebook.supernova.MainActivity
import com.codersguidebook.supernova.R
import com.codersguidebook.supernova.entities.Playlist
import com.codersguidebook.supernova.entities.Song

class PlaylistAdapter(private val fragment: PlaylistFragment,
                      private val mainActivity: MainActivity
):
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var showHandles = false
    var playlist: Playlist? = null
    var songs = mutableListOf<Song>()

    companion object {
        private const val HEADER = 1
        private const val SONG = 2
    }

    class ViewHolderPlaylistSummary(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        internal var mArtwork =itemView.findViewById<View>(R.id.largeSongArtwork) as ImageView
        internal var mArtworkGrid = itemView.findViewById(R.id.imageGrid) as GridLayout
        internal var mArtwork1 = itemView.findViewById<View>(R.id.artwork1) as ImageView
        internal var mArtwork2 = itemView.findViewById<View>(R.id.artwork2) as ImageView
        internal var mArtwork3 = itemView.findViewById<View>(R.id.artwork3) as ImageView
        internal var mArtwork4 = itemView.findViewById<View>(R.id.artwork4) as ImageView
        internal var mTitle = itemView.findViewById<View>(R.id.largeTitle) as TextView
        internal var mSongCount = itemView.findViewById<View>(R.id.largeSubtitle) as TextView
        internal var mSubtitle2 = itemView.findViewById<View>(R.id.largeSubtitle2) as TextView
    }

    inner class PlaylistSongViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView),
        View.OnClickListener {

        internal var mArtwork = itemView.findViewById<View>(R.id.playlistArtwork) as ImageView
        internal var mTitle = itemView.findViewById<View>(R.id.playlistTitle) as TextView
        internal var mArtist = itemView.findViewById<View>(R.id.playlistArtistOrYear) as TextView
        internal var mPlays = itemView.findViewById<View>(R.id.playlistSongPlays) as TextView
        internal var mBtnSongMenu = itemView.findViewById<ImageButton>(R.id.playlistMenu)

        init {
            itemView.isClickable = true
            itemView.setOnClickListener(this)
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
        return if (viewType == HEADER) ViewHolderPlaylistSummary(LayoutInflater.from(parent.context).inflate(R.layout.large_preview, parent, false))
        else PlaylistSongViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.most_played_preview, parent, false))
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder.itemViewType) {
            HEADER -> {
                holder as ViewHolderPlaylistSummary

                holder.itemView.setBackgroundColor(ContextCompat.getColor(mainActivity, R.color.preview_background))

                if (!mainActivity.insertPlaylistArtwork(playlist ?: return, holder.mArtwork)) {
                    val uniqueArtworks = songs.distinctBy {
                        it.albumId
                    }

                    when {
                        uniqueArtworks.size > 1 -> {
                            holder.mArtwork.isGone = true
                            holder.mArtworkGrid.isVisible = true
                            val shuffledArtworks = uniqueArtworks.shuffled()
                            mainActivity.insertArtwork(shuffledArtworks[0].albumId, holder.mArtwork1)
                            mainActivity.insertArtwork(shuffledArtworks[1].albumId, holder.mArtwork2)
                            if (uniqueArtworks.size > 2) mainActivity.insertArtwork(shuffledArtworks[2].albumId, holder.mArtwork3)
                            if (uniqueArtworks.size > 3)  mainActivity.insertArtwork(shuffledArtworks[3].albumId, holder.mArtwork4)
                        }
                        songs.isNotEmpty() -> mainActivity.insertArtwork(songs[0].albumId, holder.mArtwork)
                    }
                }

                if (songs.isNotEmpty()){
                    holder.mTitle.text = playlist?.name
                    holder.mSongCount.text = if (songs.size == 1) "1 Song"
                    else songs.size.toString() + " songs"
                    holder.mSubtitle2.isGone = true
                }
            }
            SONG -> {
                holder as PlaylistSongViewHolder
                val current = songs[position -1]

                if (showHandles) {
                    holder.mArtwork.setColorFilter(ContextCompat.getColor(fragment.requireActivity(), R.color.onSurface60))
                    holder.mArtwork.layoutParams.width = mainActivity.resources.getDimension(R.dimen.handle_width).toInt()
                    Glide.with(fragment)
                        .load(R.drawable.ic_drag_handle)
                        .into(holder.mArtwork)
                    holder.mArtwork.setOnTouchListener { _, event ->
                        if (event.actionMasked == MotionEvent.ACTION_DOWN) fragment.startDragging(holder)
                        return@setOnTouchListener true
                    }
                    holder.itemView.setOnLongClickListener {
                        return@setOnLongClickListener true
                    }
                } else {
                    holder.mArtwork.layoutParams.width = mainActivity.resources.getDimension(R.dimen.artwork_preview_width).toInt()
                    holder.mArtwork.clearColorFilter()
                    holder.mArtwork.setOnTouchListener { _, _ ->
                        return@setOnTouchListener true
                    }
                    mainActivity.insertArtwork(current.albumId, holder.mArtwork)
                    holder.itemView.setOnLongClickListener {
                        fragment.openDialog(songs.toMutableList(), position -1, playlist!!)
                        return@setOnLongClickListener true
                    }
                }

                holder.mTitle.text = current.title
                holder.mArtist.text = current.artist
                if (playlist?.name == mainActivity.getString(R.string.most_played)) {
                    holder.mPlays.isVisible = true
                    val plays = current.plays
                    val playsText = if (plays == 1) "1 play"
                    else "$plays plays"
                    holder.mPlays.text = playsText
                    when (position) {
                        1 -> {
                            holder.mTitle.setTextColor(ContextCompat.getColor(mainActivity, R.color.gold))
                            holder.mArtist.setTextColor(ContextCompat.getColor(mainActivity, R.color.gold60))
                            holder.mPlays.setTextColor(ContextCompat.getColor(mainActivity, R.color.gold60))
                            holder.mBtnSongMenu.setColorFilter(ContextCompat.getColor(mainActivity, R.color.gold60))
                        }
                        2 -> {
                            holder.mTitle.setTextColor(ContextCompat.getColor(mainActivity, R.color.silver))
                            holder.mArtist.setTextColor(ContextCompat.getColor(mainActivity, R.color.silver60))
                            holder.mPlays.setTextColor(ContextCompat.getColor(mainActivity, R.color.silver60))
                            holder.mBtnSongMenu.setColorFilter(ContextCompat.getColor(mainActivity, R.color.silver60))
                        }
                        3 -> {
                            holder.mTitle.setTextColor(ContextCompat.getColor(mainActivity, R.color.bronze))
                            holder.mArtist.setTextColor(ContextCompat.getColor(mainActivity, R.color.bronze60))
                            holder.mPlays.setTextColor(ContextCompat.getColor(mainActivity, R.color.bronze60))
                            holder.mBtnSongMenu.setColorFilter(ContextCompat.getColor(mainActivity, R.color.bronze60))
                        }
                        else -> {
                            holder.mTitle.setTextColor(ContextCompat.getColor(mainActivity, android.R.color.white))
                            holder.mArtist.setTextColor(ContextCompat.getColor(mainActivity, R.color.onSurface60))
                            holder.mPlays.setTextColor(ContextCompat.getColor(mainActivity, R.color.onSurface60))
                            holder.mBtnSongMenu.setColorFilter(ContextCompat.getColor(mainActivity, R.color.onSurface60))
                        }
                    }
                }

                holder.mBtnSongMenu.setOnClickListener {
                    fragment.openDialog(songs.toMutableList(), position -1, playlist!!)
                }
            }
        }
    }

    internal fun manageHandles(applyHandles: Boolean){
        this.showHandles = applyHandles
        notifyDataSetChanged()
    }
    
    internal fun processSongs(newSongs: MutableList<Song>) {
        when {
            songs.isEmpty() -> {
                songs = newSongs
                notifyItemRangeInserted(0, newSongs.size)
            }
            newSongs.size > songs.size -> {
                songs = newSongs
                notifyItemInserted(newSongs.size)
                notifyItemRangeChanged(newSongs.size, newSongs.size + 1)
                notifyItemChanged(0)
            }
            newSongs.size < songs.size -> {
                try {
                    val difference = songs.size - newSongs.size
                    var i = 0
                    outer@ while (i < difference) {
                        for ((index, _) in songs.withIndex()) {
                            if (songs[index].songId != newSongs[index].songId) {
                                songs.removeAt(index)
                                notifyItemRemoved(index + 1)
                                notifyItemChanged(index + 1)
                                notifyItemChanged(0)
                                ++i
                                break
                            }
                        }
                    }
                } catch (e: IndexOutOfBoundsException) {
                    if (songs[songs.size - 1].songId != newSongs[newSongs.size - 1].songId) {
                        val index = songs.size - 1
                        songs.removeAt(index)
                        notifyItemRemoved(index + 1)
                        notifyItemChanged(index + 1)
                        notifyItemChanged(0)
                    }
                }
            }
        }
    }

    override fun getItemCount() = songs.size + 1
}
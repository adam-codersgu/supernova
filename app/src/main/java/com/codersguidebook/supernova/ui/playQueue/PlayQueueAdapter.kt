package com.codersguidebook.supernova.ui.playQueue

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.codersguidebook.supernova.*
import com.codersguidebook.supernova.entities.QueueItem

class PlayQueueAdapter(private val fragment: PlayQueueFragment
, private val activity: MainActivity): RecyclerView.Adapter<PlayQueueAdapter.PlayQueueViewHolder>() {
    var currentlyPlayingQueueID = -1
    var playQueue = mutableListOf<QueueItem>()

    inner class PlayQueueViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView),
        View.OnClickListener {

        internal var mSongTitle = itemView.findViewById<View>(R.id.title) as TextView
        internal var mSongArtist = itemView.findViewById<View>(R.id.subtitle) as TextView
        internal var mHandle = itemView.findViewById<ImageView>(R.id.handleView)
        private var mBtnSongMenu = itemView.findViewById<ImageButton>(R.id.buttonPlayQueueMenu)

        init {
            itemView.isClickable = true
            itemView.setOnClickListener(this)
            mBtnSongMenu.setOnClickListener {
                val isCurrentlyPlayingSelected = playQueue[layoutPosition].queueID == currentlyPlayingQueueID
                activity.openDialog(QueueOptions(playQueue[layoutPosition], isCurrentlyPlayingSelected, layoutPosition))
            }
        }

        override fun onClick(view: View) {
            activity.skipToQueueItem(layoutPosition)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayQueueViewHolder {
        return PlayQueueViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.play_queue_song, parent, false))
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: PlayQueueViewHolder, position: Int) {
        val currentSong = playQueue[position].song

        holder.mSongTitle.text = currentSong.title
        holder.mSongArtist.text = currentSong.artist
        if (playQueue[position].queueID == currentlyPlayingQueueID) {
            holder.mSongTitle.setTextColor(ContextCompat.getColor(activity, R.color.accent))
            holder.mSongArtist.setTextColor(ContextCompat.getColor(activity, R.color.accent))
        } else {
            holder.mSongTitle.setTextColor(ContextCompat.getColor(activity, R.color.onSurface60))
            holder.mSongArtist.setTextColor(ContextCompat.getColor(activity, R.color.onSurface60))
        }

        holder.mHandle.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) fragment.startDragging(holder)
            return@setOnTouchListener true
        }
    }

    internal fun currentlyPlayingSongChanged(newQueueID: Int) {
        val oldCurrentlyPlayingIndex = playQueue.indexOfFirst {
            it.queueID == currentlyPlayingQueueID
        }

        currentlyPlayingQueueID = newQueueID
        if (oldCurrentlyPlayingIndex != -1) notifyItemChanged(oldCurrentlyPlayingIndex)

        val newCurrentlyPlayingIndex = playQueue.indexOfFirst {
            it.queueID == currentlyPlayingQueueID
        }
        notifyItemChanged(newCurrentlyPlayingIndex)
    }

    override fun getItemCount() = playQueue.size
}
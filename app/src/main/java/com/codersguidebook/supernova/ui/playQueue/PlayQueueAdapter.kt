package com.codersguidebook.supernova.ui.playQueue

import android.annotation.SuppressLint
import android.support.v4.media.session.MediaSessionCompat.QueueItem
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.codersguidebook.supernova.MainActivity
import com.codersguidebook.supernova.QueueOptions
import com.codersguidebook.supernova.R

class PlayQueueAdapter(private val fragment: PlayQueueFragment
, private val activity: MainActivity): RecyclerView.Adapter<PlayQueueAdapter.PlayQueueViewHolder>() {
    var currentlyPlayingQueueId = -1L
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
                val isCurrentlyPlayingSelected =
                    playQueue[layoutPosition].queueId == currentlyPlayingQueueId
                activity.openDialog(QueueOptions(playQueue[layoutPosition],
                    isCurrentlyPlayingSelected, layoutPosition))
            }
        }

        override fun onClick(view: View) {
            activity.skipToQueueItem(playQueue[layoutPosition].queueId)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayQueueViewHolder {
        return PlayQueueViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.play_queue_song, parent, false))
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: PlayQueueViewHolder, position: Int) {
        val currentQueueItemDescription = playQueue[position].description

        holder.mSongTitle.text = currentQueueItemDescription.title
        holder.mSongArtist.text = currentQueueItemDescription.subtitle
        if (playQueue[position].queueId == currentlyPlayingQueueId) {
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

    internal fun changeCurrentlyPlayingQueueItemId(newQueueId: Long) {
        val oldCurrentlyPlayingIndex = playQueue.indexOfFirst {
            it.queueId == currentlyPlayingQueueId
        }

        currentlyPlayingQueueId = newQueueId
        if (oldCurrentlyPlayingIndex != -1) notifyItemChanged(oldCurrentlyPlayingIndex)

        val newCurrentlyPlayingIndex = playQueue.indexOfFirst {
            it.queueId == currentlyPlayingQueueId
        }
        notifyItemChanged(newCurrentlyPlayingIndex)
    }

    override fun getItemCount() = playQueue.size

    fun removeQueueItemById(queueItemId: Long) {
        val index = playQueue.indexOfFirst { item ->
            item.queueId == queueItemId
        }
        if (index != -1) {
            playQueue.removeAt(index)
            notifyItemRemoved(index)
        }
    }
}
package com.codersguidebook.supernova.fragment.adapter

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
import com.codersguidebook.supernova.dialogs.QueueOptions
import com.codersguidebook.supernova.R
import com.codersguidebook.supernova.ui.playQueue.PlayQueueFragment

class PlayQueueAdapter(private val fragment: PlayQueueFragment
, private val activity: MainActivity): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var currentlyPlayingQueueId = -1L
    val playQueue = mutableListOf<QueueItem>()

    inner class ViewHolderPlayQueue(itemView: View) : RecyclerView.ViewHolder(itemView) {

        internal var txtSongTitle = itemView.findViewById<View>(R.id.title) as TextView
        internal var txtSongArtist = itemView.findViewById<View>(R.id.subtitle) as TextView
        internal var handle = itemView.findViewById<ImageView>(R.id.handleView)
        private var btnSongMenu = itemView.findViewById<ImageButton>(R.id.buttonPlayQueueMenu)

        init {
            itemView.isClickable = true
            itemView.setOnClickListener {
                activity.skipToQueueItem(playQueue[layoutPosition].queueId)
            }
            btnSongMenu.setOnClickListener {
                val isCurrentlyPlayingSelected =
                    playQueue[layoutPosition].queueId == currentlyPlayingQueueId
                activity.openDialog(
                    QueueOptions(playQueue[layoutPosition],
                    isCurrentlyPlayingSelected)
                )
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ViewHolderPlayQueue(LayoutInflater.from(parent.context)
            .inflate(R.layout.play_queue_song, parent, false))
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        holder as ViewHolderPlayQueue
        val currentQueueItemDescription = playQueue[position].description

        holder.txtSongTitle.text = currentQueueItemDescription.title
        holder.txtSongArtist.text = currentQueueItemDescription.subtitle
        if (playQueue[position].queueId == currentlyPlayingQueueId) {
            holder.txtSongTitle.setTextColor(ContextCompat.getColor(activity, R.color.accent))
            holder.txtSongArtist.setTextColor(ContextCompat.getColor(activity, R.color.accent))
        } else {
            holder.txtSongTitle.setTextColor(ContextCompat.getColor(activity, R.color.onSurface60))
            holder.txtSongArtist.setTextColor(ContextCompat.getColor(activity, R.color.onSurface60))
        }

        holder.handle.setOnTouchListener { _, event ->
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

    fun processLoopIteration(index: Int, queueItem: QueueItem) {
        when {
            index >= playQueue.size -> {
                playQueue.add(queueItem)
                notifyItemInserted(index)
            }
            queueItem.queueId != playQueue[index].queueId -> {
                // Check if the item has been moved
                val oldIndex = playQueue.indexOfFirst {
                    it.queueId == queueItem.queueId
                }
                if (oldIndex != -1) {
                    playQueue.removeAt(oldIndex)
                    playQueue.add(index, queueItem)
                    notifyItemMoved(oldIndex, index)
                } else {
                    playQueue.add(index, queueItem)
                    notifyItemInserted(index)
                }
            }
            queueItem.description.title != playQueue[index].description.title ||
                    queueItem.description.subtitle != playQueue[index].description.subtitle -> {
                playQueue[index] = queueItem
                notifyItemChanged(index)
            }
        }
    }

    override fun getItemCount() = playQueue.size
}
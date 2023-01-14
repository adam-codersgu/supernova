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
import com.codersguidebook.supernova.R
import com.codersguidebook.supernova.dialogs.QueueOptions
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

    /**
     * Handle updates to the content of the RecyclerView. The below method will determine what
     * changes are required when an element/elements is/are changed, inserted, or deleted.
     * This enhanced process loop iteration method assumes each queue item can only appear once.
     *
     * @param newPlayQueue The new list of QueueItem objects that should be displayed.
     */
    fun processNewPlayQueue(newPlayQueue: List<QueueItem>) {
        for ((index, queueItem) in newPlayQueue.withIndex()) {
            when {
                index >= playQueue.size -> {
                    playQueue.add(queueItem)
                    notifyItemInserted(index)
                }
                queueItem.queueId != playQueue[index].queueId -> {
                    // Check if the queueItem is a new entry to the list
                    val queueItemIsNewEntry = playQueue.find { it.queueId == queueItem.queueId } == null
                    if (queueItemIsNewEntry) {
                        playQueue.add(index, queueItem)
                        notifyItemInserted(index)
                        continue
                    }

                    // Check if queueItem(s) has/have been removed from the list
                    val queueItemIsRemoved = newPlayQueue.find { it.queueId == playQueue[index].queueId } == null
                    if (queueItemIsRemoved) {
                        var numberOfItemsRemoved = 0
                        do {
                            playQueue.removeAt(index)
                            ++numberOfItemsRemoved
                        } while (index < playQueue.size &&
                            newPlayQueue.find { it.queueId == playQueue[index].queueId } == null)

                        when {
                            numberOfItemsRemoved == 1 -> notifyItemRemoved(index)
                            numberOfItemsRemoved > 1 -> notifyItemRangeRemoved(index,
                                numberOfItemsRemoved)
                        }

                        // Check if removing the queueItem(s) has fixed the list
                        if (queueItem.queueId == playQueue[index].queueId) continue
                    }

                    // Check if the queueItem has been moved earlier in the list
                    val oldIndex = playQueue.indexOfFirst { it.queueId == queueItem.queueId }
                    if (oldIndex != -1 && oldIndex > index) {
                        playQueue.removeAt(oldIndex)
                        playQueue.add(index, queueItem)
                        notifyItemMoved(oldIndex, index)
                        continue
                    }

                    // Check if the queueItem(s) has been moved later in the list
                    var newIndex = newPlayQueue.indexOfFirst { it.queueId == playQueue[index].queueId }
                    if (newIndex != -1) {
                        do {
                            playQueue.removeAt(index)

                            if (newIndex <= playQueue.size) {
                                playQueue.add(newIndex, queueItem)
                                notifyItemMoved(index, newIndex)
                            } else {
                                notifyItemRemoved(index)
                            }

                            // See if further playQueue need to be moved
                            newIndex = newPlayQueue.indexOfFirst { it.queueId == playQueue[index].queueId }
                        } while (index < playQueue.size &&
                            queueItem.queueId != playQueue[index].queueId &&
                            newIndex != -1)

                        // Check if moving the queueItem(s) has fixed the list
                        if (queueItem.queueId == playQueue[index].queueId) continue
                        else {
                            playQueue.add(index, queueItem)
                            notifyItemInserted(index)
                        }
                    }
                }
                queueItem.description.title != playQueue[index].description.title ||
                        queueItem.description.subtitle != playQueue[index].description.subtitle -> {
                    playQueue[index] = queueItem
                    notifyItemChanged(index)
                }
            }
        }

        if (playQueue.size > newPlayQueue.size) {
            val numberItemsToRemove = playQueue.size - newPlayQueue.size
            repeat(numberItemsToRemove) { playQueue.removeLast() }
            notifyItemRangeRemoved(newPlayQueue.size, numberItemsToRemove)
        }
    }

    override fun getItemCount() = playQueue.size
}
package com.codersguidebook.supernova.fragment.adapter

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Build
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.media3.common.MediaItem
import androidx.recyclerview.widget.RecyclerView
import com.codersguidebook.supernova.MainActivity
import com.codersguidebook.supernova.R
import com.codersguidebook.supernova.dialogs.QueueOptions
import com.codersguidebook.supernova.ui.playQueue.PlayQueueFragment
import com.google.android.material.color.MaterialColors

class PlayQueueAdapter(private val fragment: PlayQueueFragment,
                       private val activity: MainActivity): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var currentlyPlayingQueueIndex = -1
    val playQueue = mutableListOf<MediaItem>()

    inner class ViewHolderPlayQueue(itemView: View) : RecyclerView.ViewHolder(itemView) {

        internal var txtSongTitle = itemView.findViewById<View>(R.id.title) as TextView
        internal var txtSongArtist = itemView.findViewById<View>(R.id.subtitle) as TextView
        internal var handle = itemView.findViewById<ImageView>(R.id.handleView)
        private var btnSongMenu = itemView.findViewById<ImageButton>(R.id.buttonPlayQueueMenu)

        init {
            itemView.isClickable = true
            itemView.setOnClickListener {
                activity.skipToQueueIndex(layoutPosition)
                activity.play()
            }
            btnSongMenu.setOnClickListener {
                val isCurrentlyPlayingSelected = layoutPosition == currentlyPlayingQueueIndex
                activity.openDialog(
                    QueueOptions(playQueue[layoutPosition], layoutPosition, isCurrentlyPlayingSelected)
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

        val metadata = playQueue[position].mediaMetadata

        val accent = MaterialColors.getColor(activity, com.google.android.material.R.attr.colorAccent, Color.CYAN)
        val onSurface = MaterialColors.getColor(activity, com.google.android.material.R.attr.colorOnSurface, Color.LTGRAY)
        val onSurface60 = MaterialColors.compositeARGBWithAlpha(onSurface, 153)

        holder.handle.drawable.setTint(onSurface60)
        holder.handle.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) fragment.startDragging(holder)
            return@setOnTouchListener true
        }

        holder.txtSongTitle.text = metadata.title ?: activity.getString(R.string.default_title)
        holder.txtSongArtist.text = metadata.artist
            ?: metadata.subtitle
            ?: activity.getString(R.string.default_artist)

        if (position == currentlyPlayingQueueIndex) {
            holder.txtSongTitle.setTextColor(accent)
            holder.txtSongArtist.setTextColor(accent)
        } else {
            holder.txtSongTitle.setTextColor(onSurface60)
            holder.txtSongArtist.setTextColor(onSurface60)
        }
    }

    fun changeCurrentlyPlayingQueueItemIndex(newIndex: Int) {
        val oldCurrentlyPlayingIndex = currentlyPlayingQueueIndex
        currentlyPlayingQueueIndex = newIndex

        if (oldCurrentlyPlayingIndex != -1) notifyItemChanged(oldCurrentlyPlayingIndex)
        notifyItemChanged(currentlyPlayingQueueIndex)
    }

    /**
     * Handle updates to the content of the RecyclerView. The below method will determine what
     * changes are required when an element/elements is/are changed, inserted, or deleted.
     * This enhanced process loop iteration method assumes each queue item can only appear once.
     *
     * @param newPlayQueue The new list of QueueItem objects that should be displayed.
     */
    fun processNewPlayQueue(newPlayQueue: List<MediaItem>) {
        if (newPlayQueue == playQueue) return

        for ((index, queueItem) in newPlayQueue.withIndex()) {
            when {
                index >= playQueue.size -> {
                    playQueue.add(queueItem)
                    notifyItemInserted(index)
                }
                playQueue.find { it.mediaId == queueItem.mediaId } == null -> {
                    playQueue.add(index, queueItem)
                    notifyItemInserted(index)
                }
                newPlayQueue.find { it.mediaId == playQueue[index].mediaId } == null -> {
                    var numberOfItemsRemoved = 0
                    do {
                        playQueue.removeAt(index)
                        ++numberOfItemsRemoved
                    } while (index < playQueue.size &&
                        newPlayQueue.find { it.mediaId == playQueue[index].mediaId } == null)

                    when {
                        numberOfItemsRemoved == 1 -> notifyItemRemoved(index)
                        numberOfItemsRemoved > 1 -> notifyItemRangeRemoved(index,
                            numberOfItemsRemoved)
                    }
                }
            }
        }

        if (playQueue.size > newPlayQueue.size) {
            val numberItemsToRemove = playQueue.size - newPlayQueue.size
            repeat(numberItemsToRemove) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                    playQueue.removeLast()
                } else {
                    playQueue.removeAt(playQueue.size - 1)
                }
            }
            notifyItemRangeRemoved(newPlayQueue.size, numberItemsToRemove)
        }
    }

    override fun getItemCount() = playQueue.size
}
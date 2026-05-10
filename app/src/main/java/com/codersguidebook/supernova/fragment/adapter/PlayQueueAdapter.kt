package com.codersguidebook.supernova.fragment.adapter

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.media3.common.MediaItem
import androidx.recyclerview.widget.RecyclerView
import com.codersguidebook.supernova.MainActivity
import com.codersguidebook.supernova.R
import com.codersguidebook.supernova.dialogs.QueueOptions
import com.codersguidebook.supernova.fragment.BaseDialogFragment
import com.codersguidebook.supernova.fragment.adapter.viewholder.ViewHolderWithMenu
import com.codersguidebook.supernova.ui.playQueue.PlayQueueFragment
import com.google.android.material.color.MaterialColors

class PlayQueueAdapter(private val fragment: PlayQueueFragment,
                       private val activity: MainActivity): Adapter() {
    var currentlyPlayingQueueIndex = -1

    inner class ViewHolderPlayQueue(itemView: View) : ViewHolderWithMenu(itemView) {

        internal var handle = itemView.findViewById<ImageView>(R.id.handleView)

        override fun getActivity(): MainActivity {
            return activity
        }

        override fun rootViewAction() {
            activity.skipToQueueIndex(layoutPosition)
            activity.play()
        }

        override fun getOptionsDialog(): BaseDialogFragment {
            val isCurrentlyPlayingSelected = layoutPosition == currentlyPlayingQueueIndex
            return QueueOptions((items[layoutPosition] as MediaItem), layoutPosition, isCurrentlyPlayingSelected)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ViewHolderPlayQueue(LayoutInflater.from(parent.context)
            .inflate(R.layout.play_queue_song, parent, false))
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        holder as ViewHolderPlayQueue

        val metadata = (items[position] as MediaItem).mediaMetadata

        val accent = MaterialColors.getColor(activity, com.google.android.material.R.attr.colorSecondary, Color.CYAN)
        val onSurface = MaterialColors.getColor(activity, com.google.android.material.R.attr.colorOnSurface, Color.LTGRAY)
        val onSurface60 = MaterialColors.compositeARGBWithAlpha(onSurface, 153)

        holder.handle.drawable.setTint(onSurface60)
        holder.handle.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) fragment.startDragging(holder)
            return@setOnTouchListener true
        }

        holder.mTitle.text = metadata.title ?: activity.getString(R.string.default_title)
        holder.mSubtitle.text = metadata.artist
            ?: metadata.subtitle
            ?: activity.getString(R.string.default_artist)

        if (position == currentlyPlayingQueueIndex) {
            holder.mTitle.setTextColor(accent)
            holder.mSubtitle.setTextColor(accent)
        } else {
            holder.mTitle.setTextColor(onSurface60)
            holder.mSubtitle.setTextColor(onSurface60)
        }
    }

    fun changeCurrentlyPlayingQueueItemIndex(newIndex: Int) {
        val oldCurrentlyPlayingIndex = currentlyPlayingQueueIndex
        currentlyPlayingQueueIndex = newIndex

        if (oldCurrentlyPlayingIndex != -1) notifyItemChanged(oldCurrentlyPlayingIndex)
        notifyItemChanged(currentlyPlayingQueueIndex)
    }

    override fun processNewItems(newItems: List<Any>) {
        super.processNewItems(newItems)
        fragment.updateCurrentlyPlayingIndex()
    }

    override fun itemsEqual(item1: Any, item2: Any): Boolean {
        return (item1 as MediaItem).mediaId == (item2 as MediaItem).mediaId
    }
}
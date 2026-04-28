package com.codersguidebook.supernova.fragment.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.codersguidebook.supernova.MainActivity
import com.codersguidebook.supernova.R
import com.codersguidebook.supernova.dialogs.SongOptions
import com.codersguidebook.supernova.entities.Song
import com.codersguidebook.supernova.fragment.BaseDialogFragment
import com.codersguidebook.supernova.fragment.adapter.viewholder.ViewHolderHeader
import com.codersguidebook.supernova.fragment.adapter.viewholder.ViewHolderWithMenu
import com.codersguidebook.supernova.utils.ImageHandlingHelper

class AlbumAdapter(private val activity: MainActivity): SongWithHeaderAdapter(activity) {

    var displayDiscNumbers = false

    inner class ViewHolderSongWithDisc(itemView: View) : ViewHolderWithMenu(itemView) {

        private var songLayout: ConstraintLayout = itemView.findViewById(R.id.songPreviewLayout)
        internal var mDisc: TextView = itemView.findViewById(R.id.discNumber)
        internal var mTrack: TextView = itemView.findViewById(R.id.songTrack)

        init {
            itemView.rootView.isClickable = false
            itemView.rootView.setOnClickListener(null)
            itemView.rootView.setOnLongClickListener(null)

            songLayout.isClickable = true

            songLayout.setOnClickListener {
                rootViewAction()
            }

            songLayout.setOnLongClickListener {
                openDialog()
                return@setOnLongClickListener true
            }
        }

        override fun getActivity(): MainActivity {
            return activity
        }

        @Suppress("UNCHECKED_CAST")
        override fun rootViewAction() {
            activity.playNewPlayQueue((items as List<Song>), layoutPosition - 1)
        }

        override fun getOptionsDialog(): BaseDialogFragment {
            return SongOptions((items[layoutPosition - 1] as Song))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == HEADER) ViewHolderHeader(
            LayoutInflater.from(parent.context).inflate(R.layout.header, parent, false)
        ) else ViewHolderSongWithDisc(
            LayoutInflater.from(parent.context).inflate(R.layout.song_with_disc_preview, parent, false)
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder.itemViewType) {

            HEADER -> {
                holder as ViewHolderHeader

                holder.itemView.setBackgroundColor(ContextCompat.getColor(activity, R.color.preview_background))

                if (items.isNotEmpty()){
                    val song = items[0] as Song
                    ImageHandlingHelper.loadImageByAlbumId(activity.application,
                        song.albumId, holder.mArtwork)
                    holder.mTitle.text = song.albumName ?: activity.getString(R.string.default_album)
                    holder.mSubtitle.text = song.artist ?: activity.getString(R.string.default_artist)
                }
                val songCountInt = items.size
                holder.mSubtitle2.text = if (songCountInt == 1) activity.getString(R.string.displayed_song)
                else activity.getString(R.string.displayed_songs, songCountInt)
            }

            SONG -> {
                holder as ViewHolderSongWithDisc
                val current = items[position -1] as Song

                holder.mDisc.isGone = true
                if (shouldDisplayDiscNumber(position)) {
                    holder.mDisc.isVisible = true
                    val disc = current.track.toString().substring(0, 1)
                    val text = activity.getString(R.string.disc_number, disc)
                    holder.mDisc.text = text
                }

                holder.mTrack.text = current.track.toString().substring(1, 4).toInt().toString()
                holder.mTitle.text = current.title ?: activity.getString(R.string.default_title)
                holder.mSubtitle.text = current.artist ?: activity.getString(R.string.default_artist)
            }
        }
    }

    private fun shouldDisplayDiscNumber(position: Int): Boolean {
        if (displayDiscNumbers && items.size > 1) {
            if (position - 1 == 0 || (items[position -1] as Song).track.toString().substring(0, 1) !=
                (items[position -2] as Song).track.toString().substring(0, 1)) {
                return true
            }
        }
        return false
    }

    override fun removeItem(index: Int) {
        val discNumberShouldBeDisplayed = shouldDisplayDiscNumber(index)
        super.removeItem(index)
        if (discNumberShouldBeDisplayed) {
            notifyItemChanged(index)
        }
    }
}
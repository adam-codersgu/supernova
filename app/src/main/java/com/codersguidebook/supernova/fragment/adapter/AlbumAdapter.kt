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
import com.codersguidebook.supernova.utils.ImageHandlingHelper

class AlbumAdapter(private val activity: MainActivity): SongWithHeaderAdapter(activity) {

    var displayDiscNumbers = false

    inner class ViewHolderSongWithDisc(itemView: View) : ViewHolderSong(itemView) {

        private var songLayout = itemView.findViewById<ConstraintLayout>(R.id.songPreviewLayout)
        internal var mDisc = itemView.findViewById(R.id.discNumber) as TextView
        internal var mTrack = itemView.findViewById(R.id.songTrack) as TextView

        init {
            itemView.rootView.isClickable = false
            itemView.rootView.setOnClickListener(null)
            itemView.rootView.setOnLongClickListener(null)

            songLayout.isClickable = true

            songLayout.setOnClickListener {
                activity.playNewPlayQueue(songs, layoutPosition - 1)
            }

            songLayout.setOnLongClickListener {
                activity.openDialog(SongOptions(songs[layoutPosition - 1]))
                return@setOnLongClickListener true
            }

            mMenu!!.setOnClickListener {
                activity.openDialog(SongOptions(songs[layoutPosition - 1]))
            }
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

                if (songs.isNotEmpty()){
                    ImageHandlingHelper.loadImageByAlbumId(activity.application,
                        songs[0].albumId, holder.mArtwork)
                    holder.mTitle.text = songs[0].albumName ?: activity.getString(R.string.default_album)
                    holder.mArtist.text = songs[0].artist ?: activity.getString(R.string.default_artist)
                }
                val songCountInt = songs.size
                holder.mSongCount.text = if (songCountInt == 1) activity.getString(R.string.displayed_song)
                else activity.getString(R.string.displayed_songs, songCountInt)
            }

            SONG -> {
                holder as ViewHolderSongWithDisc
                val current = songs[position -1]

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
        if (displayDiscNumbers && songs.size > 1) {
            if (position - 1 == 0 || songs[position -1].track.toString().substring(0, 1) !=
                songs[position -2].track.toString().substring(0, 1)) {
                return true
            }
        }
        return false
    }

    override fun removeSong(index: Int) {
        val discNumberShouldBeDisplayed = shouldDisplayDiscNumber(index)
        super.removeSong(index)
        if (discNumberShouldBeDisplayed) {
            notifyItemChanged(index)
        }
    }
}
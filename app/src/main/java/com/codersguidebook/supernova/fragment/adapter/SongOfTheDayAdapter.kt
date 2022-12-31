package com.codersguidebook.supernova.fragment.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.codersguidebook.supernova.MainActivity
import com.codersguidebook.supernova.R
import com.codersguidebook.supernova.utils.ImageHandlingHelper

class SongOfTheDayAdapter(private val activity: MainActivity): SongAdapter(activity) {

    inner class ViewHolderNoContent(itemView: View) : RecyclerView.ViewHolder(itemView)

    inner class ViewHolderSong(itemView: View) : SongAdapter.ViewHolderSong(itemView) {

        internal var mAlbum = itemView.findViewById<View>(R.id.subtitle2) as TextView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (songs.isEmpty()) ViewHolderNoContent(LayoutInflater.from(parent.context)
            .inflate(R.layout.song_of_the_day_no_content, parent, false))
        else ViewHolderSong(LayoutInflater.from(parent.context)
            .inflate(R.layout.large_home_song, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (songs.isEmpty()) return
        holder as ViewHolderSong

        val song = songs[position]

        ImageHandlingHelper.loadImageByAlbumId(activity.application, song.albumId, holder.mArtwork!!)
        holder.mTitle.text = song.title
        holder.mSubtitle.text = song.artist
        holder.mAlbum.text = song.albumName
    }

    override fun getItemCount(): Int {
        return if (songs.isEmpty()) 1
        else songs.size
    }
}
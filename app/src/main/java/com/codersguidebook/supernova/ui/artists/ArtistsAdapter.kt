package com.codersguidebook.supernova.ui.artists

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isGone
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.codersguidebook.supernova.*
import com.codersguidebook.supernova.entities.Artist
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView

class ArtistsAdapter(private val mainActivity: MainActivity):
    RecyclerView.Adapter<ArtistsAdapter.ArtistsViewHolder>(), FastScrollRecyclerView.SectionedAdapter {
    var artists = mutableListOf<Artist>()

    override fun getSectionName(position: Int): String {
        return artists[position].artistName?.get(0)?.toUpperCase().toString()
    }

    inner class ArtistsViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView),
        View.OnClickListener {

        internal var mArtwork = itemView.findViewById<View>(R.id.artwork) as ImageView
        internal var mTitle = itemView.findViewById<View>(R.id.title) as TextView
        internal var mSongCount = itemView.findViewById<View>(R.id.subtitle) as TextView
        internal var mBtnMenu = itemView.findViewById<ImageButton>(R.id.menu)

        init {
            itemView.isClickable = true
            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener{
                mainActivity.openDialog(ArtistOptions(artists[layoutPosition].artistName!!))
                return@setOnLongClickListener true
            }
        }

        override fun onClick(view: View) {
            val action = ArtistsFragmentDirections.actionSelectArtist(artists[layoutPosition].artistName!!)
            view.findNavController().navigate(action)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArtistsViewHolder {
        return ArtistsViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.preview, parent, false))
    }

    override fun onBindViewHolder(holder: ArtistsViewHolder, position: Int) {
        val current = artists[position]

        holder.mArtwork.isGone = true
        holder.mTitle.text = current.artistName

        // determine how to present songCount
        val songCountInt = current.songCount
        val songCount = if (songCountInt == 1) "$songCountInt song"
        else "$songCountInt songs"
        holder.mSongCount.text = songCount

        holder.mBtnMenu.setOnClickListener {
            mainActivity.openDialog(ArtistOptions(current.artistName!!))
        }
    }

    override fun getItemCount() = artists.size
}
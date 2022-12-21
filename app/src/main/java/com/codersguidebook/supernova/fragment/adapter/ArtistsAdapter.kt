package com.codersguidebook.supernova.fragment.adapter

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.codersguidebook.supernova.ArtistOptions
import com.codersguidebook.supernova.MainActivity
import com.codersguidebook.supernova.R
import com.codersguidebook.supernova.entities.Artist
import com.codersguidebook.supernova.ui.artists.ArtistsFragmentDirections
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView


class ArtistsAdapter(private val activity: MainActivity): RecyclerView.Adapter<RecyclerView.ViewHolder>(),
    FastScrollRecyclerView.SectionedAdapter {
    val artists = mutableListOf<Artist>()

    override fun getSectionName(position: Int): String {
        return artists[position].artistName?.get(0)?.uppercase() ?: ""
    }

    inner class ViewHolderArtist(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        internal var mTitle = itemView.findViewById<View>(R.id.title) as TextView
        internal var mSongCount = itemView.findViewById<View>(R.id.subtitle) as TextView
        private var mMenu = itemView.findViewById<ImageButton>(R.id.menu)

        init {
            val outValue = TypedValue()
            activity.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
            itemView.setBackgroundResource(outValue.resourceId)
            itemView.isClickable = true
            itemView.setOnClickListener {
                val action = ArtistsFragmentDirections.actionSelectArtist(artists[layoutPosition].artistName!!)
                it.findNavController().navigate(action)
            }

            itemView.setOnLongClickListener{
                artists[layoutPosition].artistName?.let { artistName ->
                    activity.openDialog(ArtistOptions(artistName))
                }
                return@setOnLongClickListener true
            }

            mMenu.setOnClickListener {
                artists[layoutPosition].artistName?.let { artistName ->
                    activity.openDialog(ArtistOptions(artistName))
                }
            }
        }
    }

    fun processLoopIteration(index: Int, artist: Artist) {
        when {
            index >= artists.size -> {
                artists.add(artist)
                notifyItemInserted(index)
            }
            artist.artistName != artists[index].artistName -> {
                var numberOfItemsRemoved = 0
                do {
                    artists.removeAt(index)
                    ++numberOfItemsRemoved
                } while (index < artists.size &&
                    artist.artistName != artists[index].artistName)

                when {
                    numberOfItemsRemoved == 1 -> notifyItemRemoved(index)
                    numberOfItemsRemoved > 1 -> notifyItemRangeRemoved(index, numberOfItemsRemoved)
                }

                processLoopIteration(index, artist)
            }
            artist.songCount != artists[index].songCount -> {
                artists[index] = artist
                notifyItemChanged(index)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ViewHolderArtist(
            LayoutInflater.from(parent.context).inflate(R.layout.item_with_menu, parent, false)
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        holder as ViewHolderArtist
        val current = artists[position]

        holder.mTitle.text = current.artistName

        val songCountInt = current.songCount
        holder.mSongCount.text = if (songCountInt == 1) activity.getString(R.string.displayed_song)
        else activity.getString(R.string.displayed_songs, songCountInt)
    }

    override fun getItemCount() = artists.size
}
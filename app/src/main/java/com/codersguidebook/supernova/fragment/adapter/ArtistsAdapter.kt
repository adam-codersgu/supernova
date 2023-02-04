package com.codersguidebook.supernova.fragment.adapter

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.codersguidebook.supernova.MainActivity
import com.codersguidebook.supernova.R
import com.codersguidebook.supernova.dialogs.ArtistOptions
import com.codersguidebook.supernova.entities.Artist
import com.codersguidebook.supernova.ui.artists.ArtistsFragmentDirections
import com.codersguidebook.supernova.views.RecyclerViewScrollbar

class ArtistsAdapter(private val activity: MainActivity): RecyclerView.Adapter<RecyclerView.ViewHolder>(),
    RecyclerViewScrollbar.ValueLabelListener {

    val artists = mutableListOf<Artist>()

    override fun getValueLabelText(position: Int): String {
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

    /**
     * Handle updates to the content of the RecyclerView. The below method will determine what
     * changes are required when an element/elements is/are changed, inserted, or deleted.
     * This enhanced process loop iteration method assumes each artist can only appear once.
     *
     * @param newArtists The new list of Artist objects that should be displayed.
     */
    fun processNewArtists(newArtists: List<Artist>) {
        for ((index, artist) in newArtists.withIndex()) {
            when {
                index >= artists.size -> {
                    artists.add(artist)
                    notifyItemInserted(index)
                }
                artist.artistName != artists[index].artistName -> {
                    // Check if the artist is a new entry to the list
                    val artistIsNewEntry = artists.find { it.artistName == artist.artistName } == null
                    if (artistIsNewEntry) {
                        artists.add(index, artist)
                        notifyItemInserted(index)
                        continue
                    }

                    // Check if artist(s) has/have been removed from the list
                    val artistIsRemoved = newArtists.find { it.artistName == artists[index].artistName } == null
                    if (artistIsRemoved) {
                        var numberOfItemsRemoved = 0
                        do {
                            artists.removeAt(index)
                            ++numberOfItemsRemoved
                        } while (index < artists.size &&
                            newArtists.find { it.artistName == artists[index].artistName } == null)

                        when {
                            numberOfItemsRemoved == 1 -> notifyItemRemoved(index)
                            numberOfItemsRemoved > 1 -> notifyItemRangeRemoved(index,
                                numberOfItemsRemoved)
                        }

                        // Check if removing the artist(s) has fixed the list
                        if (artist.artistName == artists[index].artistName) continue
                    }

                    // Check if the artist has been moved earlier in the list
                    val oldIndex = artists.indexOfFirst { it.artistName == artist.artistName }
                    if (oldIndex != -1 && oldIndex > index) {
                        artists.removeAt(oldIndex)
                        artists.add(index, artist)
                        notifyItemMoved(oldIndex, index)
                        continue
                    }

                    // Check if the artist(s) has been moved later in the list
                    var newIndex = newArtists.indexOfFirst { it.artistName == artists[index].artistName }
                    if (newIndex != -1) {
                        do {
                            artists.removeAt(index)

                            if (newIndex <= artists.size) {
                                artists.add(newIndex, artist)
                                notifyItemMoved(index, newIndex)
                            } else {
                                notifyItemRemoved(index)
                            }

                            // See if further artists need to be moved
                            newIndex = newArtists.indexOfFirst { it.artistName == artists[index].artistName }
                        } while (index < artists.size &&
                            artist.artistName != artists[index].artistName &&
                            newIndex != -1)

                        // Check if moving the artist(s) has fixed the list
                        if (artist.artistName == artists[index].artistName) continue
                        else {
                            artists.add(index, artist)
                            notifyItemInserted(index)
                        }
                    }
                }
                artist.songCount != artists[index].songCount -> {
                    artists[index] = artist
                    notifyItemChanged(index)
                }
            }
        }

        if (artists.size > newArtists.size) {
            val numberItemsToRemove = artists.size - newArtists.size
            repeat(numberItemsToRemove) { artists.removeLast() }
            notifyItemRangeRemoved(newArtists.size, numberItemsToRemove)
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

        holder.mTitle.text = current.artistName ?: activity.getString(R.string.default_artist)

        val songCountInt = current.songCount
        holder.mSongCount.text = if (songCountInt == 1) activity.getString(R.string.displayed_song)
        else activity.getString(R.string.displayed_songs, songCountInt)
    }

    override fun getItemCount() = artists.size
}
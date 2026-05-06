package com.codersguidebook.supernova.fragment.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.codersguidebook.recyclerviewfastscroller.RecyclerViewScrollbar
import com.codersguidebook.supernova.MainActivity
import com.codersguidebook.supernova.R
import com.codersguidebook.supernova.dialogs.ArtistOptions
import com.codersguidebook.supernova.entities.Artist
import com.codersguidebook.supernova.fragment.BaseDialogFragment
import com.codersguidebook.supernova.fragment.adapter.viewholder.ViewHolderWithMenu
import com.codersguidebook.supernova.ui.artists.ArtistsFragmentDirections

class ArtistsAdapter(private val activity: MainActivity): Adapter(),
    RecyclerViewScrollbar.ValueLabelListener {

    override fun getValueLabelText(position: Int): String {
        return (items[position] as Artist).artistName?.get(0)?.uppercase() ?: ""
    }

    inner class ViewHolderArtist(itemView: View) : ViewHolderWithMenu(itemView) {

        override fun getActivity(): MainActivity {
            return activity
        }

        override fun rootViewAction() {
            val action = ArtistsFragmentDirections.actionSelectArtist((items[layoutPosition] as Artist).artistName!!)
            itemView.findNavController().navigate(action)
        }

        override fun getOptionsDialog(): BaseDialogFragment {
            return ArtistOptions((items[layoutPosition] as Artist).artistName!!)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ViewHolderArtist(
            LayoutInflater.from(parent.context).inflate(R.layout.item_with_menu, parent, false)
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        holder as ViewHolderArtist
        val current = (items[position] as Artist)

        holder.mTitle.text = current.artistName ?: activity.getString(R.string.default_artist)

        val songCount = current.songCount
        holder.mSubtitle.text = if (songCount == 1) activity.getString(R.string.displayed_song)
        else activity.getString(R.string.displayed_songs, songCount)
    }

    override fun itemsEqual(item1: Any, item2: Any): Boolean {
        return (item1 as Artist).artistName == (item2 as Artist).artistName
    }

    override fun itemShouldBeUpdated(item: Any, index: Int): Boolean {
        return (item as Artist).songCount != (items[index] as Artist).songCount
    }
}
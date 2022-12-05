package com.codersguidebook.supernova.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.codersguidebook.supernova.MainActivity
import com.codersguidebook.supernova.databinding.FragmentWithFabBinding
import com.codersguidebook.supernova.entities.Song

abstract class RecyclerViewWithFabFragment: RecyclerViewFragment() {

    var fragmentBinding: FragmentWithFabBinding? = null
    val binding get() = fragmentBinding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        fragmentBinding = FragmentWithFabBinding.inflate(inflater, container, false)
        mainActivity = activity as MainActivity
        return binding.root
    }

    override fun updateRecyclerView(songs: List<Song>) {
        super.updateRecyclerView(songs)

        setupMenu(songs)

        binding.fab.setOnClickListener {
            mainActivity.playNewPlayQueue(songs, shuffle = true)
        }

        val discNumbers = songs.distinctBy {
            it.track.toString().substring(0, 1).toInt()
        }.map { it.track.toString().substring(0, 1).toInt() }

        if (adapter.songs.isEmpty()) {
            adapter.displayDiscNumbers = discNumbers.size > 1
            adapter.songs.addAll(songs)
            adapter.notifyItemRangeInserted(0, songs.size)
        } else {
            for ((index, song) in songs.withIndex()) {
                processLoopIteration(index, song)
            }

            if (albumAdapter.songs.size > songs.size) {
                val numberItemsToRemove = albumAdapter.songs.size - songs.size
                repeat(numberItemsToRemove) { albumAdapter.songs.removeLast() }
                albumAdapter.notifyItemRangeRemoved(songs.size, numberItemsToRemove)
            }
        }

        isUpdating = false
        if (unhandledRequestReceived) {
            unhandledRequestReceived = false
            requestNewData()
            musicDatabase.musicDao().findAlbumSongs(albumId ?: return).value?.let {
                processNewSongs(it)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        fragmentBinding = null
    }
}
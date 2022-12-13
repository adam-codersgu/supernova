package com.codersguidebook.supernova.ui.playlists

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import com.codersguidebook.supernova.MusicLibraryViewModel
import com.codersguidebook.supernova.databinding.FragmentWithRecyclerViewBinding
import com.codersguidebook.supernova.entities.Playlist
import com.codersguidebook.supernova.recyclerview.RecyclerViewFragment
import com.codersguidebook.supernova.recyclerview.adapter.PlaylistsAdapter

class PlaylistsFragment : RecyclerViewFragment() {

    override val binding get() = fragmentBinding as FragmentWithRecyclerViewBinding
    override lateinit var adapter: PlaylistsAdapter
    private lateinit var musicLibraryViewModel: MusicLibraryViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        fragmentBinding = FragmentWithRecyclerViewBinding.inflate(inflater, container, false)
        musicLibraryViewModel = ViewModelProvider(this)[MusicLibraryViewModel::class.java]

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.root.layoutManager = GridLayoutManager(mainActivity, 3)
        binding.root.itemAnimator = DefaultItemAnimator()
        binding.root.adapter = adapter

        musicLibraryViewModel.allPlaylists.observe(viewLifecycleOwner) {
            updateRecyclerViewWithPlaylists(it)
        }
    }

    override fun initialiseAdapter() {
        adapter = PlaylistsAdapter(mainActivity)
    }

    override fun requestNewData() {
        musicLibraryViewModel.allPlaylists.value?.let { updateRecyclerViewWithPlaylists(it) }
    }

    private fun updateRecyclerViewWithPlaylists(playlists: List<Playlist>) {
        setIsUpdatingTrue()

        val playlistsToDisplay = playlists.toMutableList().apply {
            removeIf { playlist ->
                playlist.isDefault && playlist.songs.isNullOrBlank()
            }
            sortBy { playlist ->
                playlist.name.uppercase()
            }
        }

        if (adapter.playlists.isEmpty()) {
            adapter.playlists.addAll(playlistsToDisplay)
            adapter.notifyItemRangeInserted(0, playlistsToDisplay.size)
        } else {
            for ((index, playlist) in playlistsToDisplay.withIndex()) {
                adapter.processLoopIteration(index, playlist)
            }

            if (adapter.playlists.size > playlistsToDisplay.size) {
                val numberItemsToRemove = adapter.playlists.size - playlistsToDisplay.size
                repeat(numberItemsToRemove) { adapter.playlists.removeLast() }
                adapter.notifyItemRangeRemoved(playlistsToDisplay.size, numberItemsToRemove)
            }
        }

        setIsUpdatingFalse()
    }
}

package com.codersguidebook.supernova.ui.playlists

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import com.codersguidebook.supernova.entities.Playlist
import com.codersguidebook.supernova.fragment.RecyclerViewFragment
import com.codersguidebook.supernova.fragment.adapter.PlaylistsAdapter

class PlaylistsFragment : RecyclerViewFragment() {

    override lateinit var adapter: PlaylistsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.root.layoutManager = GridLayoutManager(mainActivity, 3)

        musicLibraryViewModel.allPlaylists.observe(viewLifecycleOwner) {
            updateRecyclerView(it)
        }
    }

    override fun initialiseAdapter() {
        adapter = PlaylistsAdapter(mainActivity)
    }

    override fun requestNewData() {
        musicLibraryViewModel.allPlaylists.value?.let { updateRecyclerView(it) }
    }

    /**
     * Refresh the content displayed in the RecyclerView.
     *
     * @param playlists The up-to-date list of Playlist objects that should be displayed.
     */
    private fun updateRecyclerView(playlists: List<Playlist>) {
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
            adapter.processNewPlaylists(playlistsToDisplay)
        }

        finishUpdate()
    }
}

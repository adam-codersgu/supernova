package com.codersguidebook.supernova.ui.artists

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.codersguidebook.supernova.entities.Artist
import com.codersguidebook.supernova.fragment.RecyclerViewFragment
import com.codersguidebook.supernova.fragment.adapter.ArtistsAdapter

class ArtistsFragment : RecyclerViewFragment() {

    override lateinit var adapter: ArtistsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        musicLibraryViewModel.allArtists.observe(viewLifecycleOwner) {
            updateRecyclerViewWithArtists(it)
        }
    }

    override fun requestNewData() {
        musicLibraryViewModel.allArtists.value?.let { updateRecyclerViewWithArtists(it) }
    }

    /**
     * Refresh the content displayed in the RecyclerView.
     *
     * @param artists - The up-to-date list of Artist objects that should be displayed.
     */
    private fun updateRecyclerViewWithArtists(artists: List<Artist>) {
        setIsUpdatingTrue()

        val sortedArtists = artists.sortedBy { artist ->
            artist.artistName?.uppercase()
        }.toMutableList()

        if (adapter.artists.isEmpty()) {
            adapter.artists.addAll(sortedArtists)
            adapter.notifyItemRangeInserted(0, sortedArtists.size)
        } else {
            for ((index, artist) in sortedArtists.withIndex()) {
                adapter.processLoopIteration(index, artist)
            }

            if (adapter.artists.size > sortedArtists.size) {
                val numberItemsToRemove = adapter.artists.size - sortedArtists.size
                repeat(numberItemsToRemove) { adapter.artists.removeLast() }
                adapter.notifyItemRangeRemoved(sortedArtists.size, numberItemsToRemove)
            }
        }

        finishUpdate()
    }

    override fun initialiseAdapter() {
        adapter = ArtistsAdapter(mainActivity)
        adapter.stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }
}

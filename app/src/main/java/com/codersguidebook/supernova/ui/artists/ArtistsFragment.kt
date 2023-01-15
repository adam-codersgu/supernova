package com.codersguidebook.supernova.ui.artists

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.codersguidebook.supernova.MainActivity
import com.codersguidebook.supernova.entities.Artist
import com.codersguidebook.supernova.fragment.RecyclerViewWithScrollFragment
import com.codersguidebook.supernova.fragment.adapter.ArtistsAdapter

class ArtistsFragment : RecyclerViewWithScrollFragment() {

    override lateinit var adapter: ArtistsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // super.onViewCreated(view, savedInstanceState)

        musicLibraryViewModel.allArtists.observe(viewLifecycleOwner) {
            updateRecyclerView(it)
        }
    }

    override fun requestNewData() {
        musicLibraryViewModel.allArtists.value?.let { updateRecyclerView(it) }
    }

    /**
     * Refresh the content displayed in the RecyclerView.
     *
     * @param artists The up-to-date list of Artist objects that should be displayed.
     */
    private fun updateRecyclerView(artists: List<Artist>) {
        setIsUpdatingTrue()

        val sortedArtists = artists.sortedBy { artist ->
            artist.artistName?.uppercase()
        }.toMutableList()

        if (adapter.artists.isEmpty()) {
            adapter.artists.addAll(sortedArtists)
            adapter.notifyItemRangeInserted(0, sortedArtists.size)
        } else {
            adapter.processNewArtists(sortedArtists)
        }

        finishUpdate()
    }

    override fun initialiseAdapter() {
        // fixme: moving away from this method for fastscroll
        mainActivity = activity as MainActivity
        adapter = ArtistsAdapter(mainActivity)
        adapter.stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }
}

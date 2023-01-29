package com.codersguidebook.supernova.ui.albums

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.codersguidebook.supernova.entities.Song
import com.codersguidebook.supernova.fragment.RecyclerViewWithScrollFragment
import com.codersguidebook.supernova.fragment.adapter.AlbumsAdapter

class AlbumsFragment : RecyclerViewWithScrollFragment() {

    override lateinit var adapter: AlbumsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        musicLibraryViewModel.allSongs.observe(viewLifecycleOwner) {
            updateRecyclerView(it)
        }
    }

    override fun requestNewData() {
        musicLibraryViewModel.allSongs.value?.let { updateRecyclerView(it) }
    }

    /**
     * Refresh the content displayed in the RecyclerView.
     *
     * @param songs The up-to-date list of Song objects that should be displayed.
     */
    private fun updateRecyclerView(songs: List<Song>) {
        setIsUpdatingTrue()

        adapter.processAlbumsBySongs(songs)

        finishUpdate()
    }

    override fun initialiseAdapter() {
        adapter = AlbumsAdapter(mainActivity)
        adapter.stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }
}

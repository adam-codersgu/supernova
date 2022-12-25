package com.codersguidebook.supernova.ui.albums

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.codersguidebook.supernova.entities.Song
import com.codersguidebook.supernova.fragment.RecyclerViewFragment
import com.codersguidebook.supernova.fragment.adapter.AlbumsAdapter

class AlbumsFragment : RecyclerViewFragment() {

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
     * @param songs - The up-to-date list of Song objects that should be displayed.
     */
    private fun updateRecyclerView(songs: List<Song>) {
        setIsUpdatingTrue()

        val songsByAlbum = songs.distinctBy { song ->
            song.albumId
        }.sortedBy { song ->
            song.albumName.uppercase()
        }.toMutableList()

        if (adapter.songsByAlbum.isEmpty()) {
            adapter.songsByAlbum.addAll(songsByAlbum)
            adapter.notifyItemRangeInserted(0, songsByAlbum.size)
        } else {
            for ((index, album) in songsByAlbum.withIndex()) {
                adapter.processLoopIteration(index, album)
            }

            if (adapter.songsByAlbum.size > songsByAlbum.size) {
                val numberItemsToRemove = adapter.songsByAlbum.size - songsByAlbum.size
                repeat(numberItemsToRemove) { adapter.songsByAlbum.removeLast() }
                adapter.notifyItemRangeRemoved(songsByAlbum.size, numberItemsToRemove)
            }
        }

        finishUpdate()
    }

    override fun initialiseAdapter() {
        adapter = AlbumsAdapter(mainActivity)
        adapter.stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }
}

package com.codersguidebook.supernova.ui.albums

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.codersguidebook.supernova.MusicLibraryViewModel
import com.codersguidebook.supernova.databinding.ScrollRecyclerViewBinding
import com.codersguidebook.supernova.entities.Song
import com.codersguidebook.supernova.recyclerview.RecyclerViewFragment
import com.codersguidebook.supernova.recyclerview.adapter.AlbumsAdapter

class AlbumsFragment : RecyclerViewFragment() {

    override val binding get() = fragmentBinding as ScrollRecyclerViewBinding
    override lateinit var adapter: AlbumsAdapter
    private lateinit var musicLibraryViewModel: MusicLibraryViewModel

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        fragmentBinding = ScrollRecyclerViewBinding.inflate(inflater, container, false)
        musicLibraryViewModel = ViewModelProvider(this)[MusicLibraryViewModel::class.java]

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val layoutManager = LinearLayoutManager(activity)
        binding.recyclerView.layoutManager = layoutManager
        binding.recyclerView.itemAnimator = DefaultItemAnimator()
        binding.recyclerView.adapter = adapter

        musicLibraryViewModel.allSongs.observe(viewLifecycleOwner) {
            updateRecyclerView(it)
        }
    }

    override fun requestNewData() {
        musicLibraryViewModel.allSongs.value?.let { updateRecyclerView(it) }
    }

    override fun updateRecyclerView(songs: List<Song>) {
        super.updateRecyclerView(songs)

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

        setIsUpdatingFalse()
    }

    override fun initialiseAdapter() {
        adapter = AlbumsAdapter(mainActivity)
        adapter.stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }
}

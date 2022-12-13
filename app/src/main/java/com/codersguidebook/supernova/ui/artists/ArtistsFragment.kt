package com.codersguidebook.supernova.ui.artists

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
import com.codersguidebook.supernova.entities.Artist
import com.codersguidebook.supernova.recyclerview.RecyclerViewFragment
import com.codersguidebook.supernova.recyclerview.adapter.ArtistsAdapter

class ArtistsFragment : RecyclerViewFragment() {

    override val binding get() = fragmentBinding as ScrollRecyclerViewBinding
    override lateinit var adapter: ArtistsAdapter
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

        musicLibraryViewModel.allArtists.observe(viewLifecycleOwner) {
            updateRecyclerViewWithArtists(it)
        }
    }

    override fun requestNewData() {
        musicLibraryViewModel.allArtists.value?.let { updateRecyclerViewWithArtists(it) }
    }

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

        setIsUpdatingFalse()
    }

    override fun initialiseAdapter() {
        adapter = ArtistsAdapter(mainActivity)
        adapter.stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }
}

package com.codersguidebook.supernova.ui.songs

import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.codersguidebook.supernova.MusicLibraryViewModel
import com.codersguidebook.supernova.fragment.RecyclerViewWithFabFragment
import com.codersguidebook.supernova.fragment.adapter.SongsAdapter

class SongsFragment : RecyclerViewWithFabFragment() {

    private lateinit var musicLibraryViewModel: MusicLibraryViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        musicLibraryViewModel = ViewModelProvider(this)[MusicLibraryViewModel::class.java]
        musicLibraryViewModel.allSongs.observe(viewLifecycleOwner) {
            updateRecyclerView(it)
        }
    }

    override fun initialiseAdapter() {
        adapter = SongsAdapter(mainActivity)
        adapter.stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }

    override fun requestNewData() {
        musicLibraryViewModel.allSongs.value?.let { updateRecyclerView(it) }
    }
}

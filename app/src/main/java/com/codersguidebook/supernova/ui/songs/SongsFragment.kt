package com.codersguidebook.supernova.ui.songs

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.codersguidebook.supernova.fragment.RecyclerViewWithFabFragment
import com.codersguidebook.supernova.fragment.adapter.SongsAdapter

class SongsFragment : RecyclerViewWithFabFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        musicLibraryViewModel.allSongs.observe(viewLifecycleOwner) {
            updateRecyclerViewV2(it)
        }
    }

    override fun initialiseAdapter() {
        adapter = SongsAdapter(mainActivity)
        adapter.stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }

    override fun requestNewData() {
        musicLibraryViewModel.allSongs.value?.let { updateRecyclerViewV2(it) }
    }
}

package com.codersguidebook.supernova.ui.artist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.codersguidebook.supernova.R
import com.codersguidebook.supernova.fragment.RecyclerViewWithFabFragment
import com.codersguidebook.supernova.fragment.adapter.SongsAdapter

class ArtistSongsFragment : RecyclerViewWithFabFragment() {

    private var artistName = getString(R.string.default_artist)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        arguments?.let { arguments ->
            EditArtistFragmentArgs.fromBundle(arguments).artist?.let { artistName = it }
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        musicLibraryViewModel.setActiveArtistName(artistName)

        musicLibraryViewModel.activeArtistSongs.observe(viewLifecycleOwner) { songs ->
            updateRecyclerView(songs)
        }
    }

    override fun initialiseAdapter() {
        adapter = SongsAdapter(mainActivity)
    }

    override fun requestNewData() {
        musicLibraryViewModel.activeArtistSongs.value?.let { updateRecyclerView(it) }
    }
}
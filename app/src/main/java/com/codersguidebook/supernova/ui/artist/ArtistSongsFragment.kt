package com.codersguidebook.supernova.ui.artist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.codersguidebook.supernova.MusicDatabase
import com.codersguidebook.supernova.fragment.RecyclerViewWithFabFragment
import com.codersguidebook.supernova.fragment.adapter.SongsAdapter

class ArtistSongsFragment : RecyclerViewWithFabFragment() {

    private var artistName: String? = null
    private lateinit var musicDatabase: MusicDatabase

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        arguments?.let {
            val safeArgs = ArtistSongsFragmentArgs.fromBundle(it)
            artistName = safeArgs.artist
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        artistName?.let { artistName ->
            musicDatabase = MusicDatabase.getDatabase(mainActivity, lifecycleScope)
            musicDatabase.musicDao().findArtistsSongs(artistName).observe(viewLifecycleOwner) {
                updateRecyclerView(it)
            }
        }
    }

    override fun initialiseAdapter() {
        adapter = SongsAdapter(mainActivity)
    }

    override fun requestNewData() {
        musicDatabase.musicDao().findArtistsSongs(artistName ?: return).value?.let {
            updateRecyclerView(it)
        }
    }
}
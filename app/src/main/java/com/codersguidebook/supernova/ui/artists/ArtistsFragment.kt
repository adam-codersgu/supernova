package com.codersguidebook.supernova.ui.artists

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.codersguidebook.supernova.*
import com.codersguidebook.supernova.entities.Artist
import java.util.*

class ArtistsFragment : Fragment() {

    private var artists = mutableListOf<Artist>()
    private var isProcessing = false
    private lateinit var artistsAdapter: ArtistsAdapter
    private lateinit var callingActivity: MainActivity
    private lateinit var musicViewModel: MusicViewModel
    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_with_scroll, container, false)
        callingActivity = activity as MainActivity
        recyclerView = root.findViewById(R.id.scrollRecyclerView)
        artistsAdapter = ArtistsAdapter(callingActivity)
        recyclerView.layoutManager = WrapContentLinearLayoutManager(callingActivity, LinearLayoutManager.VERTICAL, false)
        recyclerView.itemAnimator = DefaultItemAnimator()
        recyclerView.adapter = artistsAdapter
        artistsAdapter.stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY

        musicViewModel = ViewModelProvider(this).get(MusicViewModel::class.java)
        musicViewModel.allArtists.observe(viewLifecycleOwner, { a ->
            a?.let {
                if (it.isNotEmpty() || artists.isNotEmpty()) processArtists(it)
            }
        })

        return root
    }

    private fun processArtists(artistList: List<Artist>) {
        // use the isProcessing boolean to prevent the processArtists method from being run multiple times in quick succession (e.g. when the library is being built for the first time)
        if (!isProcessing) {
            isProcessing = true
            artists = artistList.sortedBy { artist ->
                artist.artistName?.toUpperCase(Locale.ROOT)
            }.toMutableList()

            val adapterArtists = artistsAdapter.artists
            artistsAdapter.artists = artists
            when {
                adapterArtists.isEmpty() -> artistsAdapter.notifyItemRangeInserted(0, artists.size) // updateAdapter(SET_ADAPTER, artists.size)
                artists.size >= adapterArtists.size -> {
                    val difference = artists - adapterArtists
                    for (a in difference) {
                        val index = artists.indexOfFirst {
                            it.artistName == a.artistName
                        }
                        val adapterIndex = adapterArtists.indexOfFirst {
                            it.artistName == a.artistName
                        }
                        when {
                            adapterIndex != -1 -> artistsAdapter.notifyItemChanged(index)
                            index != -1 -> artistsAdapter.notifyItemInserted(index)
                        }
                    }
                }
                artists.size < adapterArtists.size -> {
                    val difference = adapterArtists - artists
                    for (a in difference) {
                        val index = adapterArtists.indexOfFirst {
                            it.artistName == a.artistName
                        }
                        adapterArtists.removeAt(index)
                        if (index != -1) artistsAdapter.notifyItemRemoved(index)
                    }
                }
            }
            isProcessing = false
        }
    }
}

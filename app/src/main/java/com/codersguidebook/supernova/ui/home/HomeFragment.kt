package com.codersguidebook.supernova.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
import androidx.viewbinding.ViewBinding
import com.codersguidebook.supernova.R
import com.codersguidebook.supernova.databinding.FragmentHomeBinding
import com.codersguidebook.supernova.entities.Playlist
import com.codersguidebook.supernova.fragment.BaseFragment
import com.codersguidebook.supernova.fragment.adapter.HomeAdapter
import com.codersguidebook.supernova.fragment.adapter.MostPlayedAdapter
import com.codersguidebook.supernova.fragment.adapter.SongAdapter
import com.codersguidebook.supernova.fragment.adapter.SongOfTheDayAdapter
import com.codersguidebook.supernova.fragment.layoutmanager.WrapContentLinearLayoutManager
import com.codersguidebook.supernova.ui.playlists.PlaylistsFragmentDirections
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : BaseFragment() {

    override var _binding: ViewBinding? = null
        get() = field as FragmentHomeBinding?
    override val binding: FragmentHomeBinding
        get() = _binding!! as FragmentHomeBinding
    private var isUpdating = false
    private var unhandledRequestReceived = false
    private lateinit var songOfTheDayAdapter: SongOfTheDayAdapter
    private lateinit var favouritesAdapter: HomeAdapter
    private lateinit var mostPlayedAdapter: MostPlayedAdapter
    private lateinit var recentlyPlayedAdapter: HomeAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        songOfTheDayAdapter = SongOfTheDayAdapter(mainActivity)
        favouritesAdapter = HomeAdapter(mainActivity)
        mostPlayedAdapter = MostPlayedAdapter(mainActivity)
        recentlyPlayedAdapter = HomeAdapter(mainActivity)

        songOfTheDayAdapter.stateRestorationPolicy = PREVENT_WHEN_EMPTY
        favouritesAdapter.stateRestorationPolicy = PREVENT_WHEN_EMPTY
        mostPlayedAdapter.stateRestorationPolicy = PREVENT_WHEN_EMPTY
        recentlyPlayedAdapter.stateRestorationPolicy = PREVENT_WHEN_EMPTY

        binding.songOfTheDayRecyclerView.layoutManager = WrapContentLinearLayoutManager(mainActivity, LinearLayoutManager.HORIZONTAL, false)
        binding.songOfTheDayRecyclerView.itemAnimator = DefaultItemAnimator()
        binding.songOfTheDayRecyclerView.adapter = songOfTheDayAdapter

        binding.favouritesRecyclerView.layoutManager = WrapContentLinearLayoutManager(mainActivity, LinearLayoutManager.HORIZONTAL, false)
        binding.favouritesRecyclerView.itemAnimator = DefaultItemAnimator()
        binding.favouritesRecyclerView.adapter = favouritesAdapter

        binding.mostPlayedRecyclerView.layoutManager = WrapContentLinearLayoutManager(mainActivity, LinearLayoutManager.HORIZONTAL, false)
        binding.mostPlayedRecyclerView.itemAnimator = DefaultItemAnimator()
        binding.mostPlayedRecyclerView.adapter = mostPlayedAdapter

        binding.recentlyPlayedRecyclerView.layoutManager = WrapContentLinearLayoutManager(mainActivity, LinearLayoutManager.HORIZONTAL, false)
        binding.recentlyPlayedRecyclerView.itemAnimator = DefaultItemAnimator()
        binding.recentlyPlayedRecyclerView.adapter = recentlyPlayedAdapter

        binding.refreshSongOfTheDay.setOnClickListener {
            musicLibraryViewModel.refreshSongOfTheDay(true)
        }

        binding.textViewSongOfTheDay.setOnClickListener {
            val action = PlaylistsFragmentDirections.actionSelectPlaylist(getString(R.string.song_day))
            findNavController().navigate(action)
        }

        binding.textViewMostPlayed.setOnClickListener {
            val action = PlaylistsFragmentDirections.actionSelectPlaylist(getString(R.string.most_played))
            findNavController().navigate(action)
        }

        binding.textViewFavourites.setOnClickListener {
            val action = PlaylistsFragmentDirections.actionSelectPlaylist(getString(R.string.favourites))
            findNavController().navigate(action)
        }

        binding.textViewRecentlyPlayed.setOnClickListener {
            val action = PlaylistsFragmentDirections.actionSelectPlaylist(getString(R.string.recently_played))
            findNavController().navigate(action)
        }

        musicLibraryViewModel.allPlaylists.observe(viewLifecycleOwner) { playlists ->
            extractPlaylists(playlists)
        }
    }

    private fun extractPlaylists(playlists: List<Playlist>) = lifecycleScope.launch(Dispatchers.Default) {
        if (isUpdating) {
            unhandledRequestReceived = true
            return@launch
        }
        isUpdating = true

        playlists.find { it.name == getString(R.string.song_day) }?.let {
            processPlaylist(it, songOfTheDayAdapter, binding.homeSongOfTheDay)
        }

        playlists.find { it.name == getString(R.string.favourites) }?.let {
            processPlaylist(it, favouritesAdapter, binding.homeFavourites)
        }

        playlists.find { it.name == getString(R.string.most_played) }?.let {
            processPlaylist(it, mostPlayedAdapter, binding.homeMostPlayed)
        }

        playlists.find { it.name == getString(R.string.recently_played) }?.let {
            processPlaylist(it, recentlyPlayedAdapter, binding.homeRecentlyPlayed)
        }

        isUpdating = false
        if (unhandledRequestReceived) {
            unhandledRequestReceived = false
            requestNewData()
        }
    }

    private fun requestNewData() {
        musicLibraryViewModel.allPlaylists.value?.let { extractPlaylists(it) }
    }

    private suspend fun processPlaylist(playlist: Playlist, adapter: SongAdapter, layout: RelativeLayout)
        = lifecycleScope.launch(Dispatchers.Main) {
        val songs = withContext(Dispatchers.IO) {
            musicLibraryViewModel.extractPlaylistSongs(playlist.songs)
        }

        val songsSelection = if (playlist.name == getString(R.string.favourites)) {
            songs.asReversed().take(10)
        } else songs.take(10)

        if (songsSelection.isEmpty()) layout.isGone = true
        else layout.isVisible = true

        if (adapter.songs.isEmpty()) {
            adapter.songs.addAll(songsSelection)
            if (songsSelection.isNotEmpty() && playlist.name == getString(R.string.song_day)) {
                adapter.notifyItemRemoved(0)
            }
            adapter.notifyItemRangeInserted(0, songsSelection.size)
        } else {
            for ((index, song) in songsSelection.withIndex()) {
                adapter.processLoopIteration(index, song)
            }

            if (adapter.songs.size > songsSelection.size) {
                val numberItemsToRemove = adapter.songs.size - songsSelection.size
                repeat(numberItemsToRemove) { adapter.songs.removeLast() }
                adapter.notifyItemRangeRemoved(adapter.getRecyclerViewIndex(songs.size), numberItemsToRemove)
            }
        }
    }
}
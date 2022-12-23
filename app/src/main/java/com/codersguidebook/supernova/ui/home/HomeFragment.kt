package com.codersguidebook.supernova.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
import com.codersguidebook.supernova.MainActivity
import com.codersguidebook.supernova.MusicLibraryViewModel
import com.codersguidebook.supernova.R
import com.codersguidebook.supernova.databinding.FragmentHomeBinding
import com.codersguidebook.supernova.fragment.layoutmanager.WrapContentLinearLayoutManager
import com.codersguidebook.supernova.ui.playlists.PlaylistsFragmentDirections
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var callingActivity: MainActivity
    private lateinit var songOfTheDayAdapter: SongOfTheDayAdapter
    private lateinit var favouritesAdapter: FavouritesAdapter
    private lateinit var mostPlayedAdapter: MostPlayedAdapter
    private lateinit var recentlyPlayedAdapter: RecentlyPlayedAdapter
    private lateinit var musicLibraryViewModel: MusicLibraryViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        callingActivity = activity as MainActivity
        musicLibraryViewModel = ViewModelProvider(this)[MusicLibraryViewModel::class.java]

        songOfTheDayAdapter = SongOfTheDayAdapter(callingActivity)
        // todo: can you create multiple instances of the same adapter actually for these bottom 3?
        favouritesAdapter = FavouritesAdapter(callingActivity)
        mostPlayedAdapter = MostPlayedAdapter(callingActivity)
        recentlyPlayedAdapter = RecentlyPlayedAdapter(callingActivity)

        songOfTheDayAdapter.stateRestorationPolicy = PREVENT_WHEN_EMPTY
        favouritesAdapter.stateRestorationPolicy = PREVENT_WHEN_EMPTY
        mostPlayedAdapter.stateRestorationPolicy = PREVENT_WHEN_EMPTY
        recentlyPlayedAdapter.stateRestorationPolicy = PREVENT_WHEN_EMPTY

        binding.refreshSongOfTheDay.setOnClickListener {
            callingActivity.refreshSongOfTheDay(true)
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.songOfTheDayRecyclerView.layoutManager = WrapContentLinearLayoutManager(callingActivity, LinearLayoutManager.HORIZONTAL, false)
        binding.songOfTheDayRecyclerView.itemAnimator = DefaultItemAnimator()
        binding.songOfTheDayRecyclerView.adapter = songOfTheDayAdapter

        binding.favouritesRecyclerView.layoutManager = WrapContentLinearLayoutManager(callingActivity, LinearLayoutManager.HORIZONTAL, false)
        binding.favouritesRecyclerView.itemAnimator = DefaultItemAnimator()
        binding.favouritesRecyclerView.adapter = favouritesAdapter

        binding.mostPlayedRecyclerView.layoutManager = WrapContentLinearLayoutManager(callingActivity, LinearLayoutManager.HORIZONTAL, false)
        binding.mostPlayedRecyclerView.itemAnimator = DefaultItemAnimator()
        binding.mostPlayedRecyclerView.adapter = mostPlayedAdapter

        binding.recentlyPlayedRecyclerView.layoutManager = WrapContentLinearLayoutManager(callingActivity, LinearLayoutManager.HORIZONTAL, false)
        binding.recentlyPlayedRecyclerView.itemAnimator = DefaultItemAnimator()
        binding.recentlyPlayedRecyclerView.adapter = recentlyPlayedAdapter

        // TODO: Find a better way of updating the playlists in this fragment that is not so dependent on other classes e.g. MainActivity and the view model loading data
        var isLoaded = false
        musicLibraryViewModel.allSongs.observe(viewLifecycleOwner) {
            if (it.isNotEmpty()) {
                if (!isLoaded) {
                    loadPlaylists()
                    isLoaded = true
                }

                if (binding.songOfTheDayNoContent.isGone) {
                    callingActivity.refreshSongOfTheDay()
                }
            }
        }
    }

    private fun loadPlaylists() {
        musicLibraryViewModel.getPlaylistByNameLiveData(getString(R.string.song_day)).observe(viewLifecycleOwner) { playlist ->
            playlist?.let {
                lifecycleScope.launch(Dispatchers.Main) {
                    val songs = withContext(Dispatchers.IO) {
                        musicLibraryViewModel.extractPlaylistSongs(it.songs)
                    }
                    if (songs.isEmpty()) binding.songOfTheDayNoContent.isVisible = true
                    else {
                        binding.songOfTheDayNoContent.isGone = true
                        if (songOfTheDayAdapter.song == null ||
                            songOfTheDayAdapter.song?.songId != songs[0].songId) {
                            songOfTheDayAdapter.changeItem(songs[0])
                        }
                        binding.textViewSongOfTheDay.setOnClickListener {
                            val action =
                                PlaylistsFragmentDirections.actionSelectPlaylist(getString(R.string.song_day))
                            findNavController().navigate(action)
                        }
                    }
                }
            }
        }

        musicLibraryViewModel.getPlaylistByNameLiveData(getString(R.string.favourites)).observe(viewLifecycleOwner) { playlist ->
            playlist?.let {
                lifecycleScope.launch(Dispatchers.Main) {
                    val previousSongs = favouritesAdapter.previousSongs
                    val songs = withContext(Dispatchers.IO) {
                        musicLibraryViewModel.extractPlaylistSongs(it.songs)
                    }
                    val adapterSongs = songs.asReversed().take(10)
                    if (songs.isEmpty()) binding.homeFavourites.isGone = true
                    else {
                        binding.homeFavourites.isVisible = true
                        binding.textViewFavourites.setOnClickListener {
                            val action =
                                PlaylistsFragmentDirections.actionSelectPlaylist(getString(R.string.favourites))
                            findNavController().navigate(action)
                        }
                    }
                    when {
                        favouritesAdapter.songs.isEmpty() -> {
                            favouritesAdapter.songs = adapterSongs.toMutableList()
                            favouritesAdapter.notifyItemRangeInserted(0, favouritesAdapter.songs.size)
                        }
                        favouritesAdapter.previousSongs.size != songs.size -> {
                            if (songs.size > previousSongs.size) {
                                favouritesAdapter.processSongs(adapterSongs, true)
                                (binding.favouritesRecyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(
                                    0,
                                    0
                                )
                            } else favouritesAdapter.processSongs(adapterSongs, false)
                        }
                    }
                    favouritesAdapter.previousSongs = songs
                }
            }
        }

        musicLibraryViewModel.getPlaylistByNameLiveData(getString(R.string.most_played)).observe(viewLifecycleOwner) { playlist ->
            playlist?.let {
                lifecycleScope.launch(Dispatchers.Main) {
                    val songs = withContext(Dispatchers.IO) {
                        musicLibraryViewModel.extractPlaylistSongs(it.songs)
                    }
                    if (songs.isEmpty()) binding.homeMostPlayed.isGone = true
                    else {
                        binding.homeMostPlayed.isVisible = true
                        binding.textViewMostPlayed.setOnClickListener {
                            val action =
                                PlaylistsFragmentDirections.actionSelectPlaylist(getString(R.string.most_played))
                            findNavController().navigate(action)
                        }
                    }
                    when {
                        mostPlayedAdapter.songs.isEmpty() -> {
                            mostPlayedAdapter.songs = songs.take(10).toMutableList()
                            mostPlayedAdapter.notifyItemRangeInserted(
                                0,
                                mostPlayedAdapter.songs.size
                            )
                        }
                        else -> mostPlayedAdapter.processSongs(songs.take(10))
                    }
                }
            }
        }

        musicLibraryViewModel.getPlaylistByNameLiveData(getString(R.string.recently_played)).observe(viewLifecycleOwner) { playlist ->
            playlist?.let {
                lifecycleScope.launch(Dispatchers.Main) {
                    val songs = withContext(Dispatchers.IO) {
                        musicLibraryViewModel.extractPlaylistSongs(it.songs)
                    }
                    if (songs.isEmpty()) binding.homeRecentlyPlayed.isGone = true
                    else {
                        binding.homeRecentlyPlayed.isVisible = true
                        binding.textViewRecentlyPlayed.setOnClickListener {
                            val action =
                                PlaylistsFragmentDirections.actionSelectPlaylist(getString(R.string.recently_played))
                            findNavController().navigate(action)
                        }
                    }
                    when {
                        recentlyPlayedAdapter.songs.isEmpty() -> {
                            recentlyPlayedAdapter.songs = songs.take(10).toMutableList()
                            recentlyPlayedAdapter.notifyItemRangeInserted(
                                0,
                                recentlyPlayedAdapter.songs.size
                            )
                        }
                        else -> {
                            recentlyPlayedAdapter.processSongs(songs.take(10))
                            (binding.recentlyPlayedRecyclerView.layoutManager as LinearLayoutManager).scrollToPosition(
                                0
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
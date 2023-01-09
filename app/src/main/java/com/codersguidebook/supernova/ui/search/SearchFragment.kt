package com.codersguidebook.supernova.ui.search

import android.os.Bundle
import android.view.*
import android.widget.SearchView
import androidx.activity.OnBackPressedCallback
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewbinding.ViewBinding
import com.codersguidebook.supernova.data.MusicDatabase
import com.codersguidebook.supernova.R
import com.codersguidebook.supernova.databinding.FragmentSearchBinding
import com.codersguidebook.supernova.fragment.BaseRecyclerViewFragment
import com.codersguidebook.supernova.fragment.adapter.SearchAdapter
import com.codersguidebook.supernova.params.SearchTypeConstants.Companion.ALBUM
import com.codersguidebook.supernova.params.SearchTypeConstants.Companion.ARTIST
import com.codersguidebook.supernova.params.SearchTypeConstants.Companion.PLAYLIST
import com.codersguidebook.supernova.params.SearchTypeConstants.Companion.TRACK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SearchFragment : BaseRecyclerViewFragment() {

    override var _binding: ViewBinding? = null
        get() = field as FragmentSearchBinding?
    override val binding: FragmentSearchBinding
        get() = _binding!! as FragmentSearchBinding

    private var musicDatabase: MusicDatabase? = null
    private var query = ""
    private var searchView: SearchView? = null
    override lateinit var adapter: SearchAdapter
    private lateinit var onBackPressedCallback: OnBackPressedCallback

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        musicDatabase = MusicDatabase.getDatabase(mainActivity, lifecycleScope)

        onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                mainActivity.iconifySearchView()
                findNavController().popBackStack()
            }
        }

        mainActivity.onBackPressedDispatcher.addCallback(viewLifecycleOwner, onBackPressedCallback)

        binding.recyclerView.layoutManager = LinearLayoutManager(activity)
        binding.recyclerView.itemAnimator = DefaultItemAnimator()

        binding.chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (adapter.itemCount > 0) clearRecyclerView()

            val checkedId = if (checkedIds.isNotEmpty()) checkedIds[0]
            else R.id.trackChip

            adapter.itemType = when (checkedId) {
                R.id.albumChip -> ALBUM
                R.id.artistChip -> ARTIST
                R.id.playlistChip -> PLAYLIST
                else -> TRACK
            }

            if (query.isNotEmpty()) requestNewData()
        }

        setupMenu()
    }

    override fun initialiseAdapter() {
        adapter = SearchAdapter(mainActivity)
    }

    /** Clear the contents of the RecyclerView */
    private fun clearRecyclerView() {
        val itemCount = adapter.itemCount
        adapter.songs.clear()
        adapter.artists.clear()
        adapter.albums.clear()
        adapter.playlists.clear()
        adapter.notifyItemRangeRemoved(0, itemCount)
    }

    override fun requestNewData() {
        binding.noResults.isGone = true
        when (adapter.itemType) {
            TRACK -> {
                lifecycleScope.launch(Dispatchers.IO) {
                    val songs = musicDatabase!!.musicDao().getSongsLikeSearch(query).take(10)

                    lifecycleScope.launch(Dispatchers.Main) {
                        if (songs.isEmpty()) binding.noResults.isVisible = true
                        if (adapter.songs.isEmpty()) {
                            adapter.songs.addAll(songs)
                            adapter.notifyItemRangeInserted(0, songs.size)
                        } else {
                            for ((index, song) in songs.withIndex()) {
                                adapter.processLoopIterationSong(index, song)
                            }

                            if (adapter.songs.size > songs.size) {
                                val numberItemsToRemove = adapter.songs.size - songs.size
                                repeat(numberItemsToRemove) { adapter.songs.removeLast() }
                                adapter.notifyItemRangeRemoved(songs.size, numberItemsToRemove)
                            }
                        }
                    }
                }
            }
            ALBUM -> {
                lifecycleScope.launch(Dispatchers.IO) {
                    val songs = musicDatabase!!.musicDao().getSongsLikeSearch(query)
                    val songsByAlbum = songs.distinctBy { song ->
                        song.albumId
                    }.take(10)

                    lifecycleScope.launch(Dispatchers.Main) {
                        if (songsByAlbum.isEmpty()) binding.noResults.isVisible = true
                        if (adapter.albums.isEmpty()) {
                            adapter.albums.addAll(songsByAlbum)
                            adapter.notifyItemRangeInserted(0, songsByAlbum.size)
                        } else {
                            for ((index, album) in songsByAlbum.withIndex()) {
                                adapter.processLoopIterationAlbum(index, album)
                            }

                            if (adapter.albums.size > songsByAlbum.size) {
                                val numberItemsToRemove = adapter.albums.size - songsByAlbum.size
                                repeat(numberItemsToRemove) { adapter.albums.removeLast() }
                                adapter.notifyItemRangeRemoved(songsByAlbum.size, numberItemsToRemove)
                            }
                        }
                    }
                }
            }
            ARTIST -> {
                lifecycleScope.launch(Dispatchers.IO) {
                    val artists = musicDatabase!!.musicDao().getArtistsLikeSearch(query)

                    lifecycleScope.launch(Dispatchers.Main) {
                        if (artists.isEmpty()) binding.noResults.isVisible = true
                        if (adapter.artists.isEmpty()) {
                            adapter.artists.addAll(artists)
                            adapter.notifyItemRangeInserted(0, artists.size)
                        } else {
                            for ((index, artist) in artists.withIndex()) {
                                adapter.processLoopIterationArtist(index, artist)
                            }

                            if (adapter.artists.size > artists.size) {
                                val numberItemsToRemove = adapter.artists.size - artists.size
                                repeat(numberItemsToRemove) { adapter.artists.removeLast() }
                                adapter.notifyItemRangeRemoved(artists.size, numberItemsToRemove)
                            }
                        }
                    }
                }
            }
            PLAYLIST -> {
                lifecycleScope.launch(Dispatchers.IO) {
                    val playlists = musicDatabase!!.playlistDao().getPlaylistsLikeSearch(query)

                    lifecycleScope.launch(Dispatchers.Main) {
                        if (playlists.isEmpty()) binding.noResults.isVisible = true
                        if (adapter.playlists.isEmpty()) {
                            adapter.playlists.addAll(playlists)
                            adapter.notifyItemRangeInserted(0, playlists.size)
                        } else {
                            for ((index, playlist) in playlists.withIndex()) {
                                adapter.processLoopIterationPlaylist(index, playlist)
                            }

                            if (adapter.playlists.size > playlists.size) {
                                val numberItemsToRemove = adapter.playlists.size - playlists.size
                                repeat(numberItemsToRemove) { adapter.playlists.removeLast() }
                                adapter.notifyItemRangeRemoved(playlists.size, numberItemsToRemove)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setupMenu() {
        (requireActivity() as MenuHost).addMenuProvider(object : MenuProvider {
            override fun onPrepareMenu(menu: Menu) {
                val searchItem = menu.findItem(R.id.search)
                searchView = searchItem.actionView as SearchView

                val onQueryListener = object : SearchView.OnQueryTextListener {
                    override fun onQueryTextChange(newText: String): Boolean {
                        query = "%$newText%"
                        requestNewData()
                        return true
                    }
                    override fun onQueryTextSubmit(query: String): Boolean = true
                }

                searchView?.apply {
                    isIconifiedByDefault = false
                    queryHint = getString(R.string.search_hint)
                    setOnQueryTextListener(onQueryListener)
                }

                binding.recyclerView.adapter = adapter
            }

            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) { }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean = false
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    override fun onStop() {
        super.onStop()
        mainActivity.hideKeyboard()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        onBackPressedCallback.remove()
    }
}
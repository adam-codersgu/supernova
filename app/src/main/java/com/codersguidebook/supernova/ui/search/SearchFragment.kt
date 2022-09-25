package com.codersguidebook.supernova.ui.search

import android.os.Bundle
import android.view.*
import android.widget.SearchView
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.codersguidebook.supernova.MainActivity
import com.codersguidebook.supernova.MusicDatabase
import com.codersguidebook.supernova.R
import com.codersguidebook.supernova.databinding.FragmentSearchBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private var musicDatabase: MusicDatabase? = null
    private var query = ""
    private var searchType = TRACK
    private var searchView: SearchView? = null
    private lateinit var adapter: SearchAdapter
    private lateinit var callingActivity: MainActivity

    companion object {
        const val TRACK = 0
        const val ALBUM = 1
        const val ARTIST = 2
        const val PLAYLIST = 3
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)
        callingActivity = activity as MainActivity
        adapter = SearchAdapter(callingActivity)
        musicDatabase = MusicDatabase.getDatabase(requireContext(), lifecycleScope)

        binding.chipGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.trackChip -> {
                    searchType = TRACK
                    adapter.itemType = TRACK
                }
                R.id.albumChip -> {
                    searchType = ALBUM
                    adapter.itemType = ALBUM
                }
                R.id.artistChip -> {
                    searchType = ARTIST
                    adapter.itemType = ARTIST
                }
                R.id.playlistChip -> {
                    searchType = PLAYLIST
                    adapter.itemType = PLAYLIST
                }
            }

            when {
                adapter.songs.isNotEmpty() -> {
                    adapter.notifyItemRangeRemoved(0, adapter.songs.size)
                    adapter.songs = mutableListOf()
                }
                adapter.albums.isNotEmpty() -> {
                    adapter.notifyItemRangeRemoved(0, adapter.albums.size)
                    adapter.albums = mutableListOf()
                }
                adapter.artists.isNotEmpty() -> {
                    adapter.notifyItemRangeRemoved(0, adapter.artists.size)
                    adapter.artists = mutableListOf()
                }
                adapter.playlists.isNotEmpty() -> {
                    adapter.notifyItemRangeRemoved(0, adapter.playlists.size)
                    adapter.playlists = mutableListOf()
                }
            }

            if (query != "") search()
        }
        
        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        val searchItem = menu.findItem(R.id.search)
        searchView = searchItem.actionView as SearchView

        val onQueryListener = object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String): Boolean {
                query = "%$newText%"
                search()
                return true
            }
            override fun onQueryTextSubmit(query: String): Boolean = true
        }

        searchView?.apply {
            isIconifiedByDefault = false
            queryHint = getString(R.string.search_hint)
            setOnQueryTextListener(onQueryListener)
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(activity)
        binding.recyclerView.itemAnimator = DefaultItemAnimator()
        binding.recyclerView.adapter = adapter

        super.onCreateOptionsMenu(menu, inflater)
    }

    private fun search() {
        when (searchType) {
            TRACK -> {
                lifecycleScope.launch(Dispatchers.IO) {
                    val songs = musicDatabase!!.musicDao().findBySearchSongs(query).take(10)

                    lifecycleScope.launch(Dispatchers.Main) {
                        val adapterSongs = adapter.songs

                        when {
                            songs.isEmpty() -> {
                                adapter.songs = mutableListOf()
                                adapter.notifyDataSetChanged()
                                binding.noResults.isVisible = true
                            }
                            adapterSongs.isEmpty() -> {
                                binding.noResults.isGone = true
                                adapter.songs = songs.toMutableList()
                                adapter.notifyItemRangeInserted(0, songs.size)
                            }
                            else -> {
                                binding.noResults.isGone = true
                                val removeItems = adapterSongs - songs
                                val addItems = songs - adapterSongs
                                for (s in removeItems) {
                                    val index = adapter.songs.indexOfFirst {
                                        it.songID == s.songID
                                    }
                                    adapter.songs.removeAt(index)
                                    adapter.notifyItemRemoved(index)
                                    adapter.notifyItemChanged(index)
                                }
                                for (s in addItems) {
                                    val index = songs.indexOfFirst {
                                        it.songID == s.songID
                                    }
                                    adapter.songs.add(index, s)
                                    adapter.notifyItemInserted(index)
                                    adapter.notifyItemChanged(index)
                                }
                            }
                        }
                    }
                }
            }
            ALBUM -> {
                lifecycleScope.launch(Dispatchers.IO) {
                    val songs = musicDatabase!!.musicDao().findBySearchSongs(query)
                    val albums = songs.distinctBy { song ->
                        song.album
                    }.take(10)

                    lifecycleScope.launch(Dispatchers.Main) {
                        val adapterAlbums = adapter.albums

                        when {
                            albums.isEmpty() -> {
                                adapter.albums = mutableListOf()
                                adapter.notifyDataSetChanged()
                                binding.noResults.isVisible = true
                            }
                            adapterAlbums.isEmpty() -> {
                                binding.noResults.isGone = true
                                adapter.albums = albums.toMutableList()
                                adapter.notifyItemRangeInserted(0, albums.size)
                            }
                            else -> {
                                binding.noResults.isGone = true
                                val removeItems = adapterAlbums - albums
                                val addItems = albums - adapterAlbums
                                for (a in removeItems) {
                                    val index = adapter.albums.indexOfFirst {
                                        it.songID == a.songID
                                    }
                                    adapter.albums.removeAt(index)
                                    adapter.notifyItemRemoved(index)
                                    adapter.notifyItemChanged(index)
                                }
                                for (a in addItems) {
                                    val index = albums.indexOfFirst {
                                        it.songID == a.songID
                                    }
                                    adapter.albums.add(index, a)
                                    adapter.notifyItemInserted(index)
                                    adapter.notifyItemChanged(index)
                                }
                            }
                        }
                    }
                }
            }
            ARTIST -> {
                lifecycleScope.launch(Dispatchers.IO) {
                    val artists = musicDatabase!!.musicDao().findBySearchArtists(query)

                    lifecycleScope.launch(Dispatchers.Main) {
                        val adapterArtists = adapter.artists

                        when {
                            artists.isEmpty() -> {
                                adapter.artists = mutableListOf()
                                adapter.notifyDataSetChanged()
                                binding.noResults.isVisible = true
                            }
                            adapterArtists.isEmpty() -> {
                                binding.noResults.isGone = true
                                adapter.artists = artists.toMutableList()
                                adapter.notifyItemRangeInserted(0, artists.size)
                            }
                            else -> {
                                binding.noResults.isGone = true
                                val removeItems = adapterArtists - artists
                                val addItems = artists - adapterArtists
                                for (a in removeItems) {
                                    val index = adapter.artists.indexOfFirst {
                                        it.artistName == a.artistName
                                    }
                                    adapter.artists.removeAt(index)
                                    adapter.notifyItemRemoved(index)
                                    adapter.notifyItemChanged(index)
                                }
                                for (a in addItems) {
                                    val index = artists.indexOfFirst {
                                        it.artistName == a.artistName
                                    }
                                    adapter.artists.add(index, a)
                                    adapter.notifyItemInserted(index)
                                    adapter.notifyItemChanged(index)
                                }
                            }
                        }
                    }
                }
            }
            PLAYLIST -> {
                lifecycleScope.launch(Dispatchers.IO) {
                    val playlists = musicDatabase!!.playlistDao().findBySearchPlaylists(query)

                    lifecycleScope.launch(Dispatchers.Main) {
                        val adapterPlaylists = adapter.playlists

                        when {
                            playlists.isEmpty() -> {
                                adapter.playlists = mutableListOf()
                                adapter.notifyDataSetChanged()
                                binding.noResults.isVisible = true
                            }
                            adapterPlaylists.isEmpty() -> {
                                binding.noResults.isGone = true
                                adapter.playlists = playlists.toMutableList()
                                adapter.notifyItemRangeInserted(0, playlists.size)
                            }
                            else -> {
                                binding.noResults.isGone = true
                                val removeItems = adapterPlaylists - playlists
                                val addItems = playlists - adapterPlaylists
                                for (p in removeItems) {
                                    val index = adapter.playlists.indexOfFirst {
                                        it.playlistID == p.playlistID
                                    }
                                    adapter.playlists.removeAt(index)
                                    adapter.notifyItemRemoved(index)
                                    adapter.notifyItemChanged(index)
                                }
                                for (p in addItems) {
                                    val index = playlists.indexOfFirst {
                                        it.playlistID == p.playlistID
                                    }
                                    adapter.playlists.add(index, p)
                                    adapter.notifyItemInserted(index)
                                    adapter.notifyItemChanged(index)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        callingActivity.hideKeyboard(callingActivity)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
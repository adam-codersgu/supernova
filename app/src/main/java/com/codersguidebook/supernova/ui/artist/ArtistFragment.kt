package com.codersguidebook.supernova.ui.artist

import android.os.Bundle
import android.view.*
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.codersguidebook.supernova.MusicDatabase
import com.codersguidebook.supernova.R
import com.codersguidebook.supernova.databinding.FragmentWithRecyclerViewBinding
import com.codersguidebook.supernova.entities.Song
import com.codersguidebook.supernova.recyclerview.RecyclerViewFragment
import com.codersguidebook.supernova.recyclerview.adapter.ArtistAdapter

class ArtistFragment : RecyclerViewFragment() {

    private var artistName: String? = null
    override val binding get() = fragmentBinding as FragmentWithRecyclerViewBinding
    override lateinit var adapter: ArtistAdapter
    private lateinit var musicDatabase: MusicDatabase

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        arguments?.let {
            val safeArgs = ArtistFragmentArgs.fromBundle(it)
            artistName = safeArgs.artist
        }
        fragmentBinding = FragmentWithRecyclerViewBinding.inflate(inflater, container, false)
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val layoutManager = LinearLayoutManager(activity)
        binding.root.layoutManager = layoutManager
        binding.root.itemAnimator = DefaultItemAnimator()
        binding.root.adapter = adapter

        musicDatabase = MusicDatabase.getDatabase(mainActivity, lifecycleScope)
        musicDatabase.musicDao().findArtistsSongs(artistName ?: "").observe(viewLifecycleOwner) {
            updateRecyclerView(it)
        }
    }

    override fun updateRecyclerView(songs: List<Song>) {
        super.updateRecyclerView(songs)

        val songsByAlbumByYear = songs.distinctBy { song ->
            song.albumId
        }.sortedBy { song ->
            song.year
        }.toMutableList()

        if (adapter.songsByAlbumByYear.isEmpty()) {
            adapter.songsByAlbumByYear.addAll(songsByAlbumByYear)
            adapter.notifyItemRangeInserted(0, adapter.getRecyclerViewIndex(songsByAlbumByYear.size))
        } else {
            for ((index, album) in songsByAlbumByYear.withIndex()) {
                adapter.processLoopIteration(index, album)
            }

            if (adapter.songsByAlbumByYear.size > songsByAlbumByYear.size) {
                val numberItemsToRemove = adapter.songsByAlbumByYear.size - songsByAlbumByYear.size
                repeat(numberItemsToRemove) { adapter.songsByAlbumByYear.removeLast() }
                adapter.notifyItemRangeRemoved(songsByAlbumByYear.size, numberItemsToRemove)
            }
        }

        if (songs.isNotEmpty()) {
            artistName?.let {
                val plays = musicDatabase.musicDao().getSongPlaysByArtist(it)
                if (plays != adapter.plays) {
                    adapter.plays = plays
                    adapter.notifyItemChanged(0)
                }
            }
        }

        setupMenu(songs.sortedBy { it.title })

        setIsUpdatingFalse()
    }

    override fun requestNewData() {
        musicDatabase.musicDao().findArtistsSongs(artistName ?: return).value?.let {
            updateRecyclerView(it)
        }
    }

    override fun initialiseAdapter() {
        adapter = ArtistAdapter(mainActivity)
    }

    private fun setupMenu(songs: List<Song>) {
        (requireActivity() as MenuHost).addMenuProvider(object : MenuProvider {
            override fun onPrepareMenu(menu: Menu) {
                menu.setGroupVisible(R.id.menu_group_artist_actions, true)
            }

            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) { }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {

                when (menuItem.itemId) {
                    R.id.artist_play_next -> mainActivity.addSongsToPlayQueue(songs, true)
                    R.id.artist_add_queue -> mainActivity.addSongsToPlayQueue(songs)
                    R.id.artist_add_playlist -> mainActivity.openAddToPlaylistDialog(songs)
                    R.id.artist_edit_artist_info -> {
                        artistName?.let {
                            findNavController().navigate(ArtistFragmentDirections.actionEditArtist(it))
                        }
                    }
                    else -> return false
                }
                return true
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }
}
package com.codersguidebook.supernova.ui.artist

import android.os.Bundle
import android.view.*
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.codersguidebook.supernova.MusicDatabase
import com.codersguidebook.supernova.R
import com.codersguidebook.supernova.entities.Song
import com.codersguidebook.supernova.recyclerview.RecyclerViewFragment
import com.codersguidebook.supernova.recyclerview.adapter.ArtistAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ArtistFragment : RecyclerViewFragment() {

    private var artistName: String? = null
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
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        musicDatabase = MusicDatabase.getDatabase(mainActivity, lifecycleScope)
        musicDatabase.musicDao().findArtistsSongs(artistName ?: "").observe(viewLifecycleOwner) {
            updateRecyclerView(it)
        }
    }

    /**
     * Refresh the content displayed in the RecyclerView.
     *
     * @param songs - The up-to-date list of Song objects that should be displayed.
     */
    private fun updateRecyclerView(songs: List<Song>) {
        setIsUpdatingTrue()

        val songsByAlbumByYear = songs.distinctBy { song ->
            song.albumId
        }.sortedBy { song ->
            song.year
        }.toMutableList()

        if (adapter.songs.isEmpty()) {
            adapter.songs.addAll(songsByAlbumByYear)
            adapter.notifyItemRangeInserted(0, adapter.getRecyclerViewIndex(songsByAlbumByYear.size))
        } else {
            for ((index, album) in songsByAlbumByYear.withIndex()) {
                adapter.processLoopIteration(index, album)
            }

            if (adapter.songs.size > songsByAlbumByYear.size) {
                val numberItemsToRemove = adapter.songs.size - songsByAlbumByYear.size
                repeat(numberItemsToRemove) { adapter.songs.removeLast() }
                adapter.notifyItemRangeRemoved(songsByAlbumByYear.size, numberItemsToRemove)
            }
        }

        if (songs.isNotEmpty()) {
            artistName?.let {
                lifecycleScope.launch(Dispatchers.IO) {
                    val plays = musicDatabase.musicDao().getSongPlaysByArtist(it)
                    if (plays != adapter.plays) {
                        adapter.plays = plays
                        adapter.notifyItemChanged(0)
                    }
                }
            }
        }

        setupMenu(songs.sortedBy { it.title })
        finishUpdate()
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
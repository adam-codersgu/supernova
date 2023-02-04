package com.codersguidebook.supernova.ui.artist

import android.os.Build
import android.os.Bundle
import android.view.*
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.codersguidebook.supernova.R
import com.codersguidebook.supernova.entities.Song
import com.codersguidebook.supernova.fragment.RecyclerViewFragment
import com.codersguidebook.supernova.fragment.adapter.ArtistAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ArtistFragment : RecyclerViewFragment() {

    private var artistName = getString(R.string.default_artist)
    override lateinit var adapter: ArtistAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
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

    /**
     * Refresh the content displayed in the RecyclerView.
     *
     * @param songs - The up-to-date list of Song objects that should be displayed.
     */
    private fun updateRecyclerView(songs: List<Song>) {
        setIsUpdatingTrue()

        val songsByAlbumByYear = songs.distinctBy { song ->
            song.albumId
        }.sortedByDescending { song ->
            song.year
        }.toMutableList()

        if (adapter.songs.isEmpty()) {
            adapter.songs.addAll(songsByAlbumByYear)
            adapter.notifyItemRangeInserted(0, adapter.getRecyclerViewIndex(songsByAlbumByYear.size))
        } else {
            adapter.processNewSongs(songsByAlbumByYear)
        }

        if (songs.isNotEmpty()) {
            lifecycleScope.launch(Dispatchers.IO) {
                val plays = musicLibraryViewModel.getSongPlaysByArtist(artistName)
                if (plays != adapter.plays) {
                    adapter.plays = plays
                    adapter.notifyItemChanged(0)
                }
            }
        }

        // Refresh the header
        adapter.notifyItemChanged(0)

        setupMenu(songs)
        finishUpdate()
    }

    override fun requestNewData() {
        musicLibraryViewModel.activeArtistSongs.value?.let { updateRecyclerView(it) }
    }

    override fun initialiseAdapter() {
        adapter = ArtistAdapter(mainActivity)
    }

    private fun setupMenu(songs: List<Song>) {
        (requireActivity() as MenuHost).addMenuProvider(object : MenuProvider {
            override fun onPrepareMenu(menu: Menu) {
                menu.setGroupVisible(R.id.menu_group_artist_actions, true)
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                    menu.findItem(R.id.artist_delete_artist).isVisible = false
                }
            }

            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) { }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {

                when (menuItem.itemId) {
                    R.id.artist_play_next -> mainActivity.addSongsToPlayQueue(songs, true)
                    R.id.artist_add_queue -> mainActivity.addSongsToPlayQueue(songs)
                    R.id.artist_add_playlist -> mainActivity.openAddToPlaylistDialog(songs)
                    R.id.artist_edit_artist_info -> {
                        findNavController().navigate(ArtistFragmentDirections.actionEditArtist(artistName))
                    }
                    R.id.artist_delete_artist -> {
                        // Delete Artist feature only available from SDK 30 and up
                        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                            mainActivity.deleteSongs(songs)
                        }
                    }
                    else -> return false
                }
                return true
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }
}
package com.codersguidebook.supernova.ui.artist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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

    private var artistName = ""
    override lateinit var adapter: ArtistAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        arguments?.let {
            val safeArgs = ArtistFragmentArgs.fromBundle(it)
            artistName = safeArgs.artist ?: getString(R.string.default_artist)
        }

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        artistName = musicLibraryViewModel.getUpdatedNavigationArgument() ?: artistName

        musicLibraryViewModel.setActiveArtistName(artistName)

        musicLibraryViewModel.activeArtistSongs.observe(viewLifecycleOwner) { songs ->
            updateRecyclerView(songs)
        }
    }

    /**
     * Refresh the content displayed in the RecyclerView.
     *
     * @param songs The up-to-date list of Song objects that should be displayed.
     */
    private fun updateRecyclerView(songs: List<Song>) {
        setIsUpdatingTrue()

        val songsByAlbumByYear = songs.distinctBy { song ->
            song.albumId
        }.sortedByDescending { song ->
            song.year
        }.toMutableList()

        adapter.processNewItems(songsByAlbumByYear)

        if (songs.isNotEmpty()) {
            lifecycleScope.launch(Dispatchers.IO) {
                val plays = musicLibraryViewModel.getSongPlaysByArtist(artistName)
                if (plays != adapter.plays) {
                    adapter.plays = plays
                    binding.root.post {
                        adapter.notifyItemChanged(0)
                    }
                }
            }
        }

        // Refresh the header
        adapter.notifyItemChanged(0)

        finishUpdate()
    }

    override fun requestNewData() {
        musicLibraryViewModel.activeArtistSongs.value?.let { updateRecyclerView(it) }
    }

    override fun initialiseAdapter() {
        adapter = ArtistAdapter(mainActivity)
    }

    override fun setupMenu() {
        (requireActivity() as MenuHost).addMenuProvider(object : MenuProvider {
            override fun onPrepareMenu(menu: Menu) {
                menu.setGroupVisible(R.id.menu_group_artist_actions, true)
            }

            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) { }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                val songs = musicLibraryViewModel.activeArtistSongs.value
                if (songs == null) {
                    Toast.makeText(activity, getString(R.string.no_songs_for_artist),
                        Toast.LENGTH_SHORT).show()
                    return true
                }

                when (menuItem.itemId) {
                    R.id.artist_play_next -> mainActivity.addSongsToPlayQueue(songs, true)
                    R.id.artist_add_queue -> mainActivity.addSongsToPlayQueue(songs)
                    R.id.artist_add_playlist -> mainActivity.openAddToPlaylistDialog(songs)
                    R.id.artist_edit_artist_info -> {
                        findNavController().navigate(ArtistFragmentDirections.actionEditArtist(artistName))
                    }
                    R.id.artist_delete_artist -> {
                        mainActivity.deleteSongs(songs)
                    }
                    else -> return false
                }
                return true
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }
}
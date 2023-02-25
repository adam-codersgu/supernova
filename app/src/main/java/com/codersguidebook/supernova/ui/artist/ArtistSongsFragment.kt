package com.codersguidebook.supernova.ui.artist

import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import com.codersguidebook.supernova.R
import com.codersguidebook.supernova.fragment.RecyclerViewWithFabFragment
import com.codersguidebook.supernova.fragment.adapter.SongsAdapter

class ArtistSongsFragment : RecyclerViewWithFabFragment() {

    private var artistName = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        artistName = EditArtistFragmentArgs.fromBundle(arguments ?: Bundle()).artist
            ?: getString(R.string.default_artist)
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        musicLibraryViewModel.setActiveArtistName(artistName)

        musicLibraryViewModel.activeArtistSongs.observe(viewLifecycleOwner) { songs ->
            updateRecyclerView(songs)
        }
    }

    override fun initialiseAdapter() {
        adapter = SongsAdapter(mainActivity)
    }

    override fun requestNewData() {
        musicLibraryViewModel.activeArtistSongs.value?.let { updateRecyclerView(it) }
    }

    override fun setupMenu() {
        (requireActivity() as MenuHost).addMenuProvider(object : MenuProvider {
            override fun onPrepareMenu(menu: Menu) {
                menu.setGroupVisible(R.id.menu_group_artist_actions, true)
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                    menu.findItem(R.id.artist_delete_artist).isVisible = false
                }
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
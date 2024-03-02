package com.codersguidebook.supernova.ui.album

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
import androidx.navigation.fragment.findNavController
import com.codersguidebook.supernova.R
import com.codersguidebook.supernova.entities.Song
import com.codersguidebook.supernova.fragment.RecyclerViewWithFabFragment
import com.codersguidebook.supernova.fragment.adapter.AlbumAdapter
import com.codersguidebook.supernova.ui.artists.ArtistsFragmentDirections

class AlbumFragment : RecyclerViewWithFabFragment() {

    private var albumId: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        arguments?.let {
            val safeArgs = AlbumFragmentArgs.fromBundle(it)
            albumId = safeArgs.albumId
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        albumId?.let { albumId ->
            musicLibraryViewModel.setActiveAlbumId(albumId)

            musicLibraryViewModel.activeAlbumSongs.observe(viewLifecycleOwner) { songs ->
                updateRecyclerView(songs)
            }
        }
    }

    override fun initialiseAdapter() {
        adapter = AlbumAdapter(mainActivity)
    }

    override fun updateRecyclerView(songs: List<Song>) {
        val discNumbers = songs.distinctBy {
            it.track.toString().substring(0, 1).toInt()
        }.map { it.track.toString().substring(0, 1).toInt() }

        (adapter as AlbumAdapter).displayDiscNumbers = discNumbers.size > 1

        super.updateRecyclerView(songs)
    }

    override fun requestNewData() {
        musicLibraryViewModel.activeAlbumSongs.value?.let { updateRecyclerView(it) }
    }

    override fun setupMenu() {
        (requireActivity() as MenuHost).addMenuProvider(object : MenuProvider {
            override fun onPrepareMenu(menu: Menu) {
                menu.setGroupVisible(R.id.menu_group_album_actions, true)
                val songs = musicLibraryViewModel.activeAlbumSongs.value
                if (!songs.isNullOrEmpty()) {
                    val distinctArtists = songs.distinctBy {
                        it.artist
                    }
                    if (distinctArtists.size != 1) menu.findItem(R.id.album_view_artist).isVisible = false
                }
            }

            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) { }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                val songs = musicLibraryViewModel.activeAlbumSongs.value
                if (songs == null) {
                    Toast.makeText(activity, getString(R.string.no_songs_for_album),
                        Toast.LENGTH_SHORT).show()
                    return true
                }

                when (menuItem.itemId) {
                    R.id.album_play_next -> {
                        mainActivity.addSongsToPlayQueue(songs, true)
                    }
                    R.id.album_add_queue -> mainActivity.addSongsToPlayQueue(songs)
                    R.id.album_add_playlist -> mainActivity.openAddToPlaylistDialog(songs)
                    R.id.album_view_artist -> {
                        val action = ArtistsFragmentDirections.actionSelectArtist(songs[0].artist)
                        findNavController().navigate(action)
                    }
                    R.id.album_edit_album_info -> {
                        val action = AlbumFragmentDirections.actionEditAlbum(songs[0].albumId)
                        findNavController().navigate(action)
                    }
                    R.id.album_delete_album -> {
                        // Delete Album feature only available from SDK 30 and up
                        mainActivity.deleteSongs(songs)
                    }
                    else -> return false
                }
                return true
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }
}

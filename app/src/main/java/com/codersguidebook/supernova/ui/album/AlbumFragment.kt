package com.codersguidebook.supernova.ui.album

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
import com.codersguidebook.supernova.recyclerview.RecyclerViewWithFabFragment
import com.codersguidebook.supernova.recyclerview.adapter.AlbumAdapter
import com.codersguidebook.supernova.ui.artists.ArtistsFragmentDirections

class AlbumFragment : RecyclerViewWithFabFragment() {

    private var albumId: String? = null
    private lateinit var musicDatabase: MusicDatabase

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        arguments?.let {
            val safeArgs = AlbumFragmentArgs.fromBundle(it)
            albumId = safeArgs.albumID
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        albumId?.let { albumId ->
            musicDatabase = MusicDatabase.getDatabase(mainActivity, lifecycleScope)
            musicDatabase.musicDao().findAlbumSongs(albumId).observe(viewLifecycleOwner) {
                updateRecyclerView(it)
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

        setupMenu(songs)
    }

    override fun requestNewData() {
        musicDatabase.musicDao().findAlbumSongs(albumId ?: return).value?.let {
            updateRecyclerView(it)
        }
    }

    private fun setupMenu(songs: List<Song>) {
        (requireActivity() as MenuHost).addMenuProvider(object : MenuProvider {
            override fun onPrepareMenu(menu: Menu) {
                menu.setGroupVisible(R.id.menu_group_album_actions, true)
                if (songs.isNotEmpty()) {
                    val distinctArtists = songs.distinctBy {
                        it.artist
                    }
                    if (distinctArtists.size != 1) menu.findItem(R.id.album_view_artist).isVisible = false
                }
            }

            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) { }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
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
                    else -> return false
                }
                return true
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }
}

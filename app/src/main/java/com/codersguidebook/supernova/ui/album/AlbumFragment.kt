package com.codersguidebook.supernova.ui.album

import android.os.Bundle
import android.view.*
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.codersguidebook.supernova.MusicDatabase
import com.codersguidebook.supernova.R
import com.codersguidebook.supernova.entities.Song
import com.codersguidebook.supernova.recyclerview.RecyclerViewWithFabFragment
import com.codersguidebook.supernova.ui.artists.ArtistsFragmentDirections

class AlbumFragment : RecyclerViewWithFabFragment() {

    private var albumId: String? = null
    private lateinit var albumAdapter: AlbumAdapter
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

        val layoutManager = LinearLayoutManager(activity)
        albumAdapter = AlbumAdapter(mainActivity)
        binding.recyclerView.layoutManager = layoutManager
        binding.recyclerView.itemAnimator = DefaultItemAnimator()
        binding.recyclerView.adapter = albumAdapter

        binding.recyclerView.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0 && binding.fab.visibility == View.VISIBLE) binding.fab.hide()
                else if (dy < 0 && binding.fab.visibility != View.VISIBLE) binding.fab.show()
            }
        })

        albumId?.let { albumId ->
            musicDatabase = MusicDatabase.getDatabase(mainActivity, lifecycleScope)
            musicDatabase.musicDao().findAlbumSongs(albumId).observe(viewLifecycleOwner) {
                updateRecyclerView(it)
            }
        }
    }

    /**
     * Handle updates to the content of the RecyclerView. The below method will determine what
     * changes are required when an element/elements is/are changed, inserted, or deleted.
     *
     * @param index - The index of the current iteration through the up-to-date content list.
     * @param song - The Song object that should be displayed at the index.
     */
    private fun processLoopIteration(index: Int, song: Song) {
        when {
            index >= albumAdapter.songs.size -> {
                albumAdapter.songs.add(song)
                albumAdapter.notifyItemInserted(index + 1)
            }
            song.songId != albumAdapter.songs[index].songId -> {
                var numberOfItemsRemoved = 0
                do {
                    albumAdapter.songs.removeAt(index)
                    ++numberOfItemsRemoved
                } while (index < albumAdapter.songs.size &&
                    song.songId != albumAdapter.songs[index].songId)

                when {
                    numberOfItemsRemoved == 1 -> albumAdapter.notifyItemRemoved(index + 1)
                    numberOfItemsRemoved > 1 -> {
                        albumAdapter.notifyItemRangeRemoved(index + 1,
                            numberOfItemsRemoved)
                    }
                }

                processLoopIteration(index, song)
            }
            song != albumAdapter.songs[index] -> {
                albumAdapter.songs[index] = song
                albumAdapter.notifyItemChanged(index + 1)
            }
        }
    }

    override fun setupMenu(songs: List<Song>) {
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

    override fun requestNewData() {
        musicDatabase.musicDao().findAlbumSongs(albumId ?: return).value?.let {
            updateRecyclerView(it)
        }
    }
}

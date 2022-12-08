package com.codersguidebook.supernova.ui.artist

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.codersguidebook.supernova.MainActivity
import com.codersguidebook.supernova.MusicDatabase
import com.codersguidebook.supernova.R
import com.codersguidebook.supernova.databinding.FragmentWithRecyclerViewBinding
import com.codersguidebook.supernova.databinding.ScrollRecyclerViewBinding
import com.codersguidebook.supernova.entities.Song
import com.codersguidebook.supernova.recyclerview.RecyclerViewFragment
import com.codersguidebook.supernova.recyclerview.adapter.AlbumsAdapter
import com.codersguidebook.supernova.recyclerview.adapter.ArtistAdapter

class ArtistFragment : RecyclerViewFragment() {
    private var artistName: String? = null
    private var artistSongs = mutableListOf<Song>()
    private var artistAlbums = mutableListOf<Song>()

    override val binding get() = fragmentBinding as FragmentWithRecyclerViewBinding
    override lateinit var adapter: AlbumsAdapter

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


        val layoutManager = LinearLayoutManager(activity)
        val artistAdapter = ArtistAdapter(callingActivity, this)
        binding.root.layoutManager = layoutManager
        binding.root.itemAnimator = DefaultItemAnimator()
        binding.root.adapter = artistAdapter

        val musicDatabase = MusicDatabase.getDatabase(mainActivity, lifecycleScope)
        musicDatabase.musicDao().findArtistsSongs(artistName ?: "").observe(viewLifecycleOwner,
            { songs ->
                songs?.let {
                    val adapterSongCount = artistAdapter.artistSongs.size
                    artistSongs = it.toMutableList()
                    artistAdapter.artistSongs = artistSongs
                    artistAlbums = it.distinctBy { album ->
                        album.albumName
                    }.sortedByDescending { album ->
                        album.year
                    }.toMutableList()
                    when {
                        artistAdapter.albums.isEmpty() -> {
                            artistAdapter.albums = artistAlbums
                            artistAdapter.notifyDataSetChanged()
                        }
                        artistAdapter.albums.size != artistAlbums.size -> artistAdapter.processAlbums(artistAlbums)
                        // song count changed but albums have not
                        adapterSongCount != artistSongs.size -> artistAdapter.notifyItemChanged(0)
                    }
                }
            })
        
        setHasOptionsMenu(true)
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.setGroupVisible(R.id.menu_group_artist_actions, true)

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val songList = artistSongs.sortedBy {
            it.title
        }
        when (item.itemId) {
            R.id.artist_play_next -> callingActivity.addSongsToPlayQueue(songList, true)
            R.id.artist_add_queue -> callingActivity.addSongsToPlayQueue(songList)
            R.id.artist_add_playlist -> callingActivity.openAddToPlaylistDialog(songList)
            R.id.artist_edit_artist_info -> {
                val action = artistName?.let { ArtistFragmentDirections.actionEditArtist(it) }
                if (action != null) findNavController().navigate(action)
            }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }
}
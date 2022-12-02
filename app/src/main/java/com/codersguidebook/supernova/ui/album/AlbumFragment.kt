package com.codersguidebook.supernova.ui.album

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.codersguidebook.supernova.MainActivity
import com.codersguidebook.supernova.MusicDatabase
import com.codersguidebook.supernova.R
import com.codersguidebook.supernova.databinding.FragmentWithFabBinding
import com.codersguidebook.supernova.entities.Song
import com.codersguidebook.supernova.ui.artists.ArtistsFragmentDirections

class AlbumFragment : Fragment() {

    private var albumID: String? = null
    private var albumSongs = emptyList<Song>()
    private var _binding: FragmentWithFabBinding? = null
    private val binding get() = _binding!!
    private lateinit var callingActivity: MainActivity

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        arguments?.let {
            val safeArgs = AlbumFragmentArgs.fromBundle(it)
            albumID = safeArgs.albumID
        }
        _binding = FragmentWithFabBinding.inflate(inflater, container, false)
        callingActivity = activity as MainActivity
        val layoutManager = LinearLayoutManager(activity)
        val albumAdapter = AlbumAdapter(callingActivity)
        binding.recyclerView.layoutManager = layoutManager
        binding.recyclerView.itemAnimator = DefaultItemAnimator()
        binding.recyclerView.adapter = albumAdapter

        setHasOptionsMenu(true)

        binding.fab.setOnClickListener {
            callingActivity.playNewPlayQueue(albumSongs, shuffle = true)
        }

        binding.recyclerView.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0 && binding.fab.visibility == View.VISIBLE) binding.fab.hide()
                else if (dy < 0 && binding.fab.visibility != View.VISIBLE) binding.fab.show()
            }
        })

        val musicDatabase = MusicDatabase.getDatabase(callingActivity, lifecycleScope)
        musicDatabase.musicDao().findAlbumSongs(albumID ?: "").observe(viewLifecycleOwner, { songs ->
            songs?.let { it ->
                albumSongs = it
                val discNumbers = mutableListOf(1)
                for (s in albumSongs) {
                    val disc = s.track.toString().substring(0, 1).toInt()
                    if (!discNumbers.contains(disc)) discNumbers.add(disc)
                }
                when {
                    albumAdapter.songs.isEmpty() -> {
                        albumAdapter.displayDiscNumbers = discNumbers.size > 1
                        albumAdapter.songs = it.toMutableList()
                        albumAdapter.notifyDataSetChanged()
                    }
                    albumAdapter.songs.size != songs.size -> albumAdapter.processSongs(it)
                }
            }
        })

        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.setGroupVisible(R.id.menu_group_album_actions, true)
        if (albumSongs.isNotEmpty()) {
            val distinctArtists = albumSongs.distinctBy {
                it.artist
            }
            if (distinctArtists.size > 1) menu.findItem(R.id.album_view_artist).isVisible = false
        }

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.album_play_next -> {
                callingActivity.addSongsToPlayQueue(albumSongs, true)
                true
            }
            R.id.album_add_queue -> {
                callingActivity.addSongsToPlayQueue(albumSongs)
                true
            }
            R.id.album_add_playlist -> {
                callingActivity.openAddToPlaylistDialog(albumSongs)
                true
            }
            R.id.album_view_artist -> {
                val action = ArtistsFragmentDirections.actionSelectArtist(albumSongs[0].artist)
                findNavController().navigate(action)
                true
            }
            R.id.album_edit_album_info -> {
                val action = AlbumFragmentDirections.actionEditAlbum(albumSongs[0].albumId)
                findNavController().navigate(action)
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

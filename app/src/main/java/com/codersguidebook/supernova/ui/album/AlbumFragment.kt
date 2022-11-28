package com.codersguidebook.supernova.ui.album

import android.os.Bundle
import android.view.*
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
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

    private var albumId: String? = null
    private var _binding: FragmentWithFabBinding? = null
    private val binding get() = _binding!!
    private lateinit var callingActivity: MainActivity

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        arguments?.let {
            val safeArgs = AlbumFragmentArgs.fromBundle(it)
            albumId = safeArgs.albumID
        }
        _binding = FragmentWithFabBinding.inflate(inflater, container, false)
        callingActivity = activity as MainActivity

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val layoutManager = LinearLayoutManager(activity)
        val albumAdapter = AlbumAdapter(callingActivity)
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
            val musicDatabase = MusicDatabase.getDatabase(callingActivity, lifecycleScope)
            musicDatabase.musicDao().findAlbumSongs(albumId).observe(viewLifecycleOwner) { albumSongs ->
                setupMenu(albumSongs)

                binding.fab.setOnClickListener {
                    callingActivity.playSongsShuffled(albumSongs)
                }

                val discNumbers = albumSongs.distinctBy {
                    it.track.toString().substring(0, 1).toInt()
                }.map { it.track.toString().substring(0, 1).toInt() }
                
                when {
                    albumAdapter.songs.isEmpty() -> {
                        albumAdapter.displayDiscNumbers = discNumbers.size > 1
                        albumAdapter.songs = albumSongs.toMutableList()
                        albumAdapter.notifyItemRangeInserted(0, albumSongs.size)
                    }
                    albumAdapter.songs.size != albumSongs.size -> albumAdapter.processSongs(albumSongs)
                }
            }
        }
    }

    private fun setupMenu(albumSongs: List<Song>) {
        (requireActivity() as MenuHost).addMenuProvider(object : MenuProvider {
            override fun onPrepareMenu(menu: Menu) {
                menu.setGroupVisible(R.id.menu_group_album_actions, true)
                if (albumSongs.isNotEmpty()) {
                    val distinctArtists = albumSongs.distinctBy {
                        it.artist
                    }
                    if (distinctArtists.size != 1) menu.findItem(R.id.album_view_artist).isVisible = false
                }
            }

            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) { }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                when (menuItem.itemId) {
                    R.id.album_play_next -> {
                        callingActivity.addSongsToPlayQueue(albumSongs, true)
                    }
                    R.id.album_add_queue -> callingActivity.addSongsToPlayQueue(albumSongs)
                    R.id.album_add_playlist -> callingActivity.openAddToPlaylistDialog(albumSongs)
                    R.id.album_view_artist -> {
                        val action = ArtistsFragmentDirections.actionSelectArtist(albumSongs[0].artist)
                        findNavController().navigate(action)
                    }
                    R.id.album_edit_album_info -> {
                        val action = AlbumFragmentDirections.actionEditAlbum(albumSongs[0].albumId)
                        findNavController().navigate(action)
                    }
                    else -> return false
                }
                return true
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

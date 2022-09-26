package com.codersguidebook.supernova.ui.albums

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.codersguidebook.supernova.MainActivity
import com.codersguidebook.supernova.MusicViewModel
import com.codersguidebook.supernova.databinding.FragmentWithScrollBinding
import com.codersguidebook.supernova.entities.Song
import java.util.*

class AlbumsFragment : Fragment() {

    private var albums = mutableListOf<Song>()
    private var _binding: FragmentWithScrollBinding? = null
    private val binding get() = _binding!!
    private var isProcessing = false
    private lateinit var albumsAdapter: AlbumsAdapter
    private lateinit var callingActivity: MainActivity
    private lateinit var musicViewModel: MusicViewModel

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWithScrollBinding.inflate(inflater, container, false)
        callingActivity = activity as MainActivity
        val layoutManager = LinearLayoutManager(activity)
        albumsAdapter = AlbumsAdapter(callingActivity)
        binding.scrollRecyclerView.layoutManager = layoutManager
        binding.scrollRecyclerView.itemAnimator = DefaultItemAnimator()
        binding.scrollRecyclerView.adapter = albumsAdapter
        albumsAdapter.stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY

        musicViewModel = ViewModelProvider(this)[MusicViewModel::class.java]
        musicViewModel.allSongs.observe(viewLifecycleOwner, { songs ->
            songs?.let {
                if (it.isNotEmpty() || albums.isNotEmpty()) processAlbums(it)
            }
        })

        return binding.root
    }

    private fun processAlbums(albumList: List<Song>) {
        // use the isProcessing boolean to prevent the processAlbums method from being run multiple times in quick succession (e.g. when the library is being built for the first time)
        if (!isProcessing) {
            isProcessing = true
            albums = albumList.distinctBy { song ->
                song.albumId
            }.sortedBy { song ->
                song.albumName.toUpperCase(Locale.ROOT)
            }.toMutableList()

            val adapterAlbums = albumsAdapter.albums
            albumsAdapter.albums = albums
            when {
                adapterAlbums.isEmpty() -> albumsAdapter.notifyItemRangeInserted(0, albums.size)
                albums.size > adapterAlbums.size -> {
                    val difference = albums - adapterAlbums
                    for (s in difference) {
                        val index = albums.indexOfFirst {
                            it.albumId == s.albumId
                        }
                        if (index != -1) albumsAdapter.notifyItemInserted(index)
                    }
                }
                albums.size < adapterAlbums.size -> {
                    val difference = adapterAlbums - albums
                    for (s in difference) {
                        val index = adapterAlbums.indexOfFirst {
                            it.albumId == s.albumId
                        }
                        if (index != -1) albumsAdapter.notifyItemRemoved(index)
                    }
                }
            }
            isProcessing = false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

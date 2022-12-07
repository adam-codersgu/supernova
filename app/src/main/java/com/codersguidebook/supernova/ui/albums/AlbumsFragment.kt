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
import com.codersguidebook.supernova.MusicLibraryViewModel
import com.codersguidebook.supernova.databinding.FragmentWithScrollBinding
import com.codersguidebook.supernova.databinding.ScrollRecyclerViewBinding
import com.codersguidebook.supernova.entities.Song
import com.codersguidebook.supernova.recyclerview.RecyclerViewFragment
import com.codersguidebook.supernova.recyclerview.adapter.AlbumsAdapter
import com.codersguidebook.supernova.recyclerview.adapter.SongsAdapter
import java.util.*

class AlbumsFragment : RecyclerViewFragment() {

    private var albums = mutableListOf<Song>()
    private var _binding: ScrollRecyclerViewBinding? = null
    override lateinit var adapter: AlbumsAdapter
    private lateinit var musicLibraryViewModel: MusicLibraryViewModel

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        _binding = ScrollRecyclerViewBinding.inflate(inflater, container, false)
        mainActivity = activity as MainActivity

        musicLibraryViewModel = ViewModelProvider(this)[MusicLibraryViewModel::class.java]
        musicLibraryViewModel.allSongs.observe(viewLifecycleOwner, { songs ->
            songs?.let {
                if (it.isNotEmpty() || albums.isNotEmpty()) processAlbums(it)
            }
        })

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val layoutManager = LinearLayoutManager(activity)
        binding.recyclerView.layoutManager = layoutManager
        binding.recyclerView.itemAnimator = DefaultItemAnimator()
        binding.recyclerView.adapter = adapter

        binding.scrollRecyclerView.recyclerView.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0 && binding.fab.visibility == View.VISIBLE) binding.fab.hide()
                else if (dy < 0 && binding.fab.visibility != View.VISIBLE) binding.fab.show()
            }
        })
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

    override fun initialiseAdapter() {
        adapter = AlbumsAdapter(mainActivity)
        adapter.stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }
}

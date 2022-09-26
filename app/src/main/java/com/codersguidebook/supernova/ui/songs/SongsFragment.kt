package com.codersguidebook.supernova.ui.songs

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
import com.codersguidebook.supernova.entities.Song
import com.codersguidebook.supernova.databinding.FragmentWithFabBinding
import java.util.*

class SongsFragment : Fragment() {

    private var _binding: FragmentWithFabBinding? = null
    private val binding get() = _binding!!
    private var completeLibrary = mutableListOf<Song>()
    private var isProcessing = false
    private lateinit var musicViewModel: MusicViewModel
    private lateinit var callingActivity: MainActivity
    private lateinit var songsAdapter: SongsAdapter

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWithFabBinding.inflate(inflater, container, false)
        callingActivity = activity as MainActivity
        songsAdapter = SongsAdapter(callingActivity)
        binding.recyclerView.layoutManager = LinearLayoutManager(activity)
        binding.recyclerView.itemAnimator = DefaultItemAnimator()
        binding.recyclerView.adapter = songsAdapter
        songsAdapter.stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY

        musicViewModel = ViewModelProvider(this).get(MusicViewModel::class.java)
        musicViewModel.allSongs.observe(viewLifecycleOwner, { songs ->
            songs?.let {
                if (it.isNotEmpty() || completeLibrary.isNotEmpty()) processSongs(it)
            }
        })

        // Shuffle the music library then play it
        binding.fab.setOnClickListener {
            callingActivity.playNewSongs(completeLibrary, 0, true)
        }

        binding.recyclerView.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0 &&binding.fab.visibility == View.VISIBLE)binding.fab.hide()
                else if (dy < 0 &&binding.fab.visibility != View.VISIBLE)binding.fab.show()
            }
        })

        return binding.root
    }

    private fun processSongs(songList: List<Song>) {
        // use the isProcessing boolean to prevent the processSongs method from being run multiple times in quick succession (e.g. when the library is being built for the first time)
        if (!isProcessing) {
            isProcessing = true
            completeLibrary = songList.sortedBy { song ->
                song.title.toUpperCase(Locale.ROOT)
            }.toMutableList()

            val songs = songsAdapter.songs
            songsAdapter.songs = completeLibrary
            when {
                songs.isEmpty() -> songsAdapter.notifyItemRangeInserted(0, completeLibrary.size)
                completeLibrary.size > songs.size -> {
                    val difference = completeLibrary - songs
                    for (s in difference) {
                        val index = completeLibrary.indexOfFirst {
                            it.songId == s.songId
                        }
                        if (index != -1) songsAdapter.notifyItemInserted(index)
                    }
                }
                completeLibrary.size < songs.size -> {
                    val difference = songs - completeLibrary
                    for (s in difference) {
                        val index = songs.indexOfFirst {
                            it.songId == s.songId
                        }
                        if (index != -1) songsAdapter.notifyItemRemoved(index)
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

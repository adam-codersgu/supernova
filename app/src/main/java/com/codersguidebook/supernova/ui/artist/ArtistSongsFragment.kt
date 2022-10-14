package com.codersguidebook.supernova.ui.artist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.codersguidebook.supernova.MainActivity
import com.codersguidebook.supernova.MusicDatabase
import com.codersguidebook.supernova.databinding.FragmentWithFabBinding
import com.codersguidebook.supernova.entities.Song
import java.util.*

class ArtistSongsFragment : Fragment() {
    private var artistName: String? = null
    private var artistsSongs = listOf<Song>()
    private var _binding: FragmentWithFabBinding? = null
    private val binding get() = _binding!!
    private var musicDatabase: MusicDatabase? = null
    private lateinit var callingActivity: MainActivity

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        arguments?.let {
            val safeArgs = ArtistSongsFragmentArgs.fromBundle(it)
            artistName = safeArgs.artist
        }

        _binding = FragmentWithFabBinding.inflate(inflater, container, false)
        callingActivity = activity as MainActivity

        val layoutManager = LinearLayoutManager(activity)
        val artistSongsAdapter = ArtistSongsAdapter(callingActivity)
        binding.recyclerView.layoutManager = layoutManager
        binding.recyclerView.itemAnimator = DefaultItemAnimator()
        binding.recyclerView.adapter = artistSongsAdapter

        musicDatabase = MusicDatabase.getDatabase(requireActivity(), lifecycleScope)
        musicDatabase!!.musicDao().findArtistsSongs(artistName!!).observe(viewLifecycleOwner,
            { songs ->
                songs?.let {
                    artistsSongs = it.sortedBy { s ->
                        s.title.toUpperCase(Locale.ROOT)
                    }
                    when {
                        artistSongsAdapter.songs.isEmpty() -> {
                            artistSongsAdapter.songs = artistsSongs.toMutableList()
                            artistSongsAdapter.notifyDataSetChanged()
                        }
                        artistSongsAdapter.songs.size != artistsSongs.size -> artistSongsAdapter.processSongs(artistsSongs)
                    }
                }
            })

        binding.fab.setOnClickListener {
            callingActivity.playSongsShuffled(artistsSongs)
        }

        binding.recyclerView.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                when {
                    dy > 0 && binding.fab.visibility == View.VISIBLE -> binding.fab.hide()
                    dy < 0 && binding.fab.visibility != View.VISIBLE -> binding.fab.show()
                }
            }
        })

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
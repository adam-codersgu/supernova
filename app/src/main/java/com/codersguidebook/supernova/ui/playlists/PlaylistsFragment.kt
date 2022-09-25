package com.codersguidebook.supernova.ui.playlists

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.codersguidebook.supernova.MainActivity
import com.codersguidebook.supernova.MusicViewModel
import com.codersguidebook.supernova.databinding.FragmentWithRecyclerViewBinding
import java.util.*

class PlaylistsFragment : Fragment() {

    private var _binding: FragmentWithRecyclerViewBinding? = null
    private val binding get() = _binding!!
    private lateinit var musicViewModel: MusicViewModel
    private lateinit var callingActivity: MainActivity

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWithRecyclerViewBinding.inflate(inflater, container, false)
        callingActivity = activity as MainActivity
        musicViewModel = ViewModelProvider(this).get(MusicViewModel::class.java)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val playlistsAdapter = PlaylistsAdapter(callingActivity)
        binding.root.layoutManager = GridLayoutManager(context, 3)
        binding.root.itemAnimator = DefaultItemAnimator()
        binding.root.adapter = playlistsAdapter
        playlistsAdapter.stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY

        musicViewModel.allPlaylists.observe(viewLifecycleOwner, { playlists ->
            playlists?.let {
                var allPlaylists = it.toMutableList()
                allPlaylists.removeIf { p ->
                    p.isDefault && p.songs.isNullOrBlank()
                }
                allPlaylists = allPlaylists.sortedBy {p ->
                    p.name.uppercase(Locale.ROOT)
                }.toMutableList()

                when {
                    playlistsAdapter.playlists.isEmpty() -> {
                        playlistsAdapter.playlists = allPlaylists
                        playlistsAdapter.notifyItemRangeInserted(0, allPlaylists.size)
                    }
                    else -> playlistsAdapter.updatePlaylists(allPlaylists)
                }
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

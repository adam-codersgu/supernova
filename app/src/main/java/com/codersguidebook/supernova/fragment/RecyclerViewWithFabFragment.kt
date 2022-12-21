package com.codersguidebook.supernova.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.codersguidebook.supernova.databinding.FragmentWithFabBinding
import com.codersguidebook.supernova.entities.Song
import com.codersguidebook.supernova.fragment.adapter.SongAdapter

abstract class RecyclerViewWithFabFragment: BaseRecyclerViewFragment() {

    override var _binding: ViewBinding? = null
        get() = field as FragmentWithFabBinding?
    override val binding: FragmentWithFabBinding
        get() = _binding!! as FragmentWithFabBinding
    override lateinit var adapter: SongAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWithFabBinding.inflate(inflater, container, false)
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.scrollRecyclerView.recyclerView.layoutManager = LinearLayoutManager(mainActivity)
        binding.scrollRecyclerView.recyclerView.itemAnimator = DefaultItemAnimator()

        binding.scrollRecyclerView.recyclerView.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0 && binding.fab.visibility == View.VISIBLE) binding.fab.hide()
                else if (dy < 0 && binding.fab.visibility != View.VISIBLE) binding.fab.show()
            }
        })
    }

    /**
     * Refresh the content displayed in the RecyclerView.
     *
     * @param songs - The up-to-date list of Song objects that should be displayed.
     */
    open fun updateRecyclerView(songs: List<Song>) {
        setIsUpdatingTrue()

        binding.fab.setOnClickListener {
            mainActivity.playNewPlayQueue(songs, shuffle = true)
        }

        if (adapter.songs.isEmpty()) {
            adapter.songs.addAll(songs)
            adapter.notifyItemRangeInserted(0, songs.size)
        } else {
            for ((index, song) in songs.withIndex()) {
                adapter.processLoopIteration(index, song)
            }

            if (adapter.songs.size > songs.size) {
                val numberItemsToRemove = adapter.songs.size - songs.size
                repeat(numberItemsToRemove) { adapter.songs.removeLast() }
                adapter.notifyItemRangeRemoved(
                    adapter.getRecyclerViewIndex(songs.size), numberItemsToRemove)
            }
        }

        finishUpdate()
    }

    private fun finishUpdate() {
        if (binding.scrollRecyclerView.recyclerView.adapter == null) {
            binding.scrollRecyclerView.recyclerView.adapter = adapter
        }
        setIsUpdatingFalse()
    }
}
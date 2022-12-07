package com.codersguidebook.supernova.recyclerview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.codersguidebook.supernova.MainActivity
import com.codersguidebook.supernova.databinding.FragmentWithFabBinding
import com.codersguidebook.supernova.entities.Song
import com.codersguidebook.supernova.recyclerview.adapter.SongAdapter

abstract class RecyclerViewWithFabFragment: RecyclerViewFragment() {

    override var fragBinding: FragmentWithFabBinding? = null
    override val binding get() = fragmentBinding!!
    override lateinit var adapter: SongAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        fragmentBinding = FragmentWithFabBinding.inflate(inflater, container, false)
        mainActivity = activity as MainActivity
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val layoutManager = LinearLayoutManager(activity)
        binding.scrollRecyclerView.recyclerView.layoutManager = layoutManager
        binding.scrollRecyclerView.recyclerView.itemAnimator = DefaultItemAnimator()
        binding.scrollRecyclerView.recyclerView.adapter = adapter

        binding.scrollRecyclerView.recyclerView.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0 && binding.fab.visibility == View.VISIBLE) binding.fab.hide()
                else if (dy < 0 && binding.fab.visibility != View.VISIBLE) binding.fab.show()
            }
        })
    }

    override fun updateRecyclerView(songs: List<Song>) {
        super.updateRecyclerView(songs)

        setupMenu(songs)

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
                adapter.notifyItemRangeRemoved(songs.size, numberItemsToRemove)
            }
        }

        isUpdating = false
        if (unhandledRequestReceived) {
            unhandledRequestReceived = false
            requestNewData()
        }
    }
}
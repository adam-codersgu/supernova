package com.codersguidebook.supernova.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.codersguidebook.supernova.databinding.FragmentWithFabBinding
import com.codersguidebook.supernova.entities.Song
import com.codersguidebook.supernova.fragment.adapter.SongAdapter
import com.codersguidebook.supernova.views.RecyclerViewScrollbar
import kotlin.math.roundToInt

abstract class RecyclerViewWithFabFragment: BaseRecyclerViewFragment(), RecyclerViewScrollbar.Listener {

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
        binding.scrollRecyclerView.seekBar.setListener(this)

        binding.scrollRecyclerView.recyclerView.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                // fixme: Need to refine the methodology of this e.g. use setVisibility
                binding.scrollRecyclerView.seekBar.visibility = VISIBLE

                val contentSize = binding.scrollRecyclerView.recyclerView.computeVerticalScrollRange()
                binding.scrollRecyclerView.seekBar.notifyRecyclerViewContentHeightChanged(contentSize)

                val scrollPosition = binding.scrollRecyclerView.recyclerView.computeVerticalScrollOffset()
                binding.scrollRecyclerView.seekBar.notifyRecyclerViewScrollPositionChanged(scrollPosition)

                if (adapter is RecyclerViewScrollbar.ValueLabelListener) {
                    val scrollProportion = scrollPosition.toFloat() / contentSize
                    val activePosition = (scrollProportion * adapter.itemCount).roundToInt()

                    val valueLabelText = (adapter as RecyclerViewScrollbar.ValueLabelListener)
                        .getValueLabelText(activePosition)
                    binding.scrollRecyclerView.seekBar.setValueLabelText(valueLabelText)
                }

                if (dy > 0 && binding.fab.visibility == VISIBLE) binding.fab.hide()
                else if (dy < 0 && binding.fab.visibility != VISIBLE) binding.fab.show()
            }
        })
    }

    override fun onScrollTo(position: Int) {
        // todo: for the library release, refactor position to scrollPercentage (currently scrollToProportion below)
        val maximumScrollPosition = binding.scrollRecyclerView.recyclerView.computeVerticalScrollRange()
        val scrollToProportion = if (position > maximumScrollPosition) 1f
        else position.toFloat() / maximumScrollPosition
        val scrollToPosition = scrollToProportion * adapter.itemCount

        binding.scrollRecyclerView.recyclerView.scrollToPosition(scrollToPosition.roundToInt())
    }

    /**
     * Refresh the content displayed in the RecyclerView.
     *
     * @param songs The up-to-date list of Song objects that should be displayed.
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
            adapter.processNewSongs(songs)
        }

        finishUpdate()
    }

    fun finishUpdate() {
        if (binding.scrollRecyclerView.recyclerView.adapter == null) {
            binding.scrollRecyclerView.recyclerView.adapter = adapter
        }
        setIsUpdatingFalse()
    }
}
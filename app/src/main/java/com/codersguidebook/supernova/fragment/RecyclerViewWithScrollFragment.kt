package com.codersguidebook.supernova.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.viewbinding.ViewBinding
import com.codersguidebook.supernova.databinding.ScrollRecyclerViewBinding
import com.codersguidebook.supernova.views.RecyclerViewScrollbar
import kotlin.math.roundToInt

abstract class RecyclerViewWithScrollFragment: BaseRecyclerViewFragment(), RecyclerViewScrollbar.ScrollbarListener {

    override var _binding: ViewBinding? = null
        get() = field as ScrollRecyclerViewBinding?
    override val binding: ScrollRecyclerViewBinding
        get() = _binding!! as ScrollRecyclerViewBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ScrollRecyclerViewBinding.inflate(inflater, container, false)
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerView.itemAnimator = DefaultItemAnimator()
        binding.scrollBar.setListener(this)

        binding.recyclerView.addOnScrollListener(RecyclerViewScrollbar.OnScrollListener(binding.scrollBar))
    }

    fun finishUpdate() {
        if (binding.recyclerView.adapter == null) {
            binding.recyclerView.adapter = adapter
        }
        setIsUpdatingFalse()
    }

    override fun onScrollTo(position: Int) {
        // todo: for the library release, refactor position to scrollPercentage (currently scrollToProportion below)
        val maximumScrollPosition = binding.recyclerView.computeVerticalScrollRange()
        val scrollToProportion = if (position > maximumScrollPosition) 1f
        else position.toFloat() / maximumScrollPosition
        val scrollToPosition = scrollToProportion * adapter.itemCount

        binding.recyclerView.scrollToPosition(scrollToPosition.roundToInt())
    }
}
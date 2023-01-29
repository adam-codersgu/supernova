package com.codersguidebook.supernova.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.codersguidebook.supernova.databinding.ScrollRecyclerViewBinding
import com.codersguidebook.supernova.views.RecyclerViewScrollbar
import kotlin.math.roundToInt

abstract class RecyclerViewWithScrollFragment: BaseRecyclerViewFragment(), RecyclerViewScrollbar.Listener {

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
        binding.seekBar.setListener(this)

        binding.recyclerView.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val contentSize = binding.recyclerView.computeVerticalScrollRange()
                binding.seekBar.notifyRecyclerViewContentHeightChanged(contentSize)

                val scrollPosition = binding.recyclerView.computeVerticalScrollOffset()
                binding.seekBar.notifyRecyclerViewScrollPositionChanged(scrollPosition)

                if (adapter is RecyclerViewScrollbar.ValueLabelListener) {
                    val scrollProportion = scrollPosition.toFloat() / contentSize
                    val activePosition = (scrollProportion * adapter.itemCount).roundToInt()

                    val valueLabelText = (adapter as RecyclerViewScrollbar.ValueLabelListener)
                        .getValueLabelText(activePosition)
                    binding.seekBar.setValueLabelText(valueLabelText)
                }
            }
        })
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
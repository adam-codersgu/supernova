package com.codersguidebook.supernova.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.codersguidebook.recyclerviewfastscroller.RecyclerViewScrollbar
import com.codersguidebook.supernova.databinding.ScrollRecyclerViewBinding

abstract class RecyclerViewWithScrollFragment: BaseRecyclerViewFragment() {

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

        binding.recyclerView.itemAnimator = getItemAnimatorWithNoChangeAnimation()
        binding.scrollBar.recyclerView = binding.recyclerView

        binding.recyclerView.addOnScrollListener(RecyclerViewScrollbar.OnScrollListener(binding.scrollBar))
    }

    override fun finishUpdate() {
        if (binding.recyclerView.adapter == null) {
            binding.recyclerView.adapter = adapter
        }
        super.finishUpdate()
    }
}
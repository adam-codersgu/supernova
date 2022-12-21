package com.codersguidebook.supernova.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewbinding.ViewBinding
import com.codersguidebook.supernova.databinding.FragmentWithRecyclerViewBinding

abstract class RecyclerViewFragment: BaseRecyclerViewFragment() {

    override var _binding: ViewBinding? = null
        get() = field as FragmentWithRecyclerViewBinding?
    override val binding: FragmentWithRecyclerViewBinding
        get() = _binding!! as FragmentWithRecyclerViewBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWithRecyclerViewBinding.inflate(inflater, container, false)
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.root.layoutManager = LinearLayoutManager(mainActivity)
        binding.root.itemAnimator = DefaultItemAnimator()
    }

    fun finishUpdate() {
        if (binding.root.adapter == null) {
            binding.root.adapter = adapter
        }
        setIsUpdatingFalse()
    }
}
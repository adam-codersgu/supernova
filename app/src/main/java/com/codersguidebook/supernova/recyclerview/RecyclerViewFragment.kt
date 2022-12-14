package com.codersguidebook.supernova.recyclerview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.codersguidebook.supernova.databinding.FragmentWithRecyclerViewBinding

abstract class RecyclerViewFragment: BaseRecyclerViewFragment() {

    override var _binding: ViewBinding? = null
        get() = field as FragmentWithRecyclerViewBinding?

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWithRecyclerViewBinding.inflate(inflater, container, false)
        return super.onCreateView(inflater, container, savedInstanceState)
    }
}
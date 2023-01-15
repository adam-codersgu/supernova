package com.codersguidebook.supernova.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.viewbinding.ViewBinding
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
        initialiseAdapter()
        _binding = ScrollRecyclerViewBinding.inflate(inflater, container, false).apply {
            recyclerView.adapter = adapter
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // fixme: see if you can add layout manager to the xml layout directly?
        // binding.recyclerView.layoutManager = LinearLayoutManager(mainActivity)
        binding.recyclerView.itemAnimator = DefaultItemAnimator()
    }

    fun finishUpdate() {
        if (binding.recyclerView.adapter == null) {
            binding.recyclerView.adapter = adapter
        }
        setIsUpdatingFalse()
    }

    /**
     * Initialise the adapter that should be attached to the RecyclerView.
     *
     * @return An initialised instance of a class that extends the Adapter abstract class.
     */
    // abstract fun getAdapter() : Adapter
}
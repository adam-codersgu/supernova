package com.codersguidebook.supernova.fragment.adapter

import androidx.recyclerview.widget.RecyclerView

abstract class Adapter: RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    /**
     * Convenience method for retrieving the target index of RecyclerView element updates.
     * RecyclerViews that contain a header will need to add + 1 to the index.
     *
     * @param index The index of the target RecyclerView element.
     * @return The index at which updates should be applied, accommodating for any headers.
     * Default: The supplied index.
     */
    open fun getRecyclerViewIndex(index: Int): Int = index
}
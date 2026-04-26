package com.codersguidebook.supernova.fragment.adapter

import android.os.Build
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder

abstract class Adapter: Adapter<ViewHolder>() {

    val items = mutableListOf<Any>()

    /**
     * Convenience method for retrieving the target index of RecyclerView element updates.
     * RecyclerViews that contain a header will need to add + 1 to the index.
     *
     * @param index The index of the target RecyclerView element.
     * @return The index at which updates should be applied, accommodating for any headers.
     * Default: The supplied index.
     */
    open fun getRecyclerViewIndex(index: Int): Int = index

    fun processNewItems(newItems: List<Any>) {
        for ((index, item) in newItems.withIndex()) {
            when {
                items.isEmpty() -> {
                    items.addAll(newItems)
                    notifyItemRangeInserted(0, newItems.size)
                }
                index >= items.size -> {
                    items.add(item)
                    notifyItemInserted(index)
                }
                itemsNotEqual(item, index) -> {
                    if (itemDoesNotExist(item)) {
                        items.add(index, item)
                        notifyItemInserted(index)
                        continue
                    } else {
                        var numberOfItemsRemoved = 0
                        do {
                            items.removeAt(index)
                            ++numberOfItemsRemoved
                        } while (index < items.size && itemsNotEqual(item, index))

                        when {
                            numberOfItemsRemoved == 1 -> notifyItemRemoved(index)
                            numberOfItemsRemoved > 1 -> notifyItemRangeRemoved(index,
                                numberOfItemsRemoved)
                        }
                    }
                }
                itemShouldBeUpdated(item, index) -> {
                    items[index] = item
                    notifyItemChanged(index)
                }
            }
        }

        if (items.size > newItems.size) {
            val numberItemsToRemove = items.size - newItems.size
            repeat(numberItemsToRemove) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                    items.removeLast()
                } else {
                    items.removeAt(items.size - 1)
                }
            }
            notifyItemRangeRemoved(newItems.size, numberItemsToRemove)
        }
    }

    abstract fun itemsEqual(item: Any, index: Int): Boolean

    abstract fun findItem(item: Any): Any?

    open fun itemShouldBeUpdated(item: Any, index: Int): Boolean {
        return false
    }

    private fun itemDoesNotExist(item: Any): Boolean {
        return findItem(item) == null
    }

    private fun itemsNotEqual(item: Any, index: Int): Boolean {
        return !itemsEqual(item, index)
    }
}
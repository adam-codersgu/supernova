package com.codersguidebook.supernova.fragment.adapter

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
        if (items == newItems) return

        for ((index, item) in newItems.withIndex()) {
            val recyclerViewIndex = getRecyclerViewIndex(index)
            when {
                items.isEmpty() -> {
                    items.addAll(newItems)
                    notifyItemRangeInserted(0, newItems.size)
                }
                index >= items.size -> {
                    items.add(item)
                    notifyItemInserted(recyclerViewIndex)
                }
                itemsNotEqual(item, index) -> {
                    if (itemDoesNotExist(item)) {
                        items.add(index, item)
                        notifyItemInserted(recyclerViewIndex)
                    } else {
                        val oldIndex = findItemIndex(item)
                        if (oldIndex != -1) {
                            items.removeAt(oldIndex)
                            items.add(index, item)
                            notifyItemMoved(recyclerViewIndex, getRecyclerViewIndex(index))
                        } else {
                            items.removeAt(index)
                            items.add(index, item)
                            notifyItemChanged(recyclerViewIndex, getRecyclerViewIndex(index))
                        }
                    }
                }
                itemShouldBeUpdated(item, index) -> {
                    items[index] = item
                    notifyItemChanged(recyclerViewIndex)
                }
            }
        }

        if (items.size > newItems.size) {
            val numberItemsToRemove = items.size - newItems.size
            repeat(numberItemsToRemove) {
                removeItem(items.size - 1)
            }

            if (numberItemsToRemove == 1) {
                notifyItemRemoved(getRecyclerViewIndex(newItems.size))
            } else {
                notifyItemRangeRemoved(getRecyclerViewIndex(newItems.size), numberItemsToRemove)
            }
        }
    }

    private fun itemsEqual(item: Any, index: Int): Boolean {
        return itemsEqual(item, items[index])
    }

    private fun itemDoesNotExist(item: Any): Boolean {
        return findItem(item) == null
    }

    private fun itemsNotEqual(item: Any, index: Int): Boolean {
        return !itemsEqual(item, index)
    }

    private fun findItem(item: Any): Any? {
        return items.find {
            i -> itemsEqual(i, item)
        }
    }

    private fun findItemIndex(item: Any): Int {
        return items.indexOfFirst {
            i -> itemsEqual(i, item)
        }
    }

    abstract fun itemsEqual(item1: Any, item2: Any): Boolean

    open fun itemShouldBeUpdated(item: Any, index: Int): Boolean {
        return false
    }

    open fun removeItem(index: Int) {
        items.removeAt(index)
    }

    override fun getItemCount() = items.size
}
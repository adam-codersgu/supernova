package com.codersguidebook.supernova.recyclerview.layoutmanager

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class WrapContentLinearLayoutManager(context: Context, orientation: Int, reverseLayout: Boolean) : LinearLayoutManager(context, orientation, reverseLayout) {
    override fun onLayoutChildren(recycler: RecyclerView.Recycler, state: RecyclerView.State) {
        try {
            super.onLayoutChildren(recycler, state)
        } catch (_: IndexOutOfBoundsException) { }
    }
}
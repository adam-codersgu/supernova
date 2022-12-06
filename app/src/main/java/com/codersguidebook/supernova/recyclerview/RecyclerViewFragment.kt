package com.codersguidebook.supernova.recyclerview

import androidx.fragment.app.Fragment
import com.codersguidebook.supernova.MainActivity
import com.codersguidebook.supernova.entities.Song

abstract class RecyclerViewFragment: Fragment() {

    var isUpdating = false
    var unhandledRequestReceived = false
    lateinit var mainActivity: MainActivity

    /**
     * Refresh the content displayed in the RecyclerView.
     *
     * @param songs - The up-to-date list of Song objects that should be displayed.
     */
    open fun updateRecyclerView(songs: List<Song>) {
        if (isUpdating) {
            unhandledRequestReceived = true
            return
        }
        isUpdating = true
    }

    /**
     * Simultaneous requests to update the RecyclerView are not permitted. If such a request is received,
     * then the RecyclerView will only be updated once the previous request has completed. To facilitate
     * this, we must request up-to-date data via requestNewData() when a postponed update is initiated.
     */
    abstract fun requestNewData()

    /**
     * Configure the fragment's menu.
     *
     * @param songs - A list of Song objects to be used for certain menu actions.
     */
    abstract fun setupMenu(songs: List<Song>)
}
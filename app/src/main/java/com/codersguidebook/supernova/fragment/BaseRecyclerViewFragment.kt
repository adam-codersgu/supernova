package com.codersguidebook.supernova.fragment

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.RecyclerView

abstract class BaseRecyclerViewFragment: BaseFragment() {

    abstract val adapter: RecyclerView.Adapter<RecyclerView.ViewHolder>
    var isUpdating = false
    var unhandledRequestReceived = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initialiseAdapter()
    }

    /** Convenience method that sets the isUpdating state of the RecyclerView update workflow to true **/
    fun setIsUpdatingTrue() {
        if (isUpdating) {
            unhandledRequestReceived = true
            return
        }
        isUpdating = true
    }

    /** Convenience method that sets the isUpdating state of the RecyclerView update workflow to false **/
    fun setIsUpdatingFalse() {
        isUpdating = false
        if (unhandledRequestReceived) {
            unhandledRequestReceived = false
            requestNewData()
        }
    }

    /**
     * Simultaneous requests to update the RecyclerView are not permitted. If such a request is received,
     * then the RecyclerView will only be updated once the previous request has completed. To facilitate
     * this, we must request up-to-date data via requestNewData() when a postponed update is initiated.
     */
    abstract fun requestNewData()

    /** Each fragment that uses a RecyclerView must initialise the adapter variable. */
    abstract fun initialiseAdapter()
}
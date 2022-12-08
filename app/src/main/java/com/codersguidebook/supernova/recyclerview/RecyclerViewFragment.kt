package com.codersguidebook.supernova.recyclerview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.codersguidebook.supernova.MainActivity
import com.codersguidebook.supernova.entities.Song

abstract class RecyclerViewFragment: Fragment() {

    abstract val adapter: RecyclerView.Adapter<RecyclerView.ViewHolder>
    abstract val binding: ViewBinding
    var fragmentBinding: ViewBinding? = null
    var isUpdating = false
    var unhandledRequestReceived = false
    lateinit var mainActivity: MainActivity

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        mainActivity = activity as MainActivity
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initialiseAdapter()
    }

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

    /** Each fragment that uses a RecyclerView must initialise the adapter variable. */
    abstract fun initialiseAdapter()

    override fun onDestroyView() {
        super.onDestroyView()
        fragmentBinding = null
    }
}
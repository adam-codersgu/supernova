package com.codersguidebook.supernova.ui.playQueue

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.media3.common.MediaItem
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.ACTION_STATE_DRAG
import androidx.recyclerview.widget.ItemTouchHelper.DOWN
import androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback
import androidx.recyclerview.widget.ItemTouchHelper.UP
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.codersguidebook.supernova.PlayQueueViewModel
import com.codersguidebook.supernova.R
import com.codersguidebook.supernova.dialogs.CreatePlaylist
import com.codersguidebook.supernova.fragment.RecyclerViewFragment
import com.codersguidebook.supernova.fragment.adapter.PlayQueueAdapter

class PlayQueueFragment : RecyclerViewFragment() {
    private val playQueueViewModel: PlayQueueViewModel by activityViewModels()
    override lateinit var adapter: PlayQueueAdapter

    private val itemTouchHelper by lazy {
        val simpleItemTouchCallback = object : SimpleCallback(UP or DOWN, 0) {
            var from: Int? = null
            var to: Int? = null

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)

                if (actionState == ACTION_STATE_DRAG) {
                    from = viewHolder?.layoutPosition
                    viewHolder?.itemView?.alpha = 0.5f
                }
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)

                viewHolder.itemView.alpha = 1.0f

                if (from != null && to != null) {
                    mainActivity.notifyQueueItemMoved(from!!, to!!)

                    if (adapter.currentlyPlayingQueueIndex in from!!..to!!) {
                        Log.i("DEBUG", "The currently playing index ${adapter.currentlyPlayingQueueIndex}" +
                                " is in the range $from to $to.")
                        if (from!! == adapter.currentlyPlayingQueueIndex) {
                            adapter.changeCurrentlyPlayingQueueItemIndex(to!!)
                        } else if (from!! > to!!) {
                            adapter.changeCurrentlyPlayingQueueItemIndex(adapter.currentlyPlayingQueueIndex + 1)
                        } else {
                            adapter.changeCurrentlyPlayingQueueItemIndex(adapter.currentlyPlayingQueueIndex - 1)
                        }
                    }
                    from = null
                    to = null
                }
            }

            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                val temporaryFrom = viewHolder.layoutPosition
                to = target.layoutPosition
                if (temporaryFrom != to) {
                    val queueItem = adapter.items[temporaryFrom]
                    adapter.items.removeAt(temporaryFrom)
                    adapter.items.add(to!!, queueItem)
                    adapter.notifyItemMoved(temporaryFrom, to!!)
                }

                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) { }
        }
        ItemTouchHelper(simpleItemTouchCallback)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMenu()

        playQueueViewModel.playQueue.observe(viewLifecycleOwner) {
            updateRecyclerView(it)
        }

        playQueueViewModel.currentQueueItemIndex.observe(viewLifecycleOwner) { position ->
            position?.let { adapter.changeCurrentlyPlayingQueueItemIndex(it) }
        }

        itemTouchHelper.attachToRecyclerView(binding.root)
    }

    // FIXME - This value should be updated in the view model when items are removed
    //  from the play queue prior to the currently playing index
    fun updateCurrentlyPlayingIndex() {
        val index = playQueueViewModel.currentQueueItemIndex.value ?: return
        adapter.changeCurrentlyPlayingQueueItemIndex(index)
    }

    override fun initialiseAdapter() {
        adapter = PlayQueueAdapter(this, mainActivity)
    }

    override fun requestNewData() {
        playQueueViewModel.playQueue.value?.let { updateRecyclerView(it) }
    }

    private fun updateRecyclerView(playQueue: List<MediaItem>) {
        setIsUpdatingTrue()

        adapter.processNewItems(playQueue)

        finishUpdate()
    }

    override fun setupMenu() {
        (requireActivity() as MenuHost).addMenuProvider(object : MenuProvider {
            override fun onPrepareMenu(menu: Menu) {
                menu.findItem(R.id.savePlayQueue).isVisible = true
            }

            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) { }

            @Suppress("UNCHECKED_CAST")
            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.savePlayQueue -> {
                        val songIds = mutableListOf<Long>()
                        for (queueItem in (adapter.items as List<MediaItem>)) {
                            songIds.add(queueItem.mediaId.toLong())
                        }
                        if (songIds.isNotEmpty()) mainActivity.openDialog(CreatePlaylist(songIds))
                        else Toast.makeText(mainActivity, getString(R.string.empty_play_queue), Toast.LENGTH_SHORT).show()

                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    override fun onResume() {
        super.onResume()

        if (adapter.currentlyPlayingQueueIndex != -1) {
            (binding.root.layoutManager as LinearLayoutManager)
                .scrollToPositionWithOffset(adapter.currentlyPlayingQueueIndex, 0)
        }
    }

    fun startDragging(viewHolder: RecyclerView.ViewHolder) = itemTouchHelper.startDrag(viewHolder)
}
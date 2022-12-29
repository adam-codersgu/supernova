package com.codersguidebook.supernova.ui.playQueue

import android.os.Bundle
import android.support.v4.media.session.MediaSessionCompat.QueueItem
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.codersguidebook.supernova.CreatePlaylist
import com.codersguidebook.supernova.PlayQueueViewModel
import com.codersguidebook.supernova.R
import com.codersguidebook.supernova.fragment.RecyclerViewFragment
import com.codersguidebook.supernova.fragment.adapter.PlayQueueAdapter

class PlayQueueFragment : RecyclerViewFragment() {
    private val playQueueViewModel: PlayQueueViewModel by activityViewModels()
    override lateinit var adapter: PlayQueueAdapter

    private val itemTouchHelper by lazy {
        val simpleItemTouchCallback = object : SimpleCallback(UP or DOWN, 0) {
            var to: Int? = null
            var queueItem: QueueItem? = null

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)

                if (actionState == ACTION_STATE_DRAG) viewHolder?.itemView?.alpha = 0.5f
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)

                viewHolder.itemView.alpha = 1.0f

                if (to != null && queueItem != null) {
                    playQueueViewModel.playQueue.value = adapter.playQueue
                    mainActivity.notifyQueueItemMoved(queueItem!!.queueId, to!!)
                    to = null
                    queueItem = null
                }
            }

            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                val from = viewHolder.layoutPosition
                to = target.layoutPosition
                if (from != to) {
                    queueItem = adapter.playQueue[from]
                    adapter.playQueue.removeAt(from)
                    adapter.playQueue.add(to!!, queueItem!!)
                    adapter.notifyItemMoved(from, to!!)
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
            updateRecyclerViewWithPlayQueue(it)
        }

        playQueueViewModel.currentQueueItemId.observe(viewLifecycleOwner) { position ->
            position?.let { adapter.changeCurrentlyPlayingQueueItemId(it) }
        }

        itemTouchHelper.attachToRecyclerView(binding.root)
    }

    override fun initialiseAdapter() {
        adapter = PlayQueueAdapter(this, mainActivity)
    }

    override fun requestNewData() {
        playQueueViewModel.playQueue.value?.let { updateRecyclerViewWithPlayQueue(it) }
    }

    private fun updateRecyclerViewWithPlayQueue(playQueue: List<QueueItem>) {
        setIsUpdatingTrue()

        if (adapter.playQueue.isEmpty()) {
            adapter.playQueue.addAll(playQueue)
            adapter.notifyItemRangeInserted(0, playQueue.size)
        } else {
            for ((index, queueItem) in playQueue.withIndex()) {
                adapter.processLoopIteration(index, queueItem)
            }

            if (adapter.playQueue.size > playQueue.size) {
                val numberItemsToRemove = adapter.playQueue.size - playQueue.size
                repeat(numberItemsToRemove) { adapter.playQueue.removeLast() }
                adapter.notifyItemRangeRemoved(playQueue.size, numberItemsToRemove)
            }
        }

        finishUpdate()
    }

    private fun setupMenu() {
        (requireActivity() as MenuHost).addMenuProvider(object : MenuProvider {
            override fun onPrepareMenu(menu: Menu) {
                menu.findItem(R.id.savePlayQueue).isVisible = true
            }

            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) { }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.savePlayQueue -> {
                        val songIds = mutableListOf<Long>()
                        for (queueItem in adapter.playQueue) {
                            songIds.add(queueItem.description.mediaId?.toLong() ?: continue)
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

        // This code finds the position in the recycler view list of the currently playing song,
        // and scrolls to it.
        val currentlyPlayingQueueIndex = adapter.playQueue.indexOfFirst { queueItem ->
            queueItem.queueId == adapter.currentlyPlayingQueueId
        }

        if (currentlyPlayingQueueIndex != -1) {
            (binding.root.layoutManager as LinearLayoutManager)
                .scrollToPositionWithOffset(currentlyPlayingQueueIndex, 0)
        }
    }

    fun startDragging(viewHolder: RecyclerView.ViewHolder) = itemTouchHelper.startDrag(viewHolder)
}
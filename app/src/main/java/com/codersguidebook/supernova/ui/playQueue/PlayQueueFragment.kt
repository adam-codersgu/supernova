package com.codersguidebook.supernova.ui.playQueue

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.codersguidebook.supernova.CreatePlaylist
import com.codersguidebook.supernova.MainActivity
import com.codersguidebook.supernova.PlayQueueViewModel
import com.codersguidebook.supernova.R
import com.codersguidebook.supernova.databinding.FragmentWithRecyclerViewBinding

// TODO: Need to instruct the play queue views to skip to play queue items based on their ID not index
class PlayQueueFragment : Fragment() {
    private var _binding: FragmentWithRecyclerViewBinding? = null
    private val binding get() = _binding!!
    private val playQueueViewModel: PlayQueueViewModel by activityViewModels()
    private lateinit var callingActivity: MainActivity
    private lateinit var playQueueAdapter: PlayQueueAdapter

    private val itemTouchHelper by lazy {
        val simpleItemTouchCallback = object : SimpleCallback(UP or DOWN, 0) {
            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)

                if (actionState == ACTION_STATE_DRAG) viewHolder?.itemView?.alpha = 0.5f
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)

                viewHolder.itemView.alpha = 1.0f
                playQueueViewModel.currentPlayQueue.value = playQueueAdapter.playQueue
            }

            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                val from = viewHolder.layoutPosition
                val to = target.layoutPosition
                if (from != to) {
                    val song = playQueueAdapter.playQueue[from]
                    playQueueAdapter.playQueue.removeAt(from)
                    playQueueAdapter.playQueue.add(to, song)
                    playQueueAdapter.notifyItemMoved(from, to)
                }

                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) { }
        }
        ItemTouchHelper(simpleItemTouchCallback)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        _binding = FragmentWithRecyclerViewBinding.inflate(inflater, container, false)
        callingActivity = activity as MainActivity

        return binding.root
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMenu()

        playQueueAdapter = PlayQueueAdapter(this, callingActivity)
        binding.root.layoutManager = LinearLayoutManager(activity)
        binding.root.itemAnimator = DefaultItemAnimator()
        binding.root.adapter = playQueueAdapter

        playQueueViewModel.currentPlayQueue.observe(viewLifecycleOwner) { queue ->
            queue?.let {
                if (playQueueAdapter.playQueue.size > it.size) {
                    for (queueItem in playQueueAdapter.playQueue) {
                        val matchingQueueItem = it.firstOrNull { item ->
                            queueItem.queueId == item.queueId
                        }
                        // The QueueItem is no longer present in the new play queue
                        if (matchingQueueItem == null) {
                            playQueueAdapter.removeQueueItemById(queueItem.queueId)
                        }
                    }
                } else {
                    // Adapter loaded for first time or play queue shuffled
                    playQueueAdapter.playQueue = it.toMutableList()
                    playQueueAdapter.notifyDataSetChanged()
                }
            }
        }

        playQueueViewModel.currentQueueItemId.observe(viewLifecycleOwner) { position ->
            position?.let { playQueueAdapter.changeCurrentlyPlayingQueueItemId(it) }
        }

        itemTouchHelper.attachToRecyclerView(binding.root)
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
                        for (queueItem in playQueueAdapter.playQueue) {
                            songIds.add(queueItem.description.mediaId?.toLong() ?: continue)
                        }

                        if (songIds.isNotEmpty()) callingActivity.openDialog(CreatePlaylist(songIds))
                        else Toast.makeText(callingActivity, "The play queue is empty.", Toast.LENGTH_SHORT).show()

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
        val currentlyPlayingQueueIndex = playQueueAdapter.playQueue.indexOfFirst { queueItem ->
            queueItem.queueId == playQueueAdapter.currentlyPlayingQueueId
        }

        if (currentlyPlayingQueueIndex != -1) {
            (binding.root.layoutManager as LinearLayoutManager)
                .scrollToPositionWithOffset(currentlyPlayingQueueIndex, 0)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun startDragging(viewHolder: RecyclerView.ViewHolder) = itemTouchHelper.startDrag(viewHolder)
}
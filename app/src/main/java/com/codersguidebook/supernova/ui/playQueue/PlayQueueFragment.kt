package com.codersguidebook.supernova.ui.playQueue

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.codersguidebook.supernova.*
import com.codersguidebook.supernova.databinding.FragmentWithRecyclerViewBinding
import com.codersguidebook.supernova.entities.Song

class PlayQueueFragment : Fragment() {
    private var _binding: FragmentWithRecyclerViewBinding? = null
    private val binding get() = _binding!!
    private val playQueueViewModel: PlayQueueViewModel by activityViewModels()
    private lateinit var callingActivity: MainActivity
    private lateinit var playQueueAdapter: PlayQueueAdapter
    private val itemTouchHelper by lazy {
        val simpleItemTouchCallback =
            object : ItemTouchHelper.SimpleCallback(UP or DOWN, 0) {
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

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
            }
        ItemTouchHelper(simpleItemTouchCallback)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWithRecyclerViewBinding.inflate(inflater, container, false)
        callingActivity = activity as MainActivity
        setHasOptionsMenu(true)

        itemTouchHelper.attachToRecyclerView(binding.root)
        playQueueAdapter = PlayQueueAdapter(this, callingActivity)
        binding.root.layoutManager = LinearLayoutManager(activity)
        binding.root.itemAnimator = DefaultItemAnimator()
        binding.root.adapter = playQueueAdapter

        playQueueViewModel.currentPlayQueue.observe(viewLifecycleOwner, { queue ->
            queue?.let {
                if (playQueueAdapter.playQueue.size > it.size) {
                    // Songs removed from play queue
                    val difference = playQueueAdapter.playQueue - it
                    for (item in difference) {
                        val index = playQueueAdapter.playQueue.indexOfFirst {queueItem ->
                            queueItem.queueID == item.queueID
                        }
                        if (index != -1) {
                            playQueueAdapter.playQueue.removeAt(index)
                            playQueueAdapter.notifyItemRemoved(index)
                        }
                    }
                } else {
                    // Adapter loaded for first time or play queue shuffled
                    playQueueAdapter.playQueue = it.toMutableList()
                    playQueueAdapter.notifyDataSetChanged()
                }
            }
        })

        playQueueViewModel.currentlyPlayingQueueID.observe(viewLifecycleOwner, { position ->
            position?.let { playQueueAdapter.currentlyPlayingSongChanged(it) }
        })

        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.findItem(R.id.savePlayQueue).isVisible = true

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.savePlayQueue -> {
                val songs = mutableListOf<Song>()
                for (i in playQueueAdapter.playQueue) songs.add(i.song)

                if (songs.isNotEmpty()) callingActivity.openDialog(CreatePlaylist(songs))
                else Toast.makeText(callingActivity, "The play queue is empty.", Toast.LENGTH_SHORT).show()

                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()

        // This code finds the position in the recycler view list of the currently playing song, and scrolls to it
        val currentlyPlayingQueueIndex = playQueueAdapter.playQueue.indexOfFirst {queueItem ->
            queueItem.queueID == playQueueAdapter.currentlyPlayingQueueID
        }

        if (currentlyPlayingQueueIndex != -1) (binding.root.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(currentlyPlayingQueueIndex, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun startDragging(viewHolder: RecyclerView.ViewHolder) = itemTouchHelper.startDrag(viewHolder)
}
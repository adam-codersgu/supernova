package com.codersguidebook.supernova.ui.playlist

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.codersguidebook.supernova.*
import com.codersguidebook.supernova.databinding.FragmentWithFabBinding
import com.codersguidebook.supernova.entities.Playlist
import com.codersguidebook.supernova.entities.Song

class PlaylistFragment : Fragment() {

    private var _binding: FragmentWithFabBinding? = null
    private val binding get() = _binding!!
    private var playlistName: String? = null
    private var playlist: Playlist? = null
    private var playlistSongs= mutableListOf<Song>()
    private lateinit var callingActivity: MainActivity
    private lateinit var reorderPlaylist: MenuItem
    private lateinit var finishedReorder: MenuItem
    private lateinit var playlistAdapter: PlaylistAdapter
    private val itemTouchHelper by lazy {
        val simpleItemTouchCallback =
            object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
                override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?,
                                               actionState: Int) {
                    super.onSelectedChanged(viewHolder, actionState)

                    if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) viewHolder?.itemView?.alpha = 0.5f
                }
                override fun clearView(recyclerView: RecyclerView,
                                       viewHolder: RecyclerView.ViewHolder) {
                    super.clearView(recyclerView, viewHolder)

                    viewHolder.itemView.alpha = 1.0f
                    if (playlist != null) callingActivity.savePlaylistNewSongList(playlist!!, playlistAdapter.songs)
                }
                override fun onMove(recyclerView: RecyclerView,
                                    viewHolder: RecyclerView.ViewHolder,
                                    target: RecyclerView.ViewHolder): Boolean {

                    val from = viewHolder.layoutPosition
                    val to = target.layoutPosition
                    if (from != to && from != 0 && to != 0) {
                        val song = playlistAdapter.songs[from - 1]
                        playlistAdapter.songs.removeAt(from - 1)
                        playlistAdapter.songs.add(to - 1, song)
                        playlistAdapter.notifyItemMoved(from, to)
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
        arguments?.let {
            val safeArgs = PlaylistFragmentArgs.fromBundle(it)
            playlistName = safeArgs.playlistName
        }
        _binding = FragmentWithFabBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)
        
        callingActivity = activity as MainActivity
        playlistAdapter = PlaylistAdapter(this, callingActivity)
        binding.recyclerView.layoutManager = LinearLayoutManager(activity)
        binding.recyclerView.itemAnimator = DefaultItemAnimator()
        binding.recyclerView.adapter = playlistAdapter

        val musicDatabase = MusicDatabase.getDatabase(requireContext(), lifecycleScope)
        musicDatabase.playlistDao().findPlaylist(playlistName ?: "").observe(viewLifecycleOwner, { p ->
            p?.let {
                playlist = it
                if (playlistAdapter.playlist == null) playlistAdapter.playlist = it
                val newSongs = callingActivity.extractPlaylistSongs(it.songs)
                playlistSongs = newSongs
                if (newSongs.isEmpty()) {
                    playlistAdapter.songs = mutableListOf()
                    playlistAdapter.notifyDataSetChanged()
                } else playlistAdapter.processSongs(newSongs)
            }
        })

        binding.fab.setOnClickListener {
            callingActivity.playNewSongs(playlistSongs, 0, true)
        }

        binding.recyclerView.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0 && binding.fab.visibility == View.VISIBLE) binding.fab.hide()
                else if (dy < 0 && binding.fab.visibility != View.VISIBLE) binding.fab.show()
            }
        })

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun openDialog(songs: MutableList<Song>, position: Int, playlist: Playlist) {
        val dialog = PlaylistSongOptions(songs, position, playlist)
        dialog.show(childFragmentManager, "")
    }

    fun startDragging(viewHolder: RecyclerView.ViewHolder) = itemTouchHelper.startDrag(viewHolder)

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.setGroupVisible(R.id.universal_playlist_actions, true)

        if (playlistName != getString(R.string.most_played) && playlistName != getString(R.string.recently_played) && playlistName != getString(R.string.favourites) && playlistName != getString(R.string.song_day)){
            menu.setGroupVisible(R.id.user_playlist_actions, true)
            reorderPlaylist = menu.findItem(R.id.reorderPlaylist)
            finishedReorder = menu.findItem(R.id.done)
        }

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.playPlaylistNext -> {
                if (playlistSongs.isNotEmpty()){
                    callingActivity.addSongsToPlayQueue(playlistSongs, false)
                } else Toast.makeText(activity, "There are no songs in that playlist.", Toast.LENGTH_SHORT).show()
            }
            R.id.queuePlaylist -> {
                if (playlistSongs.isNotEmpty()) callingActivity.addSongsToPlayQueue(playlistSongs, true)
                else Toast.makeText(activity, "There are no songs in that playlist.", Toast.LENGTH_SHORT).show()
            }
            R.id.reorderPlaylist -> {
                itemTouchHelper.attachToRecyclerView(binding.recyclerView)
                playlistAdapter.manageHandles(true)
                reorderPlaylist.isVisible = false
                finishedReorder.isVisible = true
            }
            R.id.editPlaylist -> {
                if (playlistName != null) {
                    val action = PlaylistFragmentDirections.actionEditPlaylist(playlistName!!)
                    callingActivity.findNavController(R.id.nav_host_fragment).navigate(action)
                }
            }
            R.id.done -> {
                // null essentially removes the itemTouchHelper from the recycler view
                itemTouchHelper.attachToRecyclerView(null)
                playlistAdapter.manageHandles(false)
                reorderPlaylist.isVisible = true
                finishedReorder.isVisible = false
            }
        }
        return super.onOptionsItemSelected(item)
    }
}

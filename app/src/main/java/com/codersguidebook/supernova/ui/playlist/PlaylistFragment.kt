package com.codersguidebook.supernova.ui.playlist

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.codersguidebook.supernova.MusicDatabase
import com.codersguidebook.supernova.MusicLibraryViewModel
import com.codersguidebook.supernova.R
import com.codersguidebook.supernova.entities.Playlist
import com.codersguidebook.supernova.entities.Song
import com.codersguidebook.supernova.fragment.RecyclerViewWithFabFragment
import com.codersguidebook.supernova.fragment.adapter.PlaylistAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlaylistFragment : RecyclerViewWithFabFragment() {

    private var playlistName: String? = null
    private var playlist: Playlist? = null
    private lateinit var reorderPlaylist: MenuItem
    private lateinit var finishedReorder: MenuItem
    private lateinit var musicDatabase: MusicDatabase
    private lateinit var musicLibraryViewModel: MusicLibraryViewModel

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
                    playlist?.let {
                        val songIds = adapter.songs.map { song -> song.songId }
                        musicLibraryViewModel.savePlaylistWithSongIds(it, songIds)
                    }
                }
                override fun onMove(recyclerView: RecyclerView,
                                    viewHolder: RecyclerView.ViewHolder,
                                    target: RecyclerView.ViewHolder): Boolean {

                    val from = viewHolder.layoutPosition
                    val to = target.layoutPosition
                    if (from != to && from != 0 && to != 0) {
                        val song = adapter.songs[from - 1]
                        adapter.songs.removeAt(from - 1)
                        adapter.songs.add(to - 1, song)
                        adapter.notifyItemMoved(from, to)
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

        musicLibraryViewModel = ViewModelProvider(this)[MusicLibraryViewModel::class.java]

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        playlistName?.let { playlistName ->
            musicDatabase = MusicDatabase.getDatabase(mainActivity, lifecycleScope)
            musicDatabase.playlistDao().findPlaylist(playlistName).observe(viewLifecycleOwner) {
                lifecycleScope.launch(Dispatchers.Main) {
                    playlist = it
                    (adapter as PlaylistAdapter).playlist = it
                    val songs = withContext(Dispatchers.IO) {
                        musicLibraryViewModel.extractPlaylistSongs(it?.songs)
                    }
                    updateRecyclerView(songs)
                }
            }
        }
    }

    fun startDragging(viewHolder: RecyclerView.ViewHolder) = itemTouchHelper.startDrag(viewHolder)

    override fun updateRecyclerView(songs: List<Song>) {
        super.updateRecyclerView(songs)
        setupMenu(songs)
    }

    override fun initialiseAdapter() {
        adapter = PlaylistAdapter(this, mainActivity)
    }

    override fun requestNewData() {
        // TODO: For areas of the codebase like this, can we use a view model method that itself finds the playlist
        //  and extracts their songs in one go? This would save the coroutine code duplication
        musicDatabase.playlistDao().findPlaylist(playlistName ?: return).value?.let {
            lifecycleScope.launch(Dispatchers.Main) {
                val songs = withContext(Dispatchers.IO) {
                    musicLibraryViewModel.extractPlaylistSongs(it.songs)
                }
                updateRecyclerView(songs)
            }
        }
    }

    private fun setupMenu(songs: List<Song>) {
        (requireActivity() as MenuHost).addMenuProvider(object : MenuProvider {
            override fun onPrepareMenu(menu: Menu) {
                menu.setGroupVisible(R.id.universal_playlist_actions, true)
                if (playlistName != getString(R.string.most_played) && 
                    playlistName != getString(R.string.recently_played) && 
                    playlistName != getString(R.string.favourites) && 
                    playlistName != getString(R.string.song_day)) {
                    menu.setGroupVisible(R.id.user_playlist_actions, true)
                    reorderPlaylist = menu.findItem(R.id.reorderPlaylist)
                    finishedReorder = menu.findItem(R.id.done)
                }
            }

            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) { }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                when (menuItem.itemId) {
                    R.id.playPlaylistNext -> {
                        if (songs.isNotEmpty()){
                            mainActivity.addSongsToPlayQueue(songs, true)
                        } else {
                            Toast.makeText(activity,
                                getString(R.string.playlist_contains_zero_songs), Toast.LENGTH_SHORT).show()
                        }
                    }
                    R.id.queuePlaylist -> {
                        if (songs.isNotEmpty()) mainActivity.addSongsToPlayQueue(songs)
                        else Toast.makeText(activity,
                            getString(R.string.playlist_contains_zero_songs), Toast.LENGTH_SHORT).show()
                    }
                    R.id.reorderPlaylist -> {
                        itemTouchHelper.attachToRecyclerView(binding.scrollRecyclerView.recyclerView)
                        (adapter as PlaylistAdapter).manageHandles(true)
                        reorderPlaylist.isVisible = false
                        finishedReorder.isVisible = true
                    }
                    R.id.editPlaylist -> {
                        playlistName?.let {
                            val action = PlaylistFragmentDirections.actionEditPlaylist(playlistName!!)
                            mainActivity.findNavController(R.id.nav_host_fragment).navigate(action)
                        }
                    }
                    R.id.done -> {
                        // null essentially removes the itemTouchHelper from the recycler view
                        itemTouchHelper.attachToRecyclerView(null)
                        (adapter as PlaylistAdapter).manageHandles(false)
                        reorderPlaylist.isVisible = true
                        finishedReorder.isVisible = false
                    }
                    else -> return false
                }
                return true
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }
}

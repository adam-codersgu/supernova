package com.codersguidebook.supernova.dialogs

import android.app.Dialog
import android.os.Bundle
import android.widget.Toast
import androidx.core.view.isGone
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.viewbinding.ViewBinding
import com.codersguidebook.supernova.R
import com.codersguidebook.supernova.databinding.PlaylistSongOptionsBinding
import com.codersguidebook.supernova.entities.Playlist
import com.codersguidebook.supernova.entities.Song
import com.codersguidebook.supernova.fragment.BaseDialogFragment
import com.codersguidebook.supernova.ui.albums.AlbumsFragmentDirections
import com.codersguidebook.supernova.ui.artists.ArtistsFragmentDirections
import com.codersguidebook.supernova.ui.songs.SongsFragmentDirections
import com.codersguidebook.supernova.utils.PlaylistHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlaylistSongOptions(private val song: Song,
                          private val position: Int,
                          private val playlist: Playlist
) : BaseDialogFragment() {

    override var _binding: ViewBinding? = null
        get() = field as PlaylistSongOptionsBinding?
    override val binding: PlaylistSongOptionsBinding
        get() = _binding!! as PlaylistSongOptionsBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = PlaylistSongOptionsBinding.inflate(inflater)

        binding.optionsTitle.text = song.title

        binding.playNext.setOnClickListener {
            mainActivity.addSongsToPlayQueue(listOf(song), true)
            dismiss()
        }

        binding.addPlayQueue.setOnClickListener {
            mainActivity.addSongsToPlayQueue(listOf(song))
            dismiss()
        }

        if (song.isFavourite) binding.favourite.text = getString(R.string.remove_favourites)

        binding.favourite.setOnClickListener {
            lifecycleScope.launch(Dispatchers.Main) {
                val isFavourite = withContext(Dispatchers.IO) {
                    musicLibraryViewModel.toggleSongFavouriteStatus(song)
                }
                if (isFavourite == true) {
                    Toast.makeText(mainActivity, getString(R.string.added_to_favourites),
                        Toast.LENGTH_SHORT).show()
                } else if (isFavourite == false)  {
                    Toast.makeText(mainActivity, getString(R.string.removed_from_favourites),
                        Toast.LENGTH_SHORT).show()
                }
                dismiss()
            }
        }

        binding.artist.setOnClickListener {
            val action = ArtistsFragmentDirections.actionSelectArtist(song.artist)
            mainActivity.findNavController(R.id.nav_host_fragment).navigate(action)
            dismiss()
        }

        binding.album.setOnClickListener {
            val action = AlbumsFragmentDirections.actionSelectAlbum(song.albumId)
            mainActivity.findNavController(R.id.nav_host_fragment).navigate(action)
            dismiss()
        }

        binding.addPlaylist.setOnClickListener {
            mainActivity.openAddToPlaylistDialog(listOf(song))
            dismiss()
        }

        binding.editSong.setOnClickListener {
            val action = SongsFragmentDirections.actionEditSong(song)
            mainActivity.findNavController(R.id.nav_host_fragment).navigate(action)
            dismiss()
        }

        if (playlist.name == getString(R.string.most_played) || playlist.name == getString(R.string.favourites)) {
            binding.removeSong.isGone = true
        } else {
            binding.removeSong.setOnClickListener {
                val songIds = PlaylistHelper.extractSongIds(playlist.songs)
                songIds.removeAt(position)
                musicLibraryViewModel.savePlaylistWithSongIds(playlist, songIds)
                dismiss()
            }
        }

        return super.onCreateDialog(savedInstanceState)
    }
}
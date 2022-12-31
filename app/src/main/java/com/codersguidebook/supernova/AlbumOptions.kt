package com.codersguidebook.supernova

import android.app.Dialog
import android.os.Build
import android.os.Bundle
import androidx.core.view.isGone
import androidx.navigation.findNavController
import androidx.viewbinding.ViewBinding
import com.codersguidebook.supernova.databinding.AlbumOptionsBinding
import com.codersguidebook.supernova.fragment.BaseDialogFragment
import com.codersguidebook.supernova.ui.album.AlbumFragmentDirections
import com.codersguidebook.supernova.ui.artists.ArtistsFragmentDirections

class AlbumOptions(private val albumId: String) : BaseDialogFragment() {

    override var _binding: ViewBinding? = null
        get() = field as AlbumOptionsBinding?
    override val binding: AlbumOptionsBinding
        get() = _binding!! as AlbumOptionsBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = AlbumOptionsBinding.inflate(inflater)

        musicLibraryViewModel.setActiveAlbumId(albumId)

        musicLibraryViewModel.activeAlbumSongs.observe(this) { songs ->
            if (songs.isEmpty()) {
                dismiss()
                return@observe
            }

            binding.optionsTitle.text = songs[0].albumName

            binding.playNext.setOnClickListener {
                mainActivity.addSongsToPlayQueue(songs, true)
                dismiss()
            }

            binding.addPlayQueue.setOnClickListener {
                mainActivity.addSongsToPlayQueue(songs)
                dismiss()
            }

            binding.artist.setOnClickListener {
                val action = ArtistsFragmentDirections.actionSelectArtist(songs[0].artist)
                mainActivity.findNavController(R.id.nav_host_fragment).navigate(action)
                dismiss()
            }

            binding.addPlaylist.setOnClickListener {
                mainActivity.openAddToPlaylistDialog(songs)
                dismiss()
            }

            binding.editAlbum.setOnClickListener {
                val action = AlbumFragmentDirections.actionEditAlbum(songs[0].albumId)
                mainActivity.findNavController(R.id.nav_host_fragment).navigate(action)
                dismiss()
            }

            // Delete Album feature only available from SDK 30 and up
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                binding.deleteAlbum.setOnClickListener {
                    mainActivity.deleteSongs(songs)
                    dismiss()
                }
            } else binding.deleteAlbum.isGone = true
        }

        return super.onCreateDialog(savedInstanceState)
    }
}
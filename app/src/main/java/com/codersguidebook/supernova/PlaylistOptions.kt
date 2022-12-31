package com.codersguidebook.supernova

import android.app.Dialog
import android.os.Bundle
import android.widget.Toast
import androidx.core.view.isGone
import androidx.navigation.findNavController
import androidx.viewbinding.ViewBinding
import com.codersguidebook.supernova.databinding.PlaylistOptionsBinding
import com.codersguidebook.supernova.entities.Playlist
import com.codersguidebook.supernova.fragment.BaseDialogFragment
import com.codersguidebook.supernova.ui.playlist.PlaylistFragmentDirections

class PlaylistOptions(private val playlist: Playlist) : BaseDialogFragment() {

    override var _binding: ViewBinding? = null
        get() = field as PlaylistOptionsBinding?
    override val binding: PlaylistOptionsBinding
        get() = _binding!! as PlaylistOptionsBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = PlaylistOptionsBinding.inflate(inflater)

        musicLibraryViewModel.setActivePlaylistName(playlist.name)

        binding.optionsTitle.text = playlist.name

        musicLibraryViewModel.activePlaylistSongs.observe(this) { songs ->
            if (songs.isNotEmpty()) {
                binding.playNext.setOnClickListener {
                    mainActivity.addSongsToPlayQueue(songs, true)
                    dismiss()
                }

                binding.addPlayQueue.setOnClickListener {
                    mainActivity.addSongsToPlayQueue(songs)
                    dismiss()
                }
            } else {
                binding.playNext.setOnClickListener {
                    Toast.makeText(requireActivity(), getString(R.string.playlist_contains_zero_songs),
                        Toast.LENGTH_SHORT).show()
                    dismiss()
                }

                binding.addPlayQueue.setOnClickListener {
                    Toast.makeText(requireActivity(), getString(R.string.playlist_contains_zero_songs),
                        Toast.LENGTH_SHORT).show()
                    dismiss()
                }
            }
        }

        if (playlist.isDefault) {
            binding.editPlaylist.isGone = true
            binding.deletePlaylist.isGone = true
        } else {
            binding.editPlaylist.setOnClickListener{
                val action = PlaylistFragmentDirections.actionEditPlaylist(playlist.name)
                mainActivity.findNavController(R.id.nav_host_fragment).navigate(action)
                dismiss()
            }

            binding.deletePlaylist.setOnClickListener{
                musicLibraryViewModel.deletePlaylist(playlist)
                dismiss()
            }
        }

        return super.onCreateDialog(savedInstanceState)
    }
}
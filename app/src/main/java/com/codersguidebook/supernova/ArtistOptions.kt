package com.codersguidebook.supernova

import android.app.Dialog
import android.os.Build
import android.os.Bundle
import androidx.core.view.isGone
import androidx.navigation.findNavController
import androidx.viewbinding.ViewBinding
import com.codersguidebook.supernova.databinding.ArtistOptionsBinding
import com.codersguidebook.supernova.fragment.BaseDialogFragment
import com.codersguidebook.supernova.ui.artist.ArtistFragmentDirections

class ArtistOptions(private val artist: String) : BaseDialogFragment() {

    override var _binding: ViewBinding? = null
        get() = field as ArtistOptionsBinding?
    override val binding: ArtistOptionsBinding
        get() = _binding!! as ArtistOptionsBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = ArtistOptionsBinding.inflate(inflater)

        musicLibraryViewModel.setActiveArtistName(artist)

        binding.optionsTitle.text = artist

        musicLibraryViewModel.activeArtistSongs.observe(this) { songs ->
            binding.playNext.setOnClickListener{
                mainActivity.addSongsToPlayQueue(songs, true)
                dismiss()
            }

            binding.addPlayQueue.setOnClickListener {
                mainActivity.addSongsToPlayQueue(songs)
                dismiss()
            }

            binding.addPlaylist.setOnClickListener {
                mainActivity.openAddToPlaylistDialog(songs)
                dismiss()
            }

            binding.editArtist.setOnClickListener{
                val action = ArtistFragmentDirections.actionEditArtist(artist)
                mainActivity.findNavController(R.id.nav_host_fragment).navigate(action)
                dismiss()
            }

            // Delete Artist feature only available from SDK 30 and up
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                binding.deleteArtist.setOnClickListener {
                    mainActivity.deleteSongs(songs)
                    dismiss()
                }
            } else binding.deleteArtist.isGone = true
        }

        return super.onCreateDialog(savedInstanceState)
    }
}
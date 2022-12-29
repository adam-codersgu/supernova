package com.codersguidebook.supernova

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isGone
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import com.codersguidebook.supernova.databinding.OptionsLayoutBinding
import com.codersguidebook.supernova.fragment.BaseDialogFragment
import com.codersguidebook.supernova.ui.album.AlbumFragmentDirections
import com.codersguidebook.supernova.ui.artists.ArtistsFragmentDirections
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AlbumOptions(private val albumId: String) : BaseDialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = mainActivity.layoutInflater
        _binding = OptionsLayoutBinding.inflate(inflater)

        lifecycleScope.launch(Dispatchers.Main) {
            val songs = withContext(Dispatchers.IO) {
                musicLibraryViewModel.getSongsByAlbumId(albumId)
            }
            if (songs.isEmpty()) {
                dismiss()
                return@launch
            }

            binding.optionsTitle.text = songs[0].albumName
            binding.option1.text = getString(R.string.play_next)
            binding.option2.text = getString(R.string.add_que)
            binding.option3.text = getString(R.string.artist)
            binding.option4.text = getString(R.string.add_playlist)
            binding.option5.text = getString(R.string.edit_album)
            binding.option6.isGone = true
            binding.option7.isGone = true

            binding.option1.setOnClickListener{
                mainActivity.addSongsToPlayQueue(songs, true)
                dismiss()
            }

            binding.option2.setOnClickListener{
                mainActivity.addSongsToPlayQueue(songs)
                dismiss()
            }

            binding.option3.setOnClickListener{
                val action = ArtistsFragmentDirections.actionSelectArtist(songs[0].artist)
                mainActivity.findNavController(R.id.nav_host_fragment).navigate(action)
                dismiss()
            }

            binding.option4.setOnClickListener{
                mainActivity.openAddToPlaylistDialog(songs)
                dismiss()
            }

            binding.option5.setOnClickListener{
                val action = AlbumFragmentDirections.actionEditAlbum(songs[0].albumId)
                mainActivity.findNavController(R.id.nav_host_fragment).navigate(action)
                dismiss()
            }
        }

        return AlertDialog.Builder(mainActivity)
            .setView(binding.root)
            .create()
    }
}
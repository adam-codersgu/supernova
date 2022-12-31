package com.codersguidebook.supernova

import android.app.Dialog
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import com.codersguidebook.supernova.databinding.CreatePlaylistBinding
import com.codersguidebook.supernova.entities.Playlist
import com.codersguidebook.supernova.fragment.BaseDialogFragment
import com.codersguidebook.supernova.utils.PlaylistHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CreatePlaylist (private val songIds: List<Long>) : BaseDialogFragment() {

    override var _binding: ViewBinding? = null
        get() = field as CreatePlaylistBinding?
    override val binding: CreatePlaylistBinding
        get() = _binding!! as CreatePlaylistBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = CreatePlaylistBinding.inflate(inflater)

        binding.btnCreatePlaylistCancel.setOnClickListener { dismiss() }

        binding.btnCreatePlaylistOK.setOnClickListener {
            lifecycleScope.launch(Dispatchers.Main) {
                val playlistName = binding.addPlaylistCreate.text.toString()
                if (playlistName.isEmpty()) {
                    Toast.makeText(mainActivity, getString(R.string.playlist_name_cannot_be_empty),
                        Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val playlistNameAlreadyUsed = withContext(Dispatchers.IO) {
                    musicLibraryViewModel.doesPlaylistExistByName(playlistName)
                }
                if (playlistNameAlreadyUsed) {
                    Toast.makeText(mainActivity, getString(R.string.playlist_already_exists),
                        Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val songListJson = PlaylistHelper.serialiseSongIds(songIds)
                val newPlaylist = Playlist(0, playlistName, songListJson, false)
                musicLibraryViewModel.savePlaylist(newPlaylist)
                Toast.makeText(mainActivity, getString(R.string.item_saved, playlistName),
                    Toast.LENGTH_SHORT).show()
                dismiss()
            }
        }

        return super.onCreateDialog(savedInstanceState)
    }
}
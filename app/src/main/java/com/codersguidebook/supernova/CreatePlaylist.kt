package com.codersguidebook.supernova

import android.app.Dialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.codersguidebook.supernova.databinding.CreatePlaylistBinding
import com.codersguidebook.supernova.entities.Playlist
import com.codersguidebook.supernova.utils.PlaylistHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CreatePlaylist (private val songIds: List<Long>) : DialogFragment() {

    private var _binding: CreatePlaylistBinding? = null
    private val binding get() = _binding!!
    private lateinit var mainActivity: MainActivity
    private lateinit var musicLibraryViewModel: MusicLibraryViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainActivity = activity as MainActivity
        musicLibraryViewModel = ViewModelProvider(mainActivity)[MusicLibraryViewModel::class.java]
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = mainActivity.layoutInflater
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

        return AlertDialog.Builder(mainActivity)
            .setView(binding.root)
            .create()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
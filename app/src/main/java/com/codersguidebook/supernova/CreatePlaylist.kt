package com.codersguidebook.supernova

import android.app.Dialog
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.codersguidebook.supernova.entities.Playlist
import com.codersguidebook.supernova.utils.PlaylistHelper

class CreatePlaylist (private val songIds: List<Long>) : DialogFragment() {

    private lateinit var musicLibraryViewModel: MusicLibraryViewModel

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        musicLibraryViewModel = ViewModelProvider(this)[MusicLibraryViewModel::class.java]
        val builder = AlertDialog.Builder(this.requireActivity())
        val inflater = requireActivity().layoutInflater
        val dialogView = inflater.inflate(R.layout.create_playlist, null as ViewGroup?)
        val txtCreatePlaylist = dialogView.findViewById(R.id.addPlaylistCreate) as EditText
        val btnCancel = dialogView.findViewById(R.id.btnCreatePlaylistCancel) as Button
        val btnOK = dialogView.findViewById(R.id.btnCreatePlaylistOK) as Button

        btnCancel.setOnClickListener {
            dismiss()
        }

        btnOK.setOnClickListener {
            val playlistName = txtCreatePlaylist.text.toString()
            if (playlistName.isNotEmpty()) {
                val songListJson = PlaylistHelper.serialiseSongIds(songIds)

                val newPlaylist = Playlist(0, playlistName, songListJson, false)

                if (musicLibraryViewModel.savePlaylist(newPlaylist)) {
                    Toast.makeText(requireActivity(), getString(R.string.item_saved, playlistName), Toast.LENGTH_SHORT).show()
                    dismiss()
                } else Toast.makeText(requireActivity(), getString(R.string.playlist_already_exists), Toast.LENGTH_SHORT).show()
            } else Toast.makeText(requireActivity(), getString(R.string.playlist_name_cannot_be_empty), Toast.LENGTH_SHORT).show()
        }

        builder.setView(dialogView)

        return builder.create()
    }
}
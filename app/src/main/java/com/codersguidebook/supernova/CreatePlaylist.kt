package com.codersguidebook.supernova

import android.app.Dialog
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.codersguidebook.supernova.entities.Playlist
import com.codersguidebook.supernova.entities.Song

class CreatePlaylist (private val songs: List<Song>) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val callingActivity = activity as MainActivity
        val builder = AlertDialog.Builder(this.requireActivity())
        val inflater = requireActivity().layoutInflater
        val dialogView = inflater.inflate(R.layout.create_playlist, null as ViewGroup?)
        val txtCreatePlaylist = dialogView.findViewById(R.id.addPlaylistCreate) as EditText
        val btnCancel = dialogView.findViewById(R.id.btnCreatePlaylistCancel) as Button
        val btnOK = dialogView.findViewById(R.id.btnCreatePlaylistOK) as Button

        btnCancel.setOnClickListener{
            dismiss()
        }

        btnOK.setOnClickListener{
            val playlistName = txtCreatePlaylist.text.toString()
            if (playlistName.isNotEmpty()) {
                val songListJSON = callingActivity.convertSongsToSongIDJSON(songs)

                // playlist ID should be automatically assigned
                val newPlaylist = Playlist(0, playlistName, songListJSON, false)

                if (callingActivity.saveNewPlaylist(newPlaylist)) {
                    Toast.makeText(activity, "Playlist called $playlistName is saved.", Toast.LENGTH_SHORT).show()
                    dismiss()
                } else Toast.makeText(activity, "A playlist with that name already exists", Toast.LENGTH_SHORT).show()
            } else Toast.makeText(activity, "Playlist name cannot be empty", Toast.LENGTH_SHORT).show()
        }

        builder.setView(dialogView)

        return builder.create()
    }
}
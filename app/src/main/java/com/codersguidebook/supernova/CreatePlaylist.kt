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

class CreatePlaylist (private val songIds: List<Long>) : DialogFragment() {

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
                val songListJSON = callingActivity.convertSongIdListToJson(songIds)

                val newPlaylist = Playlist(0, playlistName, songListJSON, false)

                if (callingActivity.savePlaylist(newPlaylist)) {
                    Toast.makeText(activity, getString(R.string.item_saved, playlistName), Toast.LENGTH_SHORT).show()
                    dismiss()
                } else Toast.makeText(activity, getString(R.string.playlist_already_exists), Toast.LENGTH_SHORT).show()
            } else Toast.makeText(activity, getString(R.string.playlist_name_cannot_be_empty), Toast.LENGTH_SHORT).show()
        }

        builder.setView(dialogView)

        return builder.create()
    }
}
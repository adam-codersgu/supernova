package com.codersguidebook.supernova

import android.app.Dialog
import android.os.Bundle
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isGone
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import com.codersguidebook.supernova.entities.Song
import com.codersguidebook.supernova.ui.artist.ArtistFragmentDirections

class ArtistOptions(private val artistName: String) : DialogFragment() {

    private var musicDatabase: MusicDatabase? = null
    private var songList: List<Song>? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val builder = AlertDialog.Builder(this.requireActivity())

        val inflater = requireActivity().layoutInflater

        val dialogView = inflater.inflate(R.layout.options_layout, null as ViewGroup?)

        val txtTitle = dialogView.findViewById(R.id.optionsTitle) as TextView
        txtTitle.text = artistName
        val txtPlayNext = dialogView.findViewById(R.id.option1) as TextView
        txtPlayNext.text = getString(R.string.play_next)
        val txtAddQueue = dialogView.findViewById(R.id.option2) as TextView
        txtAddQueue.text = getString(R.string.add_que)
        val txtAddPlaylist = dialogView.findViewById(R.id.option3) as TextView
        txtAddPlaylist.text = getString(R.string.add_playlist)
        val txtEditInfo = dialogView.findViewById(R.id.option4) as TextView
        txtEditInfo.text = getString(R.string.edit_artist)
        (dialogView.findViewById(R.id.option5) as TextView).isGone = true
        (dialogView.findViewById(R.id.option6) as TextView).isGone = true
        (dialogView.findViewById(R.id.option7) as TextView).isGone = true

        val callingActivity = activity as MainActivity

        musicDatabase = MusicDatabase.getDatabase(requireContext(), lifecycleScope)
        musicDatabase!!.musicDao().findArtistsSongs(artistName)
            .observe(this) { songs ->
                songs?.let {
                    this.songList = it
                }
            }

        builder.setView(dialogView)

        txtPlayNext.setOnClickListener{
            if (!songList.isNullOrEmpty()) callingActivity.addSongsToPlayQueue(songList!!, true)
            else Toast.makeText(activity, getString(R.string.no_songs_found, artistName), Toast.LENGTH_SHORT).show()
            dismiss()
        }

        txtAddQueue.setOnClickListener{
            if (!songList.isNullOrEmpty()) callingActivity.addSongsToPlayQueue(songList!!)
            else Toast.makeText(callingActivity, getString(R.string.no_songs_found, artistName), Toast.LENGTH_SHORT).show()
            dismiss()
        }

        txtAddPlaylist.setOnClickListener{
            callingActivity.openAddToPlaylistDialog(songList!!)
            dismiss()
        }

        txtEditInfo.setOnClickListener{
            val action = ArtistFragmentDirections.actionEditArtist(artistName)
            callingActivity.findNavController(R.id.nav_host_fragment).navigate(action)
            dismiss()
        }

        return builder.create()
    }
}
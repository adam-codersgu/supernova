package com.codersguidebook.supernova

import android.app.Dialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isGone
import androidx.fragment.app.DialogFragment
import androidx.navigation.findNavController
import com.codersguidebook.supernova.databinding.OptionsLayoutBinding
import com.codersguidebook.supernova.entities.Playlist
import com.codersguidebook.supernova.ui.playlist.PlaylistFragmentDirections

class PlaylistOptions(private val playlist: Playlist) : DialogFragment() {

    private var _binding: OptionsLayoutBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val callingActivity = activity as MainActivity
        val inflater = callingActivity.layoutInflater
        _binding = OptionsLayoutBinding.inflate(inflater)

        val builder = AlertDialog.Builder(callingActivity)
            .setView(binding.root)

        binding.optionsTitle.text = playlist.name
        binding.option1.text = getString(R.string.play_next)
        binding.option2.text = getString(R.string.add_que)
        binding.option3.text = getString(R.string.delete_playlist)
        binding.option4.text = getString(R.string.edit_playlist)
        binding.option5.isGone = true
        binding.option6.isGone = true
        binding.option7.isGone = true

        if (playlist.isDefault) {
            binding.option3.isGone = true
            binding.option4.isGone = true
        }

        binding.option1.setOnClickListener{
            if (playlist.songs != null){
                val playlistSongs = callingActivity.extractPlaylistSongs(playlist.songs)
                callingActivity.addSongsToPlayQueue(playlistSongs, false)
            } else Toast.makeText(activity, "There are no songs in that playlist.", Toast.LENGTH_SHORT).show()
            dismiss()
        }

        binding.option2.setOnClickListener{
            if (playlist.songs != null){
                val playlistSongs = callingActivity.extractPlaylistSongs(playlist.songs)
                callingActivity.addSongsToPlayQueue(playlistSongs, true)
            } else Toast.makeText(activity, "There are no songs in that playlist.", Toast.LENGTH_SHORT).show()
            dismiss()
        }

        binding.option3.setOnClickListener{
            callingActivity.deletePlaylist(playlist)
            dismiss()
        }

        binding.option4.setOnClickListener{
            val action = PlaylistFragmentDirections.actionEditPlaylist(playlist.name)
            callingActivity.findNavController(R.id.nav_host_fragment).navigate(action)
            dismiss()
        }

        return builder.create()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
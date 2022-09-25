package com.codersguidebook.supernova

import android.app.Dialog
import android.os.Bundle
import androidx.core.view.isGone
import androidx.fragment.app.DialogFragment
import androidx.navigation.findNavController
import com.codersguidebook.supernova.databinding.OptionsLayoutBinding
import com.codersguidebook.supernova.entities.QueueItem
import com.codersguidebook.supernova.ui.albums.AlbumsFragmentDirections
import com.codersguidebook.supernova.ui.artists.ArtistsFragmentDirections

class QueueOptions(private val queueItem: QueueItem,
                   private val currentlyPlaying: Boolean,
                   private val position: Int) : DialogFragment() {

    private var _binding: OptionsLayoutBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val callingActivity = activity as MainActivity
        val inflater = callingActivity.layoutInflater
        _binding = OptionsLayoutBinding.inflate(inflater)

        binding.optionsTitle.text = queueItem.song.title

        // artist TextView
        binding.option1.text = getString(R.string.artist)
        binding.option1.setOnClickListener{
            val action = ArtistsFragmentDirections.actionSelectArtist(queueItem.song.artist)
            callingActivity.findNavController(R.id.nav_host_fragment).navigate(action)
            dismiss()
        }

        // album TextView
        binding.option2.text = getString(R.string.album)
        binding.option2.setOnClickListener{
            val action = AlbumsFragmentDirections.actionSelectAlbum(queueItem.song.albumID)
            callingActivity.findNavController(R.id.nav_host_fragment).navigate(action)
            dismiss()
        }

        // add to playlist button
        binding.option3.text = getString(R.string.add_playlist)
        binding.option3.setOnClickListener{
            callingActivity.openAddToPlaylistDialog(listOf(queueItem.song))
            dismiss()
        }

        // remove from play queue button
        binding.option4.text = getString(R.string.remove_from_queue)
        if (currentlyPlaying) binding.option4.isGone = true
        else {
            binding.option4.setOnClickListener {
                callingActivity.removeQueueItem(position)
                dismiss()
            }
        }

        binding.option5.isGone = true
        binding.option6.isGone = true
        binding.option7.isGone = true

        return android.app.AlertDialog.Builder(callingActivity)
            .setView(binding.root)
            .create()
    }
}
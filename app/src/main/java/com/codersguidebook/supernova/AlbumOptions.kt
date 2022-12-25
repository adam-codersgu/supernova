package com.codersguidebook.supernova

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isGone
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import com.codersguidebook.supernova.databinding.OptionsLayoutBinding
import com.codersguidebook.supernova.ui.album.AlbumFragmentDirections
import com.codersguidebook.supernova.ui.artists.ArtistsFragmentDirections

class AlbumOptions(private val albumId: String) : DialogFragment() {

    private var _binding: OptionsLayoutBinding? = null
    private val binding get() = _binding!!
    private lateinit var musicLibraryViewModel: MusicLibraryViewModel

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val callingActivity = activity as MainActivity
        musicLibraryViewModel = ViewModelProvider(callingActivity)[MusicLibraryViewModel::class.java]

        val inflater = callingActivity.layoutInflater
        _binding = OptionsLayoutBinding.inflate(inflater)

        val builder = AlertDialog.Builder(callingActivity)
            .setView(binding.root)

        val songs = musicLibraryViewModel.getSongsByAlbumId(albumId)
        if (songs.isEmpty()) return builder.create()

        binding.optionsTitle.text = songs[0].albumName
        binding.option1.text = getString(R.string.play_next)
        binding.option2.text = getString(R.string.add_que)
        binding.option3.text = getString(R.string.artist)
        binding.option4.text = getString(R.string.add_playlist)
        binding.option5.text = getString(R.string.edit_album)
        binding.option6.isGone = true
        binding.option7.isGone = true

        binding.option1.setOnClickListener{
            callingActivity.addSongsToPlayQueue(songs, true)
            dismiss()
        }

        binding.option2.setOnClickListener{
            callingActivity.addSongsToPlayQueue(songs)
            dismiss()
        }

        binding.option3.setOnClickListener{
            val action = ArtistsFragmentDirections.actionSelectArtist(songs[0].artist)
            callingActivity.findNavController(R.id.nav_host_fragment).navigate(action)
            dismiss()
        }

        binding.option4.setOnClickListener{
            callingActivity.openAddToPlaylistDialog(songs)
            dismiss()
        }

        binding.option5.setOnClickListener{
            val action = AlbumFragmentDirections.actionEditAlbum(songs[0].albumId)
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
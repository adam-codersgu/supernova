package com.codersguidebook.supernova

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import com.codersguidebook.supernova.databinding.OptionsLayoutBinding
import com.codersguidebook.supernova.entities.Song
import com.codersguidebook.supernova.ui.albums.AlbumsFragmentDirections
import com.codersguidebook.supernova.ui.artists.ArtistsFragmentDirections
import com.codersguidebook.supernova.ui.songs.SongsFragmentDirections

class SongOptions(private val song: Song) : DialogFragment() {

    private var _binding: OptionsLayoutBinding? = null
    private val binding get() = _binding!!
    private lateinit var musicLibraryViewModel: MusicLibraryViewModel

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        musicLibraryViewModel = ViewModelProvider(this)[MusicLibraryViewModel::class.java]
        val callingActivity = activity as MainActivity
        val inflater = callingActivity.layoutInflater
        _binding = OptionsLayoutBinding.inflate(inflater)

        val builder = AlertDialog.Builder(callingActivity)
            .setView(binding.root)

        binding.optionsTitle.text = song.title
        binding.option1.text = getString(R.string.play_next)
        binding.option2.text = getString(R.string.add_que)
        binding.option4.text = getString(R.string.artist)
        binding.option5.text = getString(R.string.album)
        binding.option6.text = getString(R.string.add_playlist)
        binding.option7.text = getString(R.string.edit_music)

        val updatedSong = musicLibraryViewModel.getSongById(song.songId)

        binding.option1.setOnClickListener{
            callingActivity.addSongsToPlayQueue(listOf(song), true)
            dismiss()
        }

        binding.option2.setOnClickListener{
            callingActivity.addSongsToPlayQueue(listOf(song))
            dismiss()
        }

        if (updatedSong?.isFavourite == true)  binding.option3.text = getString(R.string.remove_favourites)
        else  binding.option3.text = getString(R.string.add_to_favourites)

        binding.option3.setOnClickListener {
            callingActivity.toggleSongFavouriteStatus(updatedSong!!)
            dismiss()
        }

        binding.option4.setOnClickListener{
            val action = ArtistsFragmentDirections.actionSelectArtist(song.artist)
            callingActivity.findNavController(R.id.nav_host_fragment).navigate(action)
            dismiss()
        }

        binding.option5.setOnClickListener{
            val action = AlbumsFragmentDirections.actionSelectAlbum(song.albumId)
            callingActivity.findNavController(R.id.nav_host_fragment).navigate(action)
            dismiss()
        }

        binding.option6.setOnClickListener{
            callingActivity.openAddToPlaylistDialog(listOf(song))
            dismiss()
        }

        binding.option7.setOnClickListener{
            val action = SongsFragmentDirections.actionEditSong(song)
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
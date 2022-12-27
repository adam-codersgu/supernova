package com.codersguidebook.supernova

import android.app.Dialog
import android.os.Bundle
import androidx.navigation.findNavController
import com.codersguidebook.supernova.databinding.OptionsLayoutBinding
import com.codersguidebook.supernova.entities.Song
import com.codersguidebook.supernova.fragment.BaseDialogFragment
import com.codersguidebook.supernova.ui.albums.AlbumsFragmentDirections
import com.codersguidebook.supernova.ui.artists.ArtistsFragmentDirections
import com.codersguidebook.supernova.ui.songs.SongsFragmentDirections

class SongOptions(private val song: Song) : BaseDialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = mainActivity.layoutInflater
        _binding = OptionsLayoutBinding.inflate(inflater)

        binding.optionsTitle.text = song.title
        binding.option1.text = getString(R.string.play_next)
        binding.option2.text = getString(R.string.add_que)
        binding.option4.text = getString(R.string.artist)
        binding.option5.text = getString(R.string.album)
        binding.option6.text = getString(R.string.add_playlist)
        binding.option7.text = getString(R.string.edit_music)

        binding.option1.setOnClickListener{
            mainActivity.addSongsToPlayQueue(listOf(song), true)
            dismiss()
        }

        binding.option2.setOnClickListener{
            mainActivity.addSongsToPlayQueue(listOf(song))
            dismiss()
        }

        if (song.isFavourite) binding.option3.text = getString(R.string.remove_favourites)
        else binding.option3.text = getString(R.string.add_to_favourites)

        binding.option3.setOnClickListener {
            musicLibraryViewModel.toggleSongFavouriteStatus(song)
            dismiss()
        }

        binding.option4.setOnClickListener{
            val action = ArtistsFragmentDirections.actionSelectArtist(song.artist)
            mainActivity.findNavController(R.id.nav_host_fragment).navigate(action)
            dismiss()
        }

        binding.option5.setOnClickListener{
            val action = AlbumsFragmentDirections.actionSelectAlbum(song.albumId)
            mainActivity.findNavController(R.id.nav_host_fragment).navigate(action)
            dismiss()
        }

        binding.option6.setOnClickListener{
            mainActivity.openAddToPlaylistDialog(listOf(song))
            dismiss()
        }

        binding.option7.setOnClickListener{
            val action = SongsFragmentDirections.actionEditSong(song)
            mainActivity.findNavController(R.id.nav_host_fragment).navigate(action)
            dismiss()
        }

        return super.onCreateDialog(savedInstanceState)
    }
}
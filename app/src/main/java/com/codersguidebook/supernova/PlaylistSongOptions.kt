package com.codersguidebook.supernova

import android.app.Dialog
import android.os.Bundle
import androidx.core.view.isGone
import androidx.navigation.findNavController
import com.codersguidebook.supernova.databinding.OptionsLayoutBinding
import com.codersguidebook.supernova.entities.Playlist
import com.codersguidebook.supernova.entities.Song
import com.codersguidebook.supernova.fragment.BaseDialogFragment
import com.codersguidebook.supernova.ui.albums.AlbumsFragmentDirections
import com.codersguidebook.supernova.ui.artists.ArtistsFragmentDirections

class PlaylistSongOptions(private val songs: MutableList<Song>,
                          private val position: Int,
                          private val playlist: Playlist
) : BaseDialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = mainActivity.layoutInflater
        _binding = OptionsLayoutBinding.inflate(inflater)

        val song = if (position < songs.size) songs[position]
        else return super.onCreateDialog(savedInstanceState)

        binding.option1.text = getString(R.string.play_next)
        binding.option2.text = getString(R.string.add_que)
        binding.option3.text = if (song.isFavourite) getString(R.string.remove_favourites)
        else getString(R.string.add_to_favourites)
        binding.option4.text = getString(R.string.artist)
        binding.option5.text = getString(R.string.album)
        binding.option6.text = getString(R.string.add_playlist)
        binding.option7.text = getString(R.string.remove_playlist)

        binding.optionsTitle.text = song.title

        binding.option1.setOnClickListener{
            mainActivity.addSongsToPlayQueue(listOf(song), true)
            dismiss()
        }

        binding.option2.setOnClickListener{
            mainActivity.addSongsToPlayQueue(listOf(song))
            dismiss()
        }

        binding.option3.setOnClickListener {
            musicLibraryViewModel.toggleSongFavouriteStatus(song)
            dismiss()
        }

        binding.option4.setOnClickListener{
            val action = ArtistsFragmentDirections.actionSelectArtist(songs[position].artist)
            mainActivity.findNavController(R.id.nav_host_fragment).navigate(action)
            dismiss()
        }

        binding.option5.setOnClickListener{
            val action = AlbumsFragmentDirections.actionSelectAlbum(songs[position].albumId)
            mainActivity.findNavController(R.id.nav_host_fragment).navigate(action)
            dismiss()
        }

        binding.option6.setOnClickListener{
            mainActivity.openAddToPlaylistDialog(listOf(songs[position]))
            dismiss()
        }

        if (playlist.name == getString(R.string.most_played) || playlist.name == getString(R.string.favourites)) {
            binding.option7.isGone = true
        } else {
            binding.option7.setOnClickListener{
                songs.removeAt(position)
                val songIds = songs.map { song -> song.songId }
                musicLibraryViewModel.savePlaylistWithSongIds(playlist, songIds)
                dismiss()
            }
        }

        return super.onCreateDialog(savedInstanceState)
    }
}
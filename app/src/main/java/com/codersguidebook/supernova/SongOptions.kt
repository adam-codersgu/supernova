package com.codersguidebook.supernova

import android.app.Dialog
import android.os.Bundle
import androidx.navigation.findNavController
import androidx.viewbinding.ViewBinding
import com.codersguidebook.supernova.databinding.SongOptionsBinding
import com.codersguidebook.supernova.entities.Song
import com.codersguidebook.supernova.fragment.BaseDialogFragment
import com.codersguidebook.supernova.ui.albums.AlbumsFragmentDirections
import com.codersguidebook.supernova.ui.artists.ArtistsFragmentDirections
import com.codersguidebook.supernova.ui.songs.SongsFragmentDirections

class SongOptions(private val song: Song) : BaseDialogFragment() {

    override var _binding: ViewBinding? = null
        get() = field as SongOptionsBinding?
    override val binding: SongOptionsBinding
        get() = _binding!! as SongOptionsBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = SongOptionsBinding.inflate(inflater)

        binding.optionsTitle.text = song.title

        binding.playNext.setOnClickListener {
            mainActivity.addSongsToPlayQueue(listOf(song), true)
            dismiss()
        }

        binding.addPlayQueue.setOnClickListener {
            mainActivity.addSongsToPlayQueue(listOf(song))
            dismiss()
        }

        if (song.isFavourite) binding.favourite.text = getString(R.string.remove_favourites)

        binding.favourite.setOnClickListener {
            musicLibraryViewModel.toggleSongFavouriteStatus(song)
            dismiss()
        }

        binding.artist.setOnClickListener {
            val action = ArtistsFragmentDirections.actionSelectArtist(song.artist)
            mainActivity.findNavController(R.id.nav_host_fragment).navigate(action)
            dismiss()
        }

        binding.album.setOnClickListener {
            val action = AlbumsFragmentDirections.actionSelectAlbum(song.albumId)
            mainActivity.findNavController(R.id.nav_host_fragment).navigate(action)
            dismiss()
        }

        binding.addPlaylist.setOnClickListener {
            mainActivity.openAddToPlaylistDialog(listOf(song))
            dismiss()
        }

        binding.editSong.setOnClickListener {
            val action = SongsFragmentDirections.actionEditSong(song)
            mainActivity.findNavController(R.id.nav_host_fragment).navigate(action)
            dismiss()
        }

        binding.deleteSong.setOnClickListener {
            // TODO: Need to include an API check here (or in MainActivity)
            mainActivity.deleteSongById(song.songId)
            dismiss()
        }

        return super.onCreateDialog(savedInstanceState)
    }
}
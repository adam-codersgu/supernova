package com.codersguidebook.supernova

import android.app.Dialog
import android.os.Bundle
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isGone
import androidx.fragment.app.DialogFragment
import androidx.navigation.findNavController
import com.codersguidebook.supernova.entities.Playlist
import com.codersguidebook.supernova.entities.Song
import com.codersguidebook.supernova.ui.albums.AlbumsFragmentDirections
import com.codersguidebook.supernova.ui.artists.ArtistsFragmentDirections

class PlaylistSongOptions(private val songs: MutableList<Song>,
                          private val position: Int,
                          private val playlist: Playlist
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val builder = AlertDialog.Builder(this.requireActivity())

        val inflater = requireActivity().layoutInflater

        val dialogView = inflater.inflate(R.layout.options_layout, null as ViewGroup?)

        val txtTitle = dialogView.findViewById(R.id.optionsTitle) as TextView
        val txtPlayNext = dialogView.findViewById(R.id.option1) as TextView
        txtPlayNext.text = getString(R.string.play_next)
        val txtAddQueue = dialogView.findViewById(R.id.option2) as TextView
        txtAddQueue.text = getString(R.string.add_que)
        val txtAddFavourites = dialogView.findViewById(R.id.option3) as TextView
        val txtViewArtist = dialogView.findViewById(R.id.option4) as TextView
        txtViewArtist.text = getString(R.string.artist)
        val txtViewAlbum = dialogView.findViewById(R.id.option5) as TextView
        txtViewAlbum.text = getString(R.string.album)
        val txtAddPlaylist = dialogView.findViewById(R.id.option6) as TextView
        txtAddPlaylist.text = getString(R.string.add_playlist)
        val txtRemovePlaylist = dialogView.findViewById(R.id.option7) as TextView
        txtRemovePlaylist.text = getString(R.string.remove_playlist)

        val callingActivity = activity as MainActivity
        val updatedSong = callingActivity.completeLibrary.find {
            it.songId == songs[position].songId
        }

        builder.setView(dialogView)

        txtTitle.text = updatedSong?.title

        txtPlayNext.setOnClickListener{
            callingActivity.addSongsToPlayQueue(listOf(songs[position]), false)
            dismiss()
        }

        txtAddQueue.setOnClickListener{
            callingActivity.addSongsToPlayQueue(listOf(songs[position]), true)
            dismiss()
        }

        if (updatedSong?.isFavourite == true) txtAddFavourites.text = getString(R.string.remove_favourites)
        else txtAddFavourites.text = getString(R.string.add_to_favourites)

        txtAddFavourites.setOnClickListener {
            callingActivity.updateFavourites(updatedSong!!)
            dismiss()
        }

        txtViewArtist.setOnClickListener{
            val action = ArtistsFragmentDirections.actionSelectArtist(songs[position].artist)
            callingActivity.findNavController(R.id.nav_host_fragment).navigate(action)
            dismiss()
        }

        txtViewAlbum.setOnClickListener{
            val action = AlbumsFragmentDirections.actionSelectAlbum(songs[position].albumId)
            callingActivity.findNavController(R.id.nav_host_fragment).navigate(action)
            dismiss()
        }

        txtAddPlaylist.setOnClickListener{
            callingActivity.openAddToPlaylistDialog(listOf(songs[position]))
            dismiss()
        }

        if (playlist.name == getString(R.string.most_played) || playlist.name == getString(R.string.favourites)) {
            txtRemovePlaylist.isGone = true
        } else {
            txtRemovePlaylist.setOnClickListener{
                songs.removeAt(position)
                callingActivity.savePlaylistNewSongList(playlist, songs)
                dismiss()
            }
        }

        return builder.create()
    }
}
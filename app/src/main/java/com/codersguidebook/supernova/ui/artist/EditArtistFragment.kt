package com.codersguidebook.supernova.ui.artist

import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.findNavController
import androidx.viewbinding.ViewBinding
import com.codersguidebook.supernova.R
import com.codersguidebook.supernova.databinding.FragmentEditArtistBinding
import com.codersguidebook.supernova.fragment.BaseEditMusicFragment

class EditArtistFragment : BaseEditMusicFragment() {

    override var _binding: ViewBinding? = null
        get() = field as FragmentEditArtistBinding?
    override val binding: FragmentEditArtistBinding
        get() = _binding!! as FragmentEditArtistBinding

    private var artistName = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        artistName = EditArtistFragmentArgs.fromBundle(arguments ?: Bundle()).artist
            ?: getString(R.string.default_artist)

        _binding = FragmentEditArtistBinding.inflate(inflater, container, false)

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.editArtistName.text = SpannableStringBuilder(artistName)

        musicLibraryViewModel.setActiveArtistName(artistName)
    }

    override fun menuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.save -> {
                val newName = binding.editArtistName.text.toString()
                val songs = musicLibraryViewModel.activeArtistSongs.value

                when {
                    newName.isEmpty() -> Toast.makeText(activity, getString(R.string.artist_name_cannot_be_empty),
                        Toast.LENGTH_SHORT).show()
                    newName == artistName -> Toast.makeText(activity, getString(R.string.artist_name_not_changed),
                        Toast.LENGTH_SHORT).show()
                    songs == null -> Toast.makeText(activity, getString(R.string.no_songs_for_artist),
                        Toast.LENGTH_SHORT).show()
                    else -> {
                        for (song in songs) song.artist = newName
                        mainActivity.updateSongs(songs)
                        Toast.makeText(activity, getString(R.string.artist_updated),
                            Toast.LENGTH_SHORT).show()
                        val action = EditArtistFragmentDirections.actionFinishEditArtist(artistName)
                        requireView().findNavController().navigate(action)
                    }
                }
                true
            }

            else -> false
        }
    }
}
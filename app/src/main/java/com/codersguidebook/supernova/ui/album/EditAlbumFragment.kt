package com.codersguidebook.supernova.ui.album

import android.net.Uri
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.findNavController
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.codersguidebook.supernova.R
import com.codersguidebook.supernova.databinding.FragmentEditAlbumBinding
import com.codersguidebook.supernova.fragment.BaseEditMusicFragment
import com.codersguidebook.supernova.utils.ImageHandlingHelper

class EditAlbumFragment : BaseEditMusicFragment() {

    private var albumId: String? = null

    override var _binding: ViewBinding? = null
        get() = field as FragmentEditAlbumBinding?
    override val binding: FragmentEditAlbumBinding
        get() = _binding!! as FragmentEditAlbumBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        arguments?.let {
            val safeArgs = EditAlbumFragmentArgs.fromBundle(it)
            albumId = safeArgs.albumId
        }

        _binding = FragmentEditAlbumBinding.inflate(inflater, container, false)
        
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        albumId?.let { albumId ->
            musicLibraryViewModel.setActiveAlbumId(albumId)

            musicLibraryViewModel.activeAlbumSongs.observe(viewLifecycleOwner) { songs ->
                val songToUseForDisplay = if (songs.isNotEmpty()) songs[0]
                else return@observe
                binding.editAlbumTitle.text = SpannableStringBuilder(songToUseForDisplay.albumName)
                binding.editAlbumYear.text = SpannableStringBuilder(songToUseForDisplay.year)
            }
        }

        ImageHandlingHelper.loadImageByAlbumId(mainActivity.application, albumId, binding.editAlbumArtwork)
        binding.editAlbumArtwork.setOnClickListener { getImage() }

        binding.editAlbumArtworkIcon.setOnClickListener { getImage() }
    }
    
    override fun menuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.save -> {
                val songs = musicLibraryViewModel.activeAlbumSongs.value
                if (songs.isNullOrEmpty()) {
                    Toast.makeText(activity, getString(R.string.no_songs_for_album),
                        Toast.LENGTH_SHORT).show()
                } else {
                    val newAlbumTitle = binding.editAlbumTitle.text.toString()
                    val newAlbumYear = binding.editAlbumYear.text.toString()

                    when {
                        newAlbumTitle.isEmpty() -> Toast.makeText(mainActivity,
                            getString(R.string.album_name_cannot_be_empty), Toast.LENGTH_SHORT).show()
                        newAlbumYear.isEmpty() -> Toast.makeText(mainActivity,
                            getString(R.string.album_year_cannot_be_empty), Toast.LENGTH_SHORT).show()
                        else -> {
                            newArtwork?.let { albumArt ->
                                ImageHandlingHelper.saveAlbumArtByResourceId(mainActivity.application,
                                    albumId!!, albumArt)
                            }

                            if (newAlbumTitle != songs[0].title || newAlbumYear != songs[0].year) {
                                for (song in songs) {
                                    song.albumName = newAlbumTitle
                                    song.year = newAlbumYear
                                }
                                mainActivity.updateSongs(songs)
                            }

                            val action = EditAlbumFragmentDirections.actionFinishEditAlbum(albumId!!)
                            requireView().findNavController().navigate(action)

                            Toast.makeText(activity, getString(R.string.album_updated), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                true
            }
            else -> false
        }
    }

    override fun furtherUriProcessing(uri: Uri) {
        Glide.with(this)
            .load(uri)
            .centerCrop()
            .into(binding.editAlbumArtwork)
    }
}
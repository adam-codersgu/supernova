package com.codersguidebook.supernova.ui.playlist

import android.net.Uri
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.codersguidebook.supernova.R
import com.codersguidebook.supernova.databinding.FragmentEditPlaylistBinding
import com.codersguidebook.supernova.entities.Playlist
import com.codersguidebook.supernova.fragment.BaseEditMusicFragment
import com.codersguidebook.supernova.utils.ImageHandlingHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditPlaylistFragment : BaseEditMusicFragment() {

    override var _binding: ViewBinding? = null
        get() = field as FragmentEditPlaylistBinding?
    override val binding: FragmentEditPlaylistBinding
        get() = _binding!! as FragmentEditPlaylistBinding

    private var playlist: Playlist? = null
    private var playlistName: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        arguments?.let {
            val safeArgs = EditPlaylistFragmentArgs.fromBundle(it)
            playlistName = safeArgs.playlistName
        }

        _binding = FragmentEditPlaylistBinding.inflate(inflater, container, false)

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.editPlaylistName.text = SpannableStringBuilder(playlistName)

        playlistName?.let { name ->
            musicLibraryViewModel.setActivePlaylistName(name)

            lifecycleScope.launch(Dispatchers.Main) {
                playlist = withContext(Dispatchers.IO) {
                    musicLibraryViewModel.getPlaylistByName(name)
                }
                playlist?.let {
                    if (!ImageHandlingHelper.loadImageByPlaylist(mainActivity.application,
                            it, binding.artwork)) {
                        musicLibraryViewModel.activePlaylistSongs.observe(viewLifecycleOwner) { songs ->
                            val randomSong = if (songs.isNotEmpty()) songs.random()
                            else return@observe
                            ImageHandlingHelper.loadImageByAlbumId(mainActivity.application,
                                randomSong.albumId, binding.artwork)
                        }
                    }
                }
            }
        }

        binding.artwork.setOnClickListener {  }

        binding.editArtworkIcon.setOnClickListener { getImage() }
    }

    override fun menuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.save -> {
                playlist?.apply {
                    val newPlaylistName = binding.editPlaylistName.text.toString()

                    if (newPlaylistName.isEmpty()) {
                        Toast.makeText(activity, getString(R.string.playlist_name_cannot_be_empty),
                            Toast.LENGTH_SHORT).show()
                    } else {
                        this.name = newPlaylistName

                        newArtwork?.let {
                            ImageHandlingHelper.savePlaylistArtByResourceId(
                                mainActivity.application, this.playlistId.toString(), it)
                        }

                        musicLibraryViewModel.updatePlaylists(listOf(this))

                        val action = EditPlaylistFragmentDirections.actionFinishEditPlaylist(newPlaylistName)
                        requireView().findNavController().navigate(action)

                        Toast.makeText(activity, getString(R.string.playlist_updated), Toast.LENGTH_SHORT).show()
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
            .into(binding.artwork)
    }
}
package com.codersguidebook.supernova.ui.playlist

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.SpannableStringBuilder
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import com.bumptech.glide.Glide
import com.codersguidebook.supernova.MainActivity
import com.codersguidebook.supernova.MusicLibraryViewModel
import com.codersguidebook.supernova.R
import com.codersguidebook.supernova.databinding.FragmentEditPlaylistBinding
import com.codersguidebook.supernova.entities.Playlist
import com.codersguidebook.supernova.utils.ImageHandlingHelper
import com.codersguidebook.supernova.utils.PlaylistHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.FileNotFoundException
import java.io.IOException

class EditPlaylistFragment : Fragment() {

    companion object {
        private const val GET_ARTWORK = 1
    }

    private var _binding: FragmentEditPlaylistBinding? = null
    private val binding get() = _binding!!
    private var newArtwork: Bitmap? = null
    private var selectedImageUri: Uri? = null
    private lateinit var callingActivity: MainActivity
    private lateinit var musicLibraryViewModel: MusicLibraryViewModel
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

        callingActivity = activity as MainActivity
        musicLibraryViewModel = ViewModelProvider(callingActivity)[MusicLibraryViewModel::class.java]
        setHasOptionsMenu(true)

        playlistName?.let { name ->
            // TODO: For areas of the codebase like this, can we use a view model method that itself finds the playlist
            //  and extracts their songs in one go? This would save the coroutine code duplication
            //  e.g. see edit playlist fragment
            musicLibraryViewModel.getPlaylistByName(name).observe(viewLifecycleOwner) {
                lifecycleScope.launch(Dispatchers.Main) {
                    playlist = it
                    playlist?.let {
                        // fixme
                        // val editable: Editable = SpannableStringBuilder(it.name)
                        // binding.editPlaylistName.text = editable
                        binding.editPlaylistName.text = SpannableStringBuilder(it.name)

                        val playlistSongIds = PlaylistHelper.extractSongIds(it.songs)
                        if (!ImageHandlingHelper.loadImageByPlaylist(callingActivity.application, it, binding.artwork)) {
                            callingActivity.loadRandomArtworkBySongIds(playlistSongIds, binding.artwork)
                        }
                    }
                }
            }
        }

        binding.artwork.setOnClickListener {
            startActivityForResult(
                Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI), GET_ARTWORK
            )
        }

        binding.editArtworkIcon.setOnClickListener {
            startActivityForResult(
                Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI), GET_ARTWORK
            )
        }

        return binding.root
    }

    override fun onActivityResult(reqCode: Int, resultCode: Int, data: Intent?) {
        if (reqCode == GET_ARTWORK && resultCode == Activity.RESULT_OK) {
            try {
                selectedImageUri = data!!.data
                val source = ImageDecoder.createSource(requireActivity().contentResolver, selectedImageUri!!)
                newArtwork = ImageDecoder.decodeBitmap(source)

                Glide.with(this)
                    .load(selectedImageUri)
                    .centerCrop()
                    .into(binding.artwork)
            } catch (_: FileNotFoundException) { }
            catch (_: IOException) { }
        }

        super.onActivityResult(reqCode, resultCode, data)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.findItem(R.id.search).isVisible = false
        menu.findItem(R.id.save).isVisible = true

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        return when (item.itemId) {
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
                                callingActivity.application, this.playlistId.toString(), it)
                        }

                        musicLibraryViewModel.updatePlaylists(listOf(this))

                        val action = EditPlaylistFragmentDirections.actionFinishEditPlaylist(newPlaylistName)
                        requireView().findNavController().navigate(action)

                        Toast.makeText(activity, getString(R.string.playlist_updated), Toast.LENGTH_SHORT).show()
                    }
                }

                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
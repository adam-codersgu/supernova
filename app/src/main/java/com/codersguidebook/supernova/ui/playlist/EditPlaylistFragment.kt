package com.codersguidebook.supernova.ui.playlist

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.SpannableStringBuilder
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import com.bumptech.glide.Glide
import com.codersguidebook.supernova.*
import com.codersguidebook.supernova.databinding.FragmentEditPlaylistBinding
import com.codersguidebook.supernova.entities.Playlist
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
        musicLibraryViewModel = ViewModelProvider(this).get(MusicLibraryViewModel::class.java)
        setHasOptionsMenu(true)

        val musicDatabase = MusicDatabase.getDatabase(requireContext(), lifecycleScope)
        musicDatabase.playlistDao().findPlaylist(playlistName ?: "").observe(viewLifecycleOwner, { p ->
            p?.let {
                playlist = it
                val editable: Editable = SpannableStringBuilder(it.name)
                binding.editPlaylistName.text = editable

                if (!callingActivity.insertPlaylistArtwork(it, binding.artwork)) {
                    val playlistSongIDs= callingActivity.extractPlaylistSongIds(it.songs)
                    callingActivity.insertArtwork(callingActivity.findFirstSongArtwork(playlistSongIDs[0]), binding.artwork)
                }
            }
        })

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
            } catch (e: FileNotFoundException) { }
            catch (e: IOException) { }
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
                if (playlist != null) {
                    // take user submission for album title or year, or use default values if submission is blank
                    val newName = binding.editPlaylistName.text.toString()

                    // check no fields are blank
                    if (newName.isNotEmpty()) {
                        // check something has actually been changed
                        if (newName != playlist!!.name || newArtwork != null) {
                            playlist!!.name = newName
                            // artwork has been changed
                            if (newArtwork != null) callingActivity.changeArtwork("playlistArt", newArtwork!!, playlist!!.playlistId.toString())

                            musicLibraryViewModel.updatePlaylists(listOf(playlist!!))
                        }

                        val action = EditPlaylistFragmentDirections.actionFinishEditPlaylist(newName)
                        requireView().findNavController().navigate(action)

                        Toast.makeText(activity, "Details saved.", Toast.LENGTH_SHORT).show()
                    } else Toast.makeText(activity, "Check none of the fields are empty.", Toast.LENGTH_SHORT).show()
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
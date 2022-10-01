package com.codersguidebook.supernova.ui.album

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
import androidx.navigation.findNavController
import com.bumptech.glide.Glide
import com.codersguidebook.supernova.MainActivity
import com.codersguidebook.supernova.MusicLibraryViewModel
import com.codersguidebook.supernova.R
import com.codersguidebook.supernova.databinding.FragmentEditAlbumBinding
import com.codersguidebook.supernova.entities.Song
import com.codersguidebook.supernova.ui.albums.AlbumsFragmentDirections
import java.io.FileNotFoundException
import java.io.IOException

class EditAlbumFragment : Fragment() {

    companion object {
        private const val GET_ARTWORK = 1
    }

    private var albumID: String? = null
    private var _binding: FragmentEditAlbumBinding? = null
    private val binding get() = _binding!!
    private var newArtwork: Bitmap? = null
    private var selectedImageUri: Uri? = null
    private var albumSongs = emptyList<Song>()
    private lateinit var callingActivity: MainActivity
    private lateinit var musicLibraryViewModel: MusicLibraryViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        arguments?.let {
            val safeArgs = EditAlbumFragmentArgs.fromBundle(it)
            albumID = safeArgs.albumID
        }

        _binding = FragmentEditAlbumBinding.inflate(inflater, container, false)

        callingActivity = activity as MainActivity
        setHasOptionsMenu(true)

        musicLibraryViewModel = ViewModelProvider(this)[MusicLibraryViewModel::class.java]
        musicLibraryViewModel.allSongs.observe(viewLifecycleOwner, { songs ->
            songs?.let {
                this.albumSongs = it.filter {song ->
                    song.albumId == albumID
                }
                var editable: Editable = SpannableStringBuilder(albumSongs[0].albumName)
                binding.editAlbumTitle.text = editable

                editable = SpannableStringBuilder(albumSongs[0].year)
                binding.editAlbumYear.text = editable
            }
        })

        // retrieve artwork of first song
        callingActivity.insertArtwork(albumID, binding.editAlbumArtwork)
        binding.editAlbumArtwork.setOnClickListener {
            startActivityForResult(
                Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI), GET_ARTWORK
            )
        }

        binding.editAlbumArtworkIcon.setOnClickListener {
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
                    .into(binding.editAlbumArtwork)
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
                if (!albumSongs.isNullOrEmpty()) {
                    // take user submission for album title or year, or use default values if submission is blank
                    val newAlbum = binding.editAlbumTitle.text.toString()
                    val newYear = binding.editAlbumYear.text.toString()

                    // check no fields are blank
                    if (newAlbum.isNotEmpty() && newYear.isNotEmpty()) {
                        // check something has actually been changed
                        if (newAlbum != albumSongs[0].albumName || newYear != albumSongs[0].year || newArtwork != null) {

                            // artwork has been changed
                            if (newArtwork != null) callingActivity.changeArtwork("albumArt", newArtwork!!, albumID!!)

                            val updatedAlbumSongs = mutableListOf<Song>()
                            for (s in albumSongs) {
                                s.albumName = newAlbum
                                s.year = newYear
                                updatedAlbumSongs.add(s)
                            }

                            callingActivity.updateSongInfo(updatedAlbumSongs)
                        }

                        val action = AlbumsFragmentDirections.actionFinishEditAlbum(albumID!!)
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
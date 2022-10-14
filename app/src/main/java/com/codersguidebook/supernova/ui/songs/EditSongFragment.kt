package com.codersguidebook.supernova.ui.songs

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.SpannableStringBuilder
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.bumptech.glide.Glide
import com.codersguidebook.supernova.MainActivity
import com.codersguidebook.supernova.R
import com.codersguidebook.supernova.entities.Song
import com.codersguidebook.supernova.databinding.FragmentEditSongBinding
import com.codersguidebook.supernova.ui.albums.AlbumsFragmentDirections
import java.io.FileNotFoundException
import java.io.IOException

class EditSongFragment : Fragment() {

    private var _binding: FragmentEditSongBinding? = null
    private val binding get() = _binding!!
    private var song: Song? = null
    private var newArtwork: Bitmap? = null
    private lateinit var callingActivity: MainActivity

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        arguments?.let {
            val safeArgs = EditSongFragmentArgs.fromBundle(it)
            song = safeArgs.song
        }

        _binding = FragmentEditSongBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)
        callingActivity = activity as MainActivity

        // Retrieve the song's album artwork
        callingActivity.insertArtwork(song!!.albumId, binding.editSongArtwork)
        binding.editSongArtwork.setOnClickListener {
            startActivityForResult(Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI), 1)
        }

        binding.editSongArtworkIcon.setOnClickListener {
            startActivityForResult(Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI), 1)
        }

        var editable: Editable = SpannableStringBuilder(song!!.title)
        binding.editSongTitle.text = editable

        editable = SpannableStringBuilder(song!!.artist)
        binding.editSongArtist.text = editable

        editable = SpannableStringBuilder(song!!.track.toString().substring(0, 1))
        binding.editSongDisc.text = editable

        editable = SpannableStringBuilder(song!!.track.toString().substring(1, 4).toInt().toString())
        binding.editSongTrack.text = editable

        editable = SpannableStringBuilder(song!!.year)
        binding.editSongYear.text = editable

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onActivityResult(reqCode: Int, resultCode: Int, data: Intent?) {
        if (reqCode == 1 && resultCode == Activity.RESULT_OK) {
            try {
                val selectedImageUri = data!!.data
                val source = ImageDecoder.createSource(requireActivity().contentResolver, selectedImageUri!!)
                newArtwork = ImageDecoder.decodeBitmap(source)

                Glide.with(this)
                    .load(selectedImageUri)
                    .centerCrop()
                    .into(binding.editSongArtwork)

            } catch (e: FileNotFoundException) {
            } catch (e: IOException) { }
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
                // take user submission for album title or year, or use default values if submission is blank
                val newTitle = binding.editSongTitle.text.toString()
                val newArtist = binding.editSongArtist.text.toString()
                val newDisc = binding.editSongDisc.text.toString()
                val newTrack = binding.editSongTrack.text.toString()
                val newYear = binding.editSongYear.text.toString()

                // check no fields are blank
                if (newTitle.isNotEmpty() && newArtist.isNotEmpty() && newDisc.isNotEmpty() && newTrack.isNotEmpty() && newYear.isNotEmpty()) {
                    // check something has actually been changed
                    val completeTrack = when (newTrack.length) {
                        3 -> newDisc + newTrack
                        2 -> newDisc + "0" + newTrack
                        else -> newDisc + "00" + newTrack
                    }.toInt()
                    if (newTitle != song!!.title || newArtist != song!!.artist || completeTrack != song!!.track || newYear != song!!.year || newArtwork != null) {

                        // artwork has been changed
                        if (newArtwork != null) callingActivity.changeArtwork("albumArt", newArtwork!!, song?.albumId!!)

                        song!!.title = newTitle
                        song!!.artist = newArtist
                        song!!.track = completeTrack
                        song!!.year = newYear

                        callingActivity.updateSongInfo(listOf(song!!))
                    }

                    val action = AlbumsFragmentDirections.actionSelectAlbum(song?.albumId!!)
                    requireView().findNavController().navigate(action)

                    Toast.makeText(activity, getString(R.string.details_saved), Toast.LENGTH_SHORT).show()
                } else Toast.makeText(activity, getString(R.string.check_fields_not_empty), Toast.LENGTH_SHORT).show()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }
}
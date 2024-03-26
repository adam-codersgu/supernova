package com.codersguidebook.supernova.ui.songs

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
import com.codersguidebook.supernova.databinding.FragmentEditSongBinding
import com.codersguidebook.supernova.entities.Song
import com.codersguidebook.supernova.fragment.BaseEditMusicFragment
import com.codersguidebook.supernova.utils.ImageHandlingHelper

class EditSongFragment : BaseEditMusicFragment() {

    override var _binding: ViewBinding? = null
        get() = field as FragmentEditSongBinding?
    override val binding: FragmentEditSongBinding
        get() = _binding!! as FragmentEditSongBinding
    private var song: Song? = null

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

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ImageHandlingHelper.loadImageByAlbumId( mainActivity.application, song?.albumId,
            binding.editSongArtwork)
        binding.editSongArtwork.setOnClickListener { getImage() }

        binding.editSongArtworkIcon.setOnClickListener { getImage() }

        binding.editSongTitle.text = SpannableStringBuilder(song?.title)
        binding.editSongArtist.text = SpannableStringBuilder(song?.artist)
        binding.editSongDisc.text = SpannableStringBuilder(song?.track.toString().substring(0, 1))
        binding.editSongTrack.text = SpannableStringBuilder(song?.track.toString().substring(1, 4)
            .toInt().toString())
        binding.editSongYear.text = SpannableStringBuilder(song?.year)
        binding.rememberPlaybackProgress.isSelected = song?.rememberProgress ?: false
    }

    override fun menuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.save -> {
                val newTitle = binding.editSongTitle.text.toString()
                val newArtist = binding.editSongArtist.text.toString()
                val newDisc = binding.editSongDisc.text.toString()
                val newTrack = binding.editSongTrack.text.toString()
                val newYear = binding.editSongYear.text.toString()
                val newRememberProgress = binding.rememberPlaybackProgress.isSelected

                // check no fields are blank
                if (newTitle.isNotEmpty() && newArtist.isNotEmpty() && newDisc.isNotEmpty() && newTrack.isNotEmpty() && newYear.isNotEmpty()) {
                    // check something has actually been changed
                    val completeTrack = when (newTrack.length) {
                        3 -> newDisc + newTrack
                        2 -> newDisc + "0" + newTrack
                        else -> newDisc + "00" + newTrack
                    }.toInt()
                    if (newTitle != song!!.title || newArtist != song!!.artist || completeTrack != song!!.track || newYear != song!!.year || newArtwork != null
                        || newRememberProgress != song!!.rememberProgress) {

                        // artwork has been changed
                        newArtwork?.let { artwork ->
                            ImageHandlingHelper.saveAlbumArtByResourceId( mainActivity.application,
                                song?.albumId!!, artwork)
                        }

                        song!!.title = newTitle
                        song!!.artist = newArtist
                        song!!.track = completeTrack
                        song!!.year = newYear

                        if (newRememberProgress != song!!.rememberProgress) song!!.resetProgress()
                        song!!.rememberProgress = newRememberProgress

                        mainActivity.updateSongs(listOf(song!!))
                    }

                    Toast.makeText(activity, getString(R.string.details_saved), Toast.LENGTH_SHORT).show()
                    requireView().findNavController().popBackStack()
                } else Toast.makeText(activity, getString(R.string.check_fields_not_empty), Toast.LENGTH_SHORT).show()
                true
            }
            else -> false
        }
    }

    override fun furtherUriProcessing(uri: Uri) {
        Glide.with(this)
            .load(uri)
            .centerCrop()
            .into(binding.editSongArtwork)
    }
}
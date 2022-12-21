package com.codersguidebook.supernova.ui.artist

import android.os.Bundle
import android.text.Editable
import android.text.SpannableStringBuilder
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.codersguidebook.supernova.MainActivity
import com.codersguidebook.supernova.MusicDatabase
import com.codersguidebook.supernova.R
import com.codersguidebook.supernova.databinding.FragmentEditArtistBinding
import com.codersguidebook.supernova.entities.Song

class EditArtistFragment : Fragment() {

    private var artistName: String? = null
    private var _binding: FragmentEditArtistBinding? = null
    private val binding get() = _binding!!
    private var musicDatabase: MusicDatabase? = null
    private var artistsSongs = emptyList<Song>()
    private lateinit var callingActivity: MainActivity

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        arguments?.let {
            val safeArgs = EditArtistFragmentArgs.fromBundle(it)
            artistName = safeArgs.artist
        }

        _binding = FragmentEditArtistBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)
        
        callingActivity = activity as MainActivity
        
        musicDatabase = MusicDatabase.getDatabase(requireContext(), lifecycleScope)
        musicDatabase!!.musicDao().findArtistsSongs(artistName!!)
            .observe(viewLifecycleOwner) {
                artistsSongs = it
            }

        val editable: Editable = SpannableStringBuilder(artistName)
        binding.editArtistName.text = editable

        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.findItem(R.id.search).isVisible = false
        menu.findItem(R.id.save).isVisible = true

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        return when (item.itemId) {
            R.id.save -> {
                val newName = binding.editArtistName.text.toString()

                when {
                    newName.isEmpty() -> Toast.makeText(activity, getString(R.string.artist_name_cannot_be_empty),
                        Toast.LENGTH_SHORT).show()
                    newName == artistName -> Toast.makeText(activity, getString(R.string.artist_name_not_changed),
                        Toast.LENGTH_SHORT).show()
                    else -> {
                        for (song in artistsSongs) song.artist = newName
                        callingActivity.updateSongs(artistsSongs)
                        Toast.makeText(activity, getString(R.string.artist_updated),
                            Toast.LENGTH_SHORT).show()
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
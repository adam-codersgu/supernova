package com.codersguidebook.supernova.ui.artist

import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.*
import android.widget.Toast
import androidx.viewbinding.ViewBinding
import com.codersguidebook.supernova.R
import com.codersguidebook.supernova.databinding.FragmentEditArtistBinding
import com.codersguidebook.supernova.entities.Song
import com.codersguidebook.supernova.fragment.BaseFragment

class EditArtistFragment : BaseFragment() {

    override var _binding: ViewBinding? = null
        get() = field as FragmentEditArtistBinding?
    override val binding: FragmentEditArtistBinding
        get() = _binding!! as FragmentEditArtistBinding

    private var artistName: String? = null
    private var artistSongs = emptyList<Song>()

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

        binding.editArtistName.text = SpannableStringBuilder(artistName)

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)

        artistName?.let { name ->
            musicLibraryViewModel.setActiveArtistName(name)

            musicLibraryViewModel.activeArtistSongs.observe(viewLifecycleOwner) { songs ->
                artistSongs = songs
            }
        }
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
                        for (song in artistSongs) song.artist = newName
                        mainActivity.updateSongs(artistSongs)
                        Toast.makeText(activity, getString(R.string.artist_updated),
                            Toast.LENGTH_SHORT).show()
                    }
                }

                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }
}
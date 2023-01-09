package com.codersguidebook.supernova.dialogs

import android.app.Dialog
import android.os.Bundle
import android.support.v4.media.session.MediaSessionCompat.QueueItem
import androidx.core.view.isGone
import androidx.navigation.findNavController
import androidx.viewbinding.ViewBinding
import com.codersguidebook.supernova.R
import com.codersguidebook.supernova.databinding.QueueOptionsBinding
import com.codersguidebook.supernova.fragment.BaseDialogFragment
import com.codersguidebook.supernova.ui.albums.AlbumsFragmentDirections
import com.codersguidebook.supernova.ui.artists.ArtistsFragmentDirections

class QueueOptions(private val queueItem: QueueItem,
                   private val currentlyPlaying: Boolean) : BaseDialogFragment() {

    override var _binding: ViewBinding? = null
        get() = field as QueueOptionsBinding?
    override val binding: QueueOptionsBinding
        get() = _binding!! as QueueOptionsBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = QueueOptionsBinding.inflate(inflater)

        val queueItemDescription = queueItem.description

        binding.optionsTitle.text = queueItemDescription.title

        binding.artist.setOnClickListener{
            val action = ArtistsFragmentDirections
                .actionSelectArtist(queueItemDescription.subtitle.toString())
            mainActivity.findNavController(R.id.nav_host_fragment).navigate(action)
            dismiss()
        }

        binding.album.setOnClickListener{
            queueItemDescription.extras?.getString("album_id")?.let {
                val action = AlbumsFragmentDirections.actionSelectAlbum(it)
                mainActivity.findNavController(R.id.nav_host_fragment).navigate(action)
            }
            dismiss()
        }

        binding.addPlaylist.setOnClickListener{
            queueItemDescription.mediaId?.let {
                mainActivity.openAddToPlaylistDialogForSongById(it.toLong())
            }
            dismiss()
        }

        if (currentlyPlaying) binding.removeSong.isGone = true
        else {
            binding.removeSong.setOnClickListener {
                mainActivity.removeQueueItemById(queueItem.queueId)
                dismiss()
            }
        }

        return super.onCreateDialog(savedInstanceState)
    }
}
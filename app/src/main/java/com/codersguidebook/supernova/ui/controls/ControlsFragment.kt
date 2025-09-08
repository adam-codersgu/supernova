package com.codersguidebook.supernova.ui.controls

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaMetadata
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.codersguidebook.supernova.PlayQueueViewModel
import com.codersguidebook.supernova.R
import com.codersguidebook.supernova.databinding.PlayerControlsBinding
import com.codersguidebook.supernova.fragment.BaseFragment
import com.codersguidebook.supernova.params.MediaServiceConstants.Companion.ALBUM_ID
import com.codersguidebook.supernova.utils.ImageHandlingHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ControlsFragment : BaseFragment() {
    
    override var _binding: ViewBinding? = null
        get() = field as PlayerControlsBinding?
    override val binding: PlayerControlsBinding
        get() = _binding!! as PlayerControlsBinding
    private var fastForwarding = false
    private var fastRewinding = false
    private val playQueueViewModel: PlayQueueViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View {
        _binding = PlayerControlsBinding.inflate(inflater, container, false)
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        playQueueViewModel.currentlyPlayingSongMetadata.observe(viewLifecycleOwner) {
            updateCurrentlyDisplayedMetadata(it)
        }

        playQueueViewModel.isPlaying.observe(viewLifecycleOwner) { playing ->
            if (playing) binding.btnPlay.setImageResource(R.drawable.ic_pause)
            else binding.btnPlay.setImageResource(R.drawable.ic_play)
        }

        playQueueViewModel.playbackPosition.observe(viewLifecycleOwner) {
            binding.songProgressBar.progress = it
        }

        playQueueViewModel.playbackDuration.observe(viewLifecycleOwner) {
            binding.songProgressBar.max = it
        }

        binding.btnPlay.setOnClickListener {
            mainActivity.playPauseControl()
        }

        binding.btnBackward.setOnClickListener{
            if (fastRewinding) fastRewinding = false
            else mainActivity.skipBack()
        }

        binding.btnBackward.setOnLongClickListener {
            fastRewinding = true
            lifecycleScope.launch {
                do {
                    mainActivity.fastRewind()
                    delay(500)
                } while (fastRewinding)
            }
            return@setOnLongClickListener false
        }

        binding.btnForward.setOnClickListener{
            if (fastForwarding) fastForwarding = false
            else mainActivity.skipForward()
        }

        binding.btnForward.setOnLongClickListener {
            fastForwarding = true
            lifecycleScope.launch {
                do {
                    mainActivity.fastForward()
                    delay(500)
                } while (fastForwarding)
            }
            return@setOnLongClickListener false
        }

        binding.songInfo.setOnClickListener {
            playQueueViewModel.currentlyPlayingSongMetadata.value?.let {
                val extras = FragmentNavigatorExtras(
                    binding.artwork to binding.artwork.transitionName,
                    binding.title to binding.title.transitionName,
                    binding.album to binding.album.transitionName,
                    binding.artist to binding.artist.transitionName,
                    binding.btnPlay to binding.btnPlay.transitionName,
                    binding.btnBackward to binding.btnBackward.transitionName,
                    binding.btnForward to binding.btnForward.transitionName
                )
                findNavController().navigate(R.id.nav_currently_playing, null, null, extras)
            }
        }
    }

    override fun onResume() {
        super.onResume()

        binding.songProgressBar.max = playQueueViewModel.playbackDuration.value ?: 0
        binding.songProgressBar.progress = playQueueViewModel.playbackPosition.value ?: 0
    }

    /**
     * Use the currently playing song's metadata to update the user interface.
     *
     * @param metadata MediaMetadataCompat object detailing the currently playing song's metadata, or null
     * if playback has stopped and any loaded metadata should be cleared.
     */
    private fun updateCurrentlyDisplayedMetadata(metadata: MediaMetadata?) {
        binding.title.text = metadata?.title
        binding.artist.text = metadata?.artist
        binding.album.text = metadata?.albumTitle

        if (metadata != null) {
            val albumId = metadata.extras?.getString(ALBUM_ID)
            ImageHandlingHelper.loadImageByAlbumId(mainActivity.application, albumId, binding.artwork)
        } else {
            Glide.with(mainActivity)
                .clear(binding.artwork)
        }
    }
}
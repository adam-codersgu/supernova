package com.codersguidebook.supernova.ui.controls

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.codersguidebook.supernova.MainActivity
import com.codersguidebook.supernova.PlayQueueViewModel
import com.codersguidebook.supernova.R
import com.codersguidebook.supernova.databinding.PlayerControlsBinding
import com.codersguidebook.supernova.entities.Song
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ControlsFragment : Fragment() {

    private var _binding: PlayerControlsBinding? = null
    private val binding get() = _binding!!
    private var currentSong: Song? = null
    private var fastForwarding = false
    private var fastRewinding = false
    private val playQueueViewModel: PlayQueueViewModel by activityViewModels()
    private lateinit var callingActivity: MainActivity

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View {
        _binding = PlayerControlsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        callingActivity = activity as MainActivity

        playQueueViewModel.playQueue.observe(viewLifecycleOwner) {
            updateCurrentlyDisplayedSong()
        }

        playQueueViewModel.currentQueueItemId.observe(viewLifecycleOwner) {
            updateCurrentlyDisplayedSong()
        }

        playQueueViewModel.isPlaying.observe(viewLifecycleOwner) { isPlaying ->
            if (isPlaying) binding.btnPlay.setImageResource(R.drawable.ic_pause)
            else binding.btnPlay.setImageResource(R.drawable.ic_play)
        }

        playQueueViewModel.playbackPosition.observe(viewLifecycleOwner) {
            binding.songProgressBar.progress = it
        }

        playQueueViewModel.playbackDuration.observe(viewLifecycleOwner) {
            binding.songProgressBar.max = it
        }

        binding.btnPlay.setOnClickListener {
            callingActivity.playPauseControl()
        }

        binding.btnBackward.setOnClickListener{
            if (fastRewinding) fastRewinding = false
            else callingActivity.skipBack()
        }

        binding.btnBackward.setOnLongClickListener {
            fastRewinding = true
            lifecycleScope.launch {
                do {
                    callingActivity.fastRewind()
                    delay(500)
                } while (fastRewinding)
            }
            return@setOnLongClickListener false
        }

        binding.btnForward.setOnClickListener{
            if (fastForwarding) fastForwarding = false
            else callingActivity.skipForward()
        }

        binding.btnForward.setOnLongClickListener {
            fastForwarding = true
            lifecycleScope.launch {
                do {
                    callingActivity.fastForward()
                    delay(500)
                } while (fastForwarding)
            }
            return@setOnLongClickListener false
        }

        binding.songInfo.setOnClickListener {
            currentSong?.let {
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

        updateCurrentlyDisplayedSong()
    }

    override fun onResume() {
        super.onResume()

        binding.songProgressBar.max = playQueueViewModel.playbackDuration.value ?: 0
        binding.songProgressBar.progress = playQueueViewModel.playbackPosition.value ?: 0
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /** Use the currently playing song's metadata to update the user interface. */
    private fun updateCurrentlyDisplayedSong() {
        val currentMediaId = playQueueViewModel.getCurrentQueueItem()?.description?.mediaId?.toLong()
        if (currentSong?.songId != currentMediaId) {
            currentSong = if (currentMediaId == null) null
            else callingActivity.getSongById(currentMediaId)
        }

        if (binding.title.text != currentSong?.title || binding.artist.text != currentSong?.artist
            || binding.album.text != currentSong?.albumName) {
            binding.title.text = currentSong?.title
            binding.artist.text = currentSong?.artist
            binding.album.text = currentSong?.albumName
            callingActivity.insertArtwork(currentSong?.albumId, binding.artwork)
        }

        if (currentSong == null) {
            Glide.with(callingActivity)
                .clear(binding.artwork)
        }
    }
}
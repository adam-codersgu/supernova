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
import com.codersguidebook.supernova.*
import com.codersguidebook.supernova.databinding.PlayerControlsBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ControlsFragment : Fragment() {

    private var _binding: PlayerControlsBinding? = null
    private val binding get() = _binding!!
    private var fastForwarding = false
    private var fastRewinding = false
    private val playQueueViewModel: PlayQueueViewModel by activityViewModels()
    private lateinit var callingActivity: MainActivity

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = PlayerControlsBinding.inflate(inflater, container, false)
        callingActivity = activity as MainActivity
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        playQueueViewModel.currentlyPlayingSong.observe(viewLifecycleOwner, {
            if (it != null) {
                binding.title.text = it.title
                binding.artist.text = it.artist
                binding.album.text = it.albumName
                callingActivity.insertArtwork(it.albumId, binding.artwork)
                binding.songInfo.setOnClickListener {
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
            } else {
                binding.title.text = null
                binding.artist.text = null
                binding.album.text = null
                Glide.with(callingActivity)
                    .clear(binding.artwork)
                binding.songInfo.setOnClickListener(null)
            }
        })

        playQueueViewModel.isPlaying.observe(viewLifecycleOwner, { isPlaying ->
            isPlaying?.let {
                if (it) binding.btnPlay.setImageResource(R.drawable.ic_pause)
                else binding.btnPlay.setImageResource(R.drawable.ic_play)
            }
        })

        playQueueViewModel.playbackPosition.observe(viewLifecycleOwner, { position ->
            position?.let {
                binding.songProgressBar.progress = position
            }
        })

        // keep track of currently playing song duration
        playQueueViewModel.playbackDuration.observe(viewLifecycleOwner, { duration ->
            duration?.let {
                binding.songProgressBar.max = it
            }
        })

        // play/pause btn actions
        binding.btnPlay.setOnClickListener {
            callingActivity.playPauseControl()
        }

        /* binding.btnBackward.setOnTouchListener { v, event ->
            if (event?.action == MotionEvent.ACTION_UP) {
                if (fastRewinding) fastRewinding = false
                else callingActivity.skipBack()
            }

            v?.onTouchEvent(event) ?: true
        } */
        // FIXME - See if it works fine without onTouchListener

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
}
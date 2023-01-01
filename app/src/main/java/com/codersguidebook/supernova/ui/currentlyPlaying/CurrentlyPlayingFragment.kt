package com.codersguidebook.supernova.ui.currentlyPlaying

import android.annotation.SuppressLint
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat.*
import android.transition.TransitionInflater
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.codersguidebook.supernova.MobileNavigationDirections
import com.codersguidebook.supernova.PlayQueueViewModel
import com.codersguidebook.supernova.R
import com.codersguidebook.supernova.SettingsActivity
import com.codersguidebook.supernova.databinding.FragmentCurrentlyPlayingBinding
import com.codersguidebook.supernova.entities.Song
import com.codersguidebook.supernova.fragment.BaseFragment
import com.codersguidebook.supernova.params.SharedPreferencesConstants.Companion.ANIMATION_ACTIVE
import com.codersguidebook.supernova.params.SharedPreferencesConstants.Companion.ANIMATION_TYPE
import com.codersguidebook.supernova.params.SharedPreferencesConstants.Companion.CUSTOM_ANIMATION_IMAGE_IDS
import com.codersguidebook.supernova.params.SharedPreferencesConstants.Companion.REPEAT_MODE
import com.codersguidebook.supernova.params.SharedPreferencesConstants.Companion.SHUFFLE_MODE
import com.codersguidebook.supernova.utils.ImageHandlingHelper
import com.codersguidebook.supernova.views.PlaybackAnimator
import com.codersguidebook.supernova.views.PullToCloseLayout
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class CurrentlyPlayingFragment : BaseFragment(), PullToCloseLayout.Listener, PlaybackAnimator.Listener {

    private var animationObjectIsDragging = false
    private val playQueueViewModel: PlayQueueViewModel by activityViewModels()
    private var currentSong: Song? = null
    override var _binding: ViewBinding? = null
        get() = field as FragmentCurrentlyPlayingBinding?
    override val binding: FragmentCurrentlyPlayingBinding
        get() = _binding!! as FragmentCurrentlyPlayingBinding
    private var isAnimationVisible = true
    private var pullToCloseIsDragging = false
    private var fastForwarding = false
    private var fastRewinding = false
    private lateinit var onBackPressedCallback: OnBackPressedCallback
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition = TransitionInflater.from(context)
            .inflateTransition(android.R.transition.move)
        sharedElementReturnTransition = TransitionInflater.from(context)
            .inflateTransition(android.R.transition.move)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        _binding = FragmentCurrentlyPlayingBinding.inflate(inflater, container, false)
        binding.root.setListener(this)
        binding.animatedView.setListener(this)

        onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                this@CurrentlyPlayingFragment.pullToCloseDismissed()
            }
        }

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mainActivity.onBackPressedDispatcher.addCallback(viewLifecycleOwner, onBackPressedCallback)

        // The onTouch event should only be propogated to the PullToClose view if a custom animation object
        // is not being dragged.
        binding.root.setOnTouchListener { _, _ ->
            isAnimationVisible && animationObjectIsDragging
        }

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity())

        playQueueViewModel.currentlyPlayingSongMetadata.observe(viewLifecycleOwner) {
            updateCurrentlyDisplayedMetadata(it)
        }

        playQueueViewModel.playbackState.observe(viewLifecycleOwner) { state ->
            if (state == STATE_PLAYING) binding.btnPlay.setImageResource(R.drawable.ic_pause)
            else binding.btnPlay.setImageResource(R.drawable.ic_play)
        }

        playQueueViewModel.playbackDuration.observe(viewLifecycleOwner) {
            binding.currentSeekBar.max = it
            binding.currentMax.text = SimpleDateFormat("mm:ss", Locale.UK).format(it)
        }

        playQueueViewModel.playbackPosition.observe(viewLifecycleOwner) {
            if (pullToCloseIsDragging) return@observe
            binding.currentSeekBar.progress = it
            binding.currentPosition.text = SimpleDateFormat("mm:ss", Locale.UK).format(it)
        }

        binding.btnPlay.setOnClickListener { mainActivity.playPauseControl() }

        binding.btnBackward.setOnClickListener {
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

        binding.currentFavourite.setOnClickListener {
            currentSong?.apply {
                musicLibraryViewModel.toggleSongFavouriteStatus(this)
                this.isFavourite = !this.isFavourite
                setFavouriteButtonStyle(this.isFavourite)
            }
        }

        val shuffleMode = sharedPreferences.getInt(SHUFFLE_MODE, SHUFFLE_MODE_NONE)
        setShuffleButtonAppearance(shuffleMode)

        binding.currentButtonShuffle.setOnClickListener {
            setShuffleButtonAppearance(mainActivity.toggleShuffleMode())
        }
        
        val repeatMode = sharedPreferences.getInt(REPEAT_MODE, REPEAT_MODE_NONE)
        setRepeatButtonAppearance(repeatMode)

        binding.currentButtonRepeat.setOnClickListener {
            setRepeatButtonAppearance(mainActivity.toggleRepeatMode())
        }

        binding.currentAddToPlaylist.setOnClickListener {
            currentSong?.let { song -> mainActivity.openAddToPlaylistDialog(listOf(song)) }
        }

        binding.currentClose.setOnClickListener { this.pullToCloseDismissed() }

        binding.currentSettings.setOnClickListener { showSettingsPopup() }

        binding.artwork.setOnClickListener { showSettingsPopup() }

        binding.currentSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) mainActivity.seekTo(progress)
            }
        })
    }

    override fun onStart() {
        super.onStart()

        isAnimationVisible = sharedPreferences.getBoolean(ANIMATION_ACTIVE, true)
        if (isAnimationVisible) binding.animatedView.visibility = View.VISIBLE
        else binding.animatedView.visibility = View.GONE

        val customDrawableString = sharedPreferences.getString(CUSTOM_ANIMATION_IMAGE_IDS, null)
        val animationPreference = sharedPreferences.getString(ANIMATION_TYPE, null)
        when {
            customDrawableString != null && animationPreference == getString(R.string.custom_image) -> {
                val listType = object : TypeToken<List<String>>() {}.type
                val imageIds: List<String> = Gson().fromJson(customDrawableString, listType)
                setCustomDrawables(imageIds)
            }
            animationPreference == getString(R.string.custom_image) -> {
                sharedPreferences.edit().apply {
                    putString(ANIMATION_TYPE, getString(R.string.leaves))
                    apply()
                }
                Toast.makeText(activity, getString(R.string.error_custom_animation_image_not_found), Toast.LENGTH_LONG).show()
                binding.animatedView.changeDrawable(getString(R.string.leaves), false)
            }
            animationPreference != null -> binding.animatedView.changeDrawable(animationPreference)
        }
    }

    /**
     * Use the currently playing song's metadata to update the user interface.
     *
     * @param metadata - MediaMetadataCompat object detailing the currently playing song's metadata, or null
     * if playback has stopped and any loaded metadata should be cleared.
     */
    private fun updateCurrentlyDisplayedMetadata(metadata: MediaMetadataCompat?) = lifecycleScope.launch(Dispatchers.Main) {
        val currentMediaId = metadata?.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)?.toLong()
        currentSong = withContext(Dispatchers.IO) {
            if (currentMediaId != null) musicLibraryViewModel.getSongById(currentMediaId)
            else null
        }

        setFavouriteButtonStyle(currentSong?.isFavourite ?: false)

        binding.title.text = metadata?.getString(MediaMetadataCompat.METADATA_KEY_TITLE)
        binding.artist.text = metadata?.getString(MediaMetadataCompat.METADATA_KEY_ARTIST)
        binding.album.text = metadata?.getString(MediaMetadataCompat.METADATA_KEY_ALBUM)

        if (metadata != null) {
            val albumId = metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI)
            ImageHandlingHelper.loadImageByAlbumId(mainActivity.application, albumId, binding.artwork)
        } else {
            Glide.with(mainActivity)
                .clear(binding.artwork)
        }
    }

    /**
     * Set the style of the favourite button based on whether the currently playing
     * song is a favourite.
     *
     * @param isFavourite - A Boolean indicating whether the currently playing song is
     * a favourite.
     */
    private fun setFavouriteButtonStyle(isFavourite: Boolean) {
        if (isFavourite) {
            binding.currentFavourite.setImageResource(R.drawable.ic_heart)
            binding.currentFavourite.setColorFilter(ContextCompat.getColor(requireActivity(), R.color.accent))
        } else {
            binding.currentFavourite.setImageResource(R.drawable.ic_heart_border)
            binding.currentFavourite.setColorFilter(ContextCompat.getColor(requireActivity(), R.color.onSurface60))
        }
    }

    /**
     * Set a list of custom image URIs as the animation images.
     *
     * @param imageIds - The IDs of the user's custom animation images.
     */
    private fun setCustomDrawables(imageIds: List<String>) {
        val directory = ContextWrapper(mainActivity).getDir("customAnimation", Context.MODE_PRIVATE)
        val drawables = imageIds.mapNotNull { id ->
            val imageFile = File(directory, "$id.jpg")
            Drawable.createFromPath(imageFile.path)
        }

        if (drawables.size != imageIds.size) {
            sharedPreferences.edit().apply {
                putString(CUSTOM_ANIMATION_IMAGE_IDS, null)
                apply()
            }
            Toast.makeText(activity, getString(R.string.error_custom_animation_image_not_found), Toast.LENGTH_LONG).show()
            binding.animatedView.changeDrawable(getString(R.string.leaves), true)
            return
        }

        binding.animatedView.drawableList = drawables
        binding.animatedView.usingCustomDrawable = true
    }

    override fun onResume() {
        super.onResume()
        mainActivity.hideStatusBars(true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mainActivity.hideStatusBars(false)
        onBackPressedCallback.remove()
    }

    /**
     * Set the tint of the shuffle button based on the active shuffle mode.
     *
     * @param shuffleMode - An Integer representing the active shuffle mode preference.
     */
    private fun setShuffleButtonAppearance(shuffleMode: Int) {
        if (shuffleMode == SHUFFLE_MODE_ALL) {
            binding.currentButtonShuffle.setColorFilter(ContextCompat.getColor(requireActivity(), R.color.accent))
        } else binding.currentButtonShuffle.setColorFilter(ContextCompat.getColor(requireActivity(), R.color.onSurface60))
    }

    /**
     * Set the tint and drawable of the repeat button based on the active repeat mode.
     *
     * @param repeatMode - An Integer representing the active repeat mode preference.
     */
    private fun setRepeatButtonAppearance(repeatMode: Int) {
        when (repeatMode) {
            REPEAT_MODE_NONE -> {
                binding.currentButtonRepeat.setImageDrawable(ContextCompat.getDrawable(requireActivity(), R.drawable.ic_repeat))
                binding.currentButtonRepeat.setColorFilter(ContextCompat.getColor(requireActivity(), R.color.onSurface60))
            }
            REPEAT_MODE_ALL -> {
                binding.currentButtonRepeat.setImageDrawable(ContextCompat.getDrawable(requireActivity(), R.drawable.ic_repeat))
                binding.currentButtonRepeat.setColorFilter(ContextCompat.getColor(requireActivity(), R.color.accent))
            }
            REPEAT_MODE_ONE -> {
                binding.currentButtonRepeat.setImageDrawable(ContextCompat.getDrawable(requireActivity(), R.drawable.ic_repeat_one))
                binding.currentButtonRepeat.setColorFilter(ContextCompat.getColor(requireActivity(), R.color.accent))
            }
        }
    }

    private fun showSettingsPopup() {
        PopupMenu(this.context, binding.currentSettings).apply {
            inflate(R.menu.currently_playing_menu)

            setForceShowIcon(true)

            setOnMenuItemClickListener { menuItem ->
                val editor = sharedPreferences.edit()
                when (menuItem.itemId) {
                    R.id.search -> {
                        findNavController().popBackStack()
                        mainActivity.findNavController(R.id.nav_host_fragment).navigate(R.id.nav_search)
                    }
                    R.id.queue -> {
                        findNavController().popBackStack()
                        mainActivity.findNavController(R.id.nav_host_fragment).navigate(R.id.nav_queue)
                    }
                    R.id.artist -> {
                        currentSong?.let {
                            findNavController().popBackStack()
                            val action = MobileNavigationDirections.actionSelectArtist(it.artist)
                            mainActivity.findNavController(R.id.nav_host_fragment).navigate(action)
                        }
                    }
                    R.id.album -> {
                        currentSong?.let {
                            findNavController().popBackStack()
                            val action = MobileNavigationDirections.actionSelectAlbum(it.albumId)
                            mainActivity.findNavController(R.id.nav_host_fragment).navigate(action)
                        }
                    }
                    R.id.animation_play -> {
                        if (isAnimationVisible) {
                            binding.animatedView.pause()
                            binding.animatedView.visibility = View.GONE
                            isAnimationVisible = false
                            editor.putBoolean(ANIMATION_ACTIVE, false)
                        } else {
                            binding.animatedView.start()
                            binding.animatedView.visibility = View.VISIBLE
                            isAnimationVisible = true
                            editor.putBoolean(ANIMATION_ACTIVE, true)
                        }
                        editor.apply()
                    }
                    R.id.animation_red -> binding.animatedView.changeColour(getString(R.string.red), true)
                    R.id.animation_blue -> binding.animatedView.changeColour(getString(R.string.blue), true)
                    R.id.animation_night -> binding.animatedView.changeColour(getString(R.string.night), true)
                    R.id.animation_pastel -> binding.animatedView.changeColour(getString(R.string.pastel), true)
                    R.id.animation_leaf -> binding.animatedView.changeDrawable(getString(R.string.leaves), true)
                    R.id.animation_space -> binding.animatedView.changeDrawable(getString(R.string.space), true)
                    R.id.animation_mandala -> binding.animatedView.changeDrawable(getString(R.string.mandala), true)
                    R.id.animation_animal -> binding.animatedView.changeDrawable(getString(R.string.animals), true)
                    R.id.animation_flower -> binding.animatedView.changeDrawable(getString(R.string.flowers), true)
                    R.id.animation_instruments -> binding.animatedView.changeDrawable(getString(R.string.instruments), true)
                    R.id.animation_custom -> {
                        val customDrawableString = sharedPreferences.getString(CUSTOM_ANIMATION_IMAGE_IDS, null)
                        if (customDrawableString != null) {
                            editor.putString(ANIMATION_TYPE, getString(R.string.custom_image))
                            editor.apply()
                            val listType = object : TypeToken<List<String>>() {}.type
                            val imageIds: List<String> = Gson().fromJson(customDrawableString, listType)
                            setCustomDrawables(imageIds)
                            Toast.makeText(mainActivity, getString(R.string.changes_applied),
                                Toast.LENGTH_SHORT).show()
                        } else {
                            findNavController().popBackStack()
                            mainActivity.findNavController(R.id.nav_host_fragment).navigate(R.id.nav_custom_animation)
                        }
                    }
                    R.id.animation_fast -> binding.animatedView.changeSpeed(getString(R.string.fast), true)
                    R.id.animation_normal -> binding.animatedView.changeSpeed(getString(R.string.normal), true)
                    R.id.animation_slow -> binding.animatedView.changeSpeed(getString(R.string.slow), true)
                    R.id.change_custom_image -> {
                        findNavController().popBackStack()
                        mainActivity.findNavController(R.id.nav_host_fragment)
                            .navigate(R.id.nav_custom_animation)
                    }
                    R.id.menu_more_settings -> {
                        val intent = Intent(requireActivity(), SettingsActivity::class.java)
                        startActivity(intent)
                    }
                }
                true
            }
            show()
        }
    }

    override fun pullToCloseIsDragging(dragging: Boolean) {
        pullToCloseIsDragging = dragging
    }

    override fun pullToCloseDismissed() {
        findNavController().popBackStack()
    }

    override fun animationObjectIsDragging(dragging: Boolean) {
        animationObjectIsDragging = dragging
    }
}
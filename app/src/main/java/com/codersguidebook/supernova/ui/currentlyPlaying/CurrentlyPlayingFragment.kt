package com.codersguidebook.supernova.ui.currentlyPlaying

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.ImageDecoder
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v4.media.session.PlaybackStateCompat.*
import android.transition.TransitionInflater
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.SeekBar
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import com.codersguidebook.supernova.*
import com.codersguidebook.supernova.databinding.FragmentCurrentlyPlayingBinding
import com.codersguidebook.supernova.entities.Song
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class CurrentlyPlayingFragment : Fragment() {

    private val playQueueViewModel: PlayQueueViewModel by activityViewModels()
    private var currentSong: Song? = null
    private var _binding: FragmentCurrentlyPlayingBinding? = null
    private val binding get() = _binding!!
    private var isAnimationVisible = true
    private var fastForwarding = false
    private var fastRewinding = false
    private lateinit var callingActivity: MainActivity
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
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.root.setOnTouchListener { _, _ -> return@setOnTouchListener true }

        callingActivity = activity as MainActivity
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity())
        
        playQueueViewModel.playQueue.observe(viewLifecycleOwner) {
            updateCurrentlyDisplayedSong()
        }

        playQueueViewModel.currentQueueItemId.observe(viewLifecycleOwner) {
            updateCurrentlyDisplayedSong()
        }

        playQueueViewModel.isPlaying.observe(viewLifecycleOwner) {
            if (it) binding.btnPlay.setImageResource(R.drawable.ic_pause)
            else binding.btnPlay.setImageResource(R.drawable.ic_play)
        }

        playQueueViewModel.playbackDuration.observe(viewLifecycleOwner) {
            binding.currentSeekBar.max = it
            binding.currentMax.text = SimpleDateFormat("mm:ss", Locale.UK).format(it)
        }

        playQueueViewModel.playbackPosition.observe(viewLifecycleOwner) {
            binding.currentSeekBar.progress = it
            binding.currentPosition.text = SimpleDateFormat("mm:ss", Locale.UK).format(it)
        }

        binding.btnPlay.setOnClickListener{ callingActivity.playPauseControl() }

        binding.btnBackward.setOnClickListener {
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

        binding.currentFavourite.setOnClickListener {
            setFavouriteButtonStyle(callingActivity.toggleSongFavouriteStatus(currentSong))
        }

        val shuffleMode = sharedPreferences.getInt("shuffleMode", SHUFFLE_MODE_NONE)
        setShuffleButtonAppearance(shuffleMode)

        binding.currentButtonShuffle.setOnClickListener{
            setShuffleButtonAppearance(callingActivity.toggleShuffleMode())
        }
        
        val repeatMode = sharedPreferences.getInt("repeatMode", REPEAT_MODE_NONE)
        setRepeatButtonAppearance(repeatMode)

        binding.currentButtonRepeat.setOnClickListener {
            setRepeatButtonAppearance(callingActivity.toggleRepeatMode())
        }

        binding.currentAddToPlaylist.setOnClickListener {
            currentSong?.let {
                callingActivity.openAddToPlaylistDialog(listOf(it))
            }
        }

        binding.currentClose.setOnClickListener { findNavController().popBackStack() }

        binding.currentSettings.setOnClickListener { showSettingsPopup() }

        binding.artwork.setOnClickListener { showSettingsPopup() }

        binding.currentSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) callingActivity.seekTo(progress)
            }
        })

        updateCurrentlyDisplayedSong()
    }

    override fun onStart() {
        super.onStart()

        binding.animatedView.viewWidth = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            callingActivity.windowManager.currentWindowMetrics.bounds.width()
        } else {
            val displayMetrics = DisplayMetrics()
            callingActivity.windowManager.defaultDisplay.getMetrics(displayMetrics)
            displayMetrics.widthPixels
        }

        isAnimationVisible = sharedPreferences.getBoolean("playAnimations", true)
        if (isAnimationVisible) binding.animatedView.visibility = View.VISIBLE
        else binding.animatedView.visibility = View.GONE

        val customDrawableString = sharedPreferences.getString("customAnimationUri", null)
        val animationPreference = sharedPreferences.getString("drawableAnimations", getString(R.string.leaves))
        when {
            customDrawableString != null && animationPreference == getString(R.string.custom_image) -> {
                val listType = object : TypeToken<List<String>>() {}.type
                val imageStrings: List<String> = Gson().fromJson(customDrawableString, listType)
                val list = mutableListOf<Uri>()
                for (s in imageStrings)  list.add(Uri.parse(s))
                setCustomDrawable(list)
            }
            animationPreference == getString(R.string.custom_image) -> {
                sharedPreferences.edit().apply {
                    putString("drawableAnimations", getString(R.string.leaves))
                    apply()
                }
                Toast.makeText(activity, getString(R.string.no_custom_image), Toast.LENGTH_LONG).show()
                binding.animatedView.changeDrawable(getString(R.string.leaves), false)
            }
            else -> binding.animatedView.changeDrawable(animationPreference!!, false)
        }
        val colourAnimations = sharedPreferences.getString("colourAnimations", getString(R.string.red))
        binding.animatedView.changeColour(colourAnimations!!, false)
        val speedAnimations = sharedPreferences.getString("speedAnimations", getString(R.string.normal))
        binding.animatedView.changeSpeed(speedAnimations!!, false)
        binding.animatedView.spinSpeed = sharedPreferences.getInt("spinAnimations", 20)
        val numberAnimations = sharedPreferences.getInt("numberAnimations", 6)
        binding.animatedView.objectList = arrayOfNulls(numberAnimations)
        binding.animatedView.createObjects()
    }

    /**
     * Use the currently playing song's metadata to update the user interface.
     *
     */
    private fun updateCurrentlyDisplayedSong() {
        val oldCurrentSong = currentSong
        val currentQueueItemId = playQueueViewModel.currentQueueItemId.value
        currentSong = if (currentQueueItemId == null) null
        else callingActivity.getSongById(currentQueueItemId)

        if (oldCurrentSong?.songId == currentSong?.songId) return

        binding.title.text = currentSong?.title
        binding.artist.text = currentSong?.artist
        binding.album.text = currentSong?.albumName
        callingActivity.insertArtwork(currentSong?.albumId, binding.artwork)

        if (currentSong?.isFavourite == true) setFavouriteButtonStyle(true)
        else setFavouriteButtonStyle(false)
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
     * @param uris - The URIs leading to the user's custom animation images.
     */
    private fun setCustomDrawable(uris: MutableList<Uri>) {
        fun removeUri(uri: Uri) {
            uris.remove(uri)
            val editor = sharedPreferences.edit()
            if (uris.isEmpty()) {
                editor.putString("drawableAnimations", getString(R.string.leaves))
                editor.putString("customAnimationUri", null)
                binding.animatedView.usingCustomDrawable = false
            } else {
                val gPretty = GsonBuilder().setPrettyPrinting().create().toJson(uris)
                editor.putString("customAnimationUri", gPretty)
            }
            editor.apply()
            // TODO: Improve error reporting and handling for URIs that cannot be found
            Toast.makeText(activity, getString(R.string.custom_image_not_found), Toast.LENGTH_LONG).show()
        }
        val drawables = arrayListOf<Drawable?>()
        for (uri in uris) {
            if (uri.path?.let { File(it).isFile } == true) {
                val source = ImageDecoder.createSource(requireActivity().contentResolver, uri)
                drawables.add(ImageDecoder.decodeDrawable(source))
            } else removeUri(uri)
        }
        binding.animatedView.drawableList = drawables
        binding.animatedView.usingCustomDrawable = true
    }

    override fun onResume() {
        super.onResume()
        callingActivity.hideSystemBars(true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        callingActivity.hideSystemBars(false)
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
        PopupMenu(requireContext(), binding.currentSettings).apply {
            inflate(R.menu.currently_playing_menu)

            setForceShowIcon(true)

            setOnDismissListener { callingActivity.hideSystemBars(true) }

            setOnMenuItemClickListener { menuItem ->
                val editor = sharedPreferences.edit()
                when (menuItem.itemId) {
                    R.id.search -> {
                        findNavController().popBackStack()
                        callingActivity.findNavController(R.id.nav_host_fragment).navigate(R.id.nav_search)
                    }
                    R.id.queue -> {
                        findNavController().popBackStack()
                        callingActivity.findNavController(R.id.nav_host_fragment).navigate(R.id.nav_queue)
                    }
                    R.id.artist -> {
                        currentSong?.let {
                            findNavController().popBackStack()
                            val action = MobileNavigationDirections.actionSelectArtist(it.artist)
                            callingActivity.findNavController(R.id.nav_host_fragment).navigate(action)
                        }
                    }
                    R.id.album -> {
                        currentSong?.let {
                            findNavController().popBackStack()
                            val action = MobileNavigationDirections.actionSelectAlbum(it.albumId)
                            callingActivity.findNavController(R.id.nav_host_fragment).navigate(action)
                        }
                    }
                    R.id.animation_play -> {
                        if (isAnimationVisible) {
                            binding.animatedView.pause()
                            binding.animatedView.visibility = View.GONE
                            isAnimationVisible = false
                            editor.putBoolean("playAnimations", false)
                        } else {
                            binding.animatedView.start()
                            binding.animatedView.visibility = View.VISIBLE
                            isAnimationVisible = true
                            editor.putBoolean("playAnimations", true)
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
                        val customDrawableString = sharedPreferences.getString("customAnimationUri", null)
                        if (customDrawableString != null) {
                            editor.putString("drawableAnimations", getString(R.string.custom_image))
                            editor.apply()
                            val listType = object : TypeToken<List<String>>() {}.type
                            val imageStrings: List<String> = Gson().fromJson(customDrawableString, listType)
                            val list = mutableListOf<Uri>()
                            for (s in imageStrings)  list.add(Uri.parse(s))
                            setCustomDrawable(list)
                            Toast.makeText(callingActivity, getString(R.string.changes_applied),
                                Toast.LENGTH_SHORT).show()
                        } else {
                            findNavController().popBackStack()
                            callingActivity.findNavController(R.id.nav_host_fragment).navigate(R.id.nav_custom_animation)
                        }
                    }
                    R.id.animation_fast -> binding.animatedView.changeSpeed(getString(R.string.fast), true)
                    R.id.animation_normal -> binding.animatedView.changeSpeed(getString(R.string.normal), true)
                    R.id.animation_slow -> binding.animatedView.changeSpeed(getString(R.string.slow), true)
                    R.id.change_custom_image -> {
                        findNavController().popBackStack()
                        callingActivity.findNavController(R.id.nav_host_fragment)
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
}
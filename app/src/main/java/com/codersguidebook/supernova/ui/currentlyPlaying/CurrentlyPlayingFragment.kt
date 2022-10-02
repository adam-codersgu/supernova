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
import java.io.FileNotFoundException
import java.lang.reflect.InvocationTargetException
import java.text.SimpleDateFormat
import java.util.*

class CurrentlyPlayingFragment : Fragment() {

    private val playQueueViewModel: PlayQueueViewModel by activityViewModels()
    private var currentlyPlayingSong: Song? = null
    private var _binding: FragmentCurrentlyPlayingBinding? = null
    private val binding get() = _binding!!
    private var isAnimationVisible = true
    private var fastForwarding = false
    private var fastRewinding = false
    private lateinit var callingActivity: MainActivity
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition = TransitionInflater.from(context).inflateTransition(android.R.transition.move)
        sharedElementReturnTransition = TransitionInflater.from(context).inflateTransition(android.R.transition.move)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCurrentlyPlayingBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.root.setOnTouchListener { _, _ ->
            return@setOnTouchListener true
        }

        callingActivity = activity as MainActivity
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity())
        
        // get information about the currently playing song
        playQueueViewModel.currentlyPlayingSong.observe(viewLifecycleOwner, { song ->
            song?.let {
                currentlyPlayingSong = it
                binding.title.text = it.title
                binding.album.text = it.albumName
                binding.artist.text = it.artist
                callingActivity.insertArtwork(it.albumId, binding.artwork)

                if (it.isFavourite) {
                    binding.currentFavourite.setImageResource(R.drawable.ic_heart)
                    binding.currentFavourite.setColorFilter(ContextCompat.getColor(requireActivity(), R.color.accent))
                } else {
                    binding.currentFavourite.setImageResource(R.drawable.ic_heart_border)
                    binding.currentFavourite.setColorFilter(ContextCompat.getColor(requireActivity(), R.color.onSurface60))
                }
                binding.currentFavourite.setOnClickListener {
                    val added = callingActivity.updateFavourites(song)
                    when {
                        added!! -> {
                            binding.currentFavourite.setImageDrawable(ContextCompat.getDrawable(requireActivity(), R.drawable.ic_heart))
                            binding.currentFavourite.setColorFilter(ContextCompat.getColor(requireActivity(), R.color.accent))
                        }
                        !added -> {
                            binding.currentFavourite.setImageDrawable(ContextCompat.getDrawable(requireActivity(), R.drawable.ic_heart_border))
                            binding.currentFavourite.setColorFilter(ContextCompat.getColor(requireActivity(), R.color.onSurface60))
                        }
                    }
                }
            }
        })

        // check whether a song is currently playing
        playQueueViewModel.isPlaying.observe(viewLifecycleOwner, { playing ->
            playing?.let {
                if (it) binding.btnPlay.setImageResource(R.drawable.ic_pause)
                else binding.btnPlay.setImageResource(R.drawable.ic_play)
            }
        })

        // keep track of currently playing song duration
        playQueueViewModel.currentPlaybackDuration.observe(viewLifecycleOwner, { duration ->
            duration?.let {
                binding.currentSeekBar.max = it
                binding.currentMax.text = SimpleDateFormat("mm:ss", Locale.UK).format(it)
            }
        })

        // keep track of currently playing song position
        playQueueViewModel.currentPlaybackPosition.observe(viewLifecycleOwner, { position ->
            position?.let {
                binding.currentSeekBar.progress = position
                binding.currentPosition.text = SimpleDateFormat("mm:ss", Locale.UK).format(it)
            }
        })

        // toggle play/pause
        binding.btnPlay.setOnClickListener{ callingActivity.playPauseControl() }

        // restart or play previous song when backward button is clicked
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

        // skip to next song when forward button is pressed
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
            if (currentlyPlayingSong != null) callingActivity.openAddToPlaylistDialog(listOf(currentlyPlayingSong!!))
        }

        binding.currentClose.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.currentSettings.setOnClickListener {
            showPopup(it)
        }

        binding.artwork.setOnClickListener {
            showPopup(binding.currentSettings)
        }

        binding.currentSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) callingActivity.seekTo(progress)
            }
        })
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
        
        val playAnimations = sharedPreferences.getBoolean("playAnimations", true)
        if (playAnimations) {
            binding.animatedView.visibility = View.VISIBLE
            isAnimationVisible = true
        } else {
            binding.animatedView.visibility = View.GONE
            isAnimationVisible = false
        }
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
                val editor = sharedPreferences.edit()
                editor.putString("drawableAnimations", getString(R.string.leaves))
                editor.apply()
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

    private fun setCustomDrawable(uris: MutableList<Uri>): Boolean {
        fun error(position: Int) {
            uris.removeAt(position)
            val editor = sharedPreferences.edit()
            if (uris.isEmpty()) {
                editor.putString("drawableAnimations", getString(R.string.leaves))
                editor.putString("customAnimationUri", null)
                binding.animatedView.usingCustomDrawable = false
                Toast.makeText(activity, getString(R.string.custom_image_not_found), Toast.LENGTH_LONG).show()
            } else {
                val gPretty = GsonBuilder().setPrettyPrinting().create().toJson(uris)
                editor.putString("customAnimationUri", gPretty)
            }
            editor.apply()
        }
        val drawables = arrayListOf<Drawable?>()
        for ((index, u) in uris.withIndex()) {
            try {
                val source = ImageDecoder.createSource(requireActivity().contentResolver, u)
                drawables.add(ImageDecoder.decodeDrawable(source))
            } catch (e: InvocationTargetException) {
                error(index)
            } catch (e: FileNotFoundException) {
                error(index)
            } catch (e: SecurityException) {
                error(index)
            }
        }
        binding.animatedView.drawableList = drawables
        binding.animatedView.usingCustomDrawable = true
        return true
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

    @SuppressLint("DiscouragedPrivateApi")
    private fun showPopup(view: View) {
        PopupMenu(requireContext(), view).apply {
            inflate(R.menu.currently_playing_menu)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                setForceShowIcon(true)
            } else {
                val fMenuHelper = PopupMenu::class.java.getDeclaredField("mPopup")
                fMenuHelper.isAccessible = true
                val menuHelper = fMenuHelper.get(this)
                val argTypes: Array<Class<*>?> = arrayOf(Boolean::class.javaPrimitiveType)
                menuHelper.javaClass.getDeclaredMethod("setForceShowIcon", *argTypes)
                    .invoke(menuHelper, true)
            }

            setOnDismissListener {
                callingActivity.hideSystemBars(true)
            }
            setOnMenuItemClickListener {
                val editor = sharedPreferences.edit()
                when (it.itemId) {
                    R.id.search -> {
                        findNavController().popBackStack()
                        callingActivity.findNavController(R.id.nav_host_fragment).navigate(R.id.nav_search)
                    }
                    R.id.queue -> {
                        findNavController().popBackStack()
                        callingActivity.findNavController(R.id.nav_host_fragment).navigate(R.id.nav_queue)
                    }
                    R.id.artist -> {
                        if (currentlyPlayingSong != null) {
                            findNavController().popBackStack()
                            val action = MobileNavigationDirections.actionSelectArtist(currentlyPlayingSong?.artist!!)
                            callingActivity.findNavController(R.id.nav_host_fragment).navigate(action)
                        }
                    }
                    R.id.album -> {
                        if (currentlyPlayingSong != null) {
                            findNavController().popBackStack()
                            val action = MobileNavigationDirections.actionSelectAlbum(currentlyPlayingSong?.albumId!!)
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
                            if (setCustomDrawable(list)) Toast.makeText(activity, getString(R.string.changes_applied), Toast.LENGTH_SHORT).show()
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
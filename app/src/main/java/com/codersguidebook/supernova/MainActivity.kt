package com.codersguidebook.supernova

import android.app.*
import android.content.*
import android.database.Cursor
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import android.view.Menu
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.SearchView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.view.*
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Player.REPEAT_MODE_ALL
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.common.Player.REPEAT_MODE_ONE
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import com.codersguidebook.supernova.databinding.ActivityMainBinding
import com.codersguidebook.supernova.dialogs.CreatePlaylist
import com.codersguidebook.supernova.entities.Playlist
import com.codersguidebook.supernova.entities.Song
import com.codersguidebook.supernova.entities.SongWithOrderId
import com.codersguidebook.supernova.params.MediaServiceConstants.Companion.NO_ACTION
import com.codersguidebook.supernova.params.MediaServiceConstants.Companion.ORDER_ID
import com.codersguidebook.supernova.params.MediaServiceConstants.Companion.REMEMBER_PROGRESS
import com.codersguidebook.supernova.params.MediaServiceConstants.Companion.SKIP_TO_NEXT
import com.codersguidebook.supernova.params.MediaServiceConstants.Companion.SKIP_TO_PREV
import com.codersguidebook.supernova.params.MediaServiceConstants.Companion.SONG_DELETED
import com.codersguidebook.supernova.params.MediaServiceConstants.Companion.SONG_UPDATED
import com.codersguidebook.supernova.params.SharedPreferencesConstants.Companion.APPLICATION_LANGUAGE
import com.codersguidebook.supernova.params.SharedPreferencesConstants.Companion.CURRENT_QUEUE_ITEM_INDEX
import com.codersguidebook.supernova.params.SharedPreferencesConstants.Companion.DEFAULT_PLAYLIST_LANGUAGE
import com.codersguidebook.supernova.params.SharedPreferencesConstants.Companion.PLAYBACK_POSITION
import com.codersguidebook.supernova.params.SharedPreferencesConstants.Companion.PLAY_QUEUE_ITEMS
import com.codersguidebook.supernova.params.SharedPreferencesConstants.Companion.REPEAT_MODE
import com.codersguidebook.supernova.params.SharedPreferencesConstants.Companion.SHUFFLE_MODE
import com.codersguidebook.supernova.utils.*
import com.codersguidebook.supernova.utils.NotificationHelper.createChannelForMediaPlayerNotification
import com.google.android.material.navigation.NavigationView
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import java.util.stream.IntStream
import kotlin.math.min
import kotlin.streams.toList

class MainActivity : AppCompatActivity() {

    companion object {
        private const val LOG_TAG = "MainActivity"
        private const val SONG_NEARLY_FINISHED_THRESHOLD = 0.98
    }

    private var handler = Handler(Looper.getMainLooper())
    private val playQueueViewModel: PlayQueueViewModel by viewModels()
    private var mediaStoreContentObserver: MediaStoreContentObserver? = null
    private var songCompleted = false
    private lateinit var controller: MediaController
    private lateinit var controllerFuture: ListenableFuture<MediaController>
    private lateinit var musicLibraryViewModel: MusicLibraryViewModel
    private lateinit var searchView: SearchView
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var storagePermissionHelper: StorageAccessPermissionHelper
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private val mediaDeletionLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
            result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            musicLibraryViewModel.songIdToDelete?.let {
                deleteSongById(it)
            }
        }
    }

    private var playbackPositionRunnable = object : Runnable {
        override fun run() {
            try {
                if (controller.isPlaying) {
                    updatePlaybackDurationAndPosition()
                }
            } finally {
                handler.postDelayed(this, 500L)
            }
        }
    }

    private val skipTrackReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                SKIP_TO_NEXT -> skipForward()
                SKIP_TO_PREV -> skipBack()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        storagePermissionHelper = StorageAccessPermissionHelper(this)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        musicLibraryViewModel = ViewModelProvider(this, MusicLibraryViewModel.Factory)[MusicLibraryViewModel::class.java]

        createChannelForMediaPlayerNotification(this)

        val taskDescription = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            // Pre-SDK 33
            @Suppress("DEPRECATION")
            ActivityManager.TaskDescription("Supernova", R.drawable.no_album_artwork,
                getColor(R.color.nav_home))
        } else {
            // SDK 33 and up
            ActivityManager.TaskDescription.Builder()
                .setLabel("Supernova")
                .setIcon(R.drawable.no_album_artwork)
                .setPrimaryColor(getColor(R.color.nav_home))
                .build()
        }

        this.setTaskDescription(taskDescription)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(setOf(R.id.nav_home, R.id.nav_queue, R.id.nav_library, R.id.nav_playlists,
            R.id.nav_playlist, R.id.nav_artists, R.id.nav_artist, R.id.nav_albums, R.id.nav_album, R.id.nav_songs), binding.drawerLayout)

        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.navView.setupWithNavController(navController)

        val onNavigationItemSelectedListener = NavigationView.OnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> navController.navigate(R.id.nav_home)
                R.id.nav_queue -> navController.navigate(R.id.nav_queue)
                R.id.nav_playlists -> {
                    val action = MobileNavigationDirections.actionLibrary(0)
                    navController.navigate(action)
                }
                R.id.nav_artists -> {
                    val action = MobileNavigationDirections.actionLibrary(1)
                    navController.navigate(action)
                }
                R.id.nav_albums -> {
                    val action = MobileNavigationDirections.actionLibrary(2)
                    navController.navigate(action)
                }
                R.id.nav_songs -> {
                    val action = MobileNavigationDirections.actionLibrary(3)
                    navController.navigate(action)
                }
                R.id.nav_settings -> {
                    val intent = Intent(this, SettingsActivity::class.java)
                    startActivity(intent)
                }
            }
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
        binding.navView.setNavigationItemSelectedListener(onNavigationItemSelectedListener)
        binding.navView.itemIconTintList = null

        val handler = Handler(Looper.getMainLooper())
        mediaStoreContentObserver = MediaStoreContentObserver(handler, this).also {
            this.contentResolver.registerContentObserver(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                true, it)
        }

        if (storagePermissionHelper.hasPermissions()) refreshMusicLibrary()
        else storagePermissionHelper.requestPermissions()

        ViewCompat.setOnApplyWindowInsetsListener(binding.body) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = insets.left
                bottomMargin = insets.bottom
                rightMargin = insets.right
                topMargin = insets.top
            }

            WindowInsetsCompat.CONSUMED
        }
    }

    @OptIn(UnstableApi::class)
    override fun onStart() {
        super.onStart()
        val sessionToken = SessionToken(this, ComponentName(this, MediaPlaybackService::class.java))
        controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture.apply {
            addListener({
                controller = get()
                initController()
            }, MoreExecutors.directExecutor())
        }
        val filter = IntentFilter().apply {
            addAction(SKIP_TO_NEXT)
            addAction(SKIP_TO_PREV)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(skipTrackReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            ContextCompat.registerReceiver(this, skipTrackReceiver,
                filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        }
    }

    override fun onPause() {
        super.onPause()
        val currentMediaId = playQueueViewModel.getCurrentSongMediaId()
        musicLibraryViewModel.savePlaybackProgress(currentMediaId!!, controller.currentPosition.toInt())
    }

    override fun onResume() {
        super.onResume()
        volumeControlStream = AudioManager.STREAM_MUSIC
    }

    override fun onRestart() {
        super.onRestart()
        musicLibraryViewModel.setMostPlayedPlaylistTimeframe()
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(skipTrackReceiver)

        sharedPreferences.edit().apply {
            putLong(PLAYBACK_POSITION, controller.currentPosition)
            apply()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        sharedPreferences.edit {
            remove(SHUFFLE_MODE)
        }

        mediaStoreContentObserver?.let {
            this.contentResolver.unregisterContentObserver(it)
        }

        controller.stop()
        controllerFuture.let {
            MediaController.releaseFuture(it)
        }
    }

    private fun initController() {
        controller.addListener(object : Player.Listener {

            override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
                if (newPosition.positionMs < oldPosition.positionMs) songCompleted = false
                updatePlaybackDurationAndPosition()
            }

            override fun onMediaMetadataChanged(metadata: MediaMetadata) {
                super.onMediaMetadataChanged(metadata)
                Log.i("DEBUG", "Received the metadata for: ${metadata.title}")

                songCompleted = false

                if (metadata.extras == null || metadata.extras?.getBoolean(REMEMBER_PROGRESS) == true) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        withContext(Dispatchers.IO) {
                            getSongById(playQueueViewModel.getCurrentSongMediaId() ?: return@withContext null)
                        }?.let { song ->
                            if (song.rememberProgress) {
                                seekTo(song.playbackProgress)
                            } else {
                                withContext(Dispatchers.Main) {
                                    processPendingSeekToRequest()
                                }
                            }
                            playQueueViewModel.currentlyPlayingSongMetadata.postValue(song.getMetadata())
                        }
                    }
                } else {
                    processPendingSeekToRequest()
                    playQueueViewModel.currentlyPlayingSongMetadata.postValue(metadata)
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    playbackPositionRunnable.run()
                    playQueueViewModel.isPlaying.value = controller.isPlaying
                } else if (playbackState == Player.STATE_ENDED) {
                    Log.i("DEBUG", "Playback state is STATE_ENDED")
                    val repeatMode = getRepeatMode()
                    if (repeatMode == REPEAT_MODE_ONE
                        || (!playQueueViewModel.playQueueContainsMoreThanOneSong()
                                && repeatMode == REPEAT_MODE_ALL)) {
                        controller.seekTo(0)
                    } else if (playQueueViewModel.isUpcomingSongsInThePlayQueue()
                        || repeatMode == REPEAT_MODE_ALL) {
                        skipForward()
                    } else {
                        Log.i("DEBUG", "No further songs. Clearing the play queue.")
                        controller.clearMediaItems()
                        handler.removeCallbacks(playbackPositionRunnable)
                        playQueueViewModel.playQueue.value = listOf()
                        playQueueViewModel.playbackDuration.value = 0
                        playQueueViewModel.playbackPosition.value = 0
                        playQueueViewModel.currentlyPlayingSongMetadata.value = null
                        playQueueViewModel.isPlaying.value = false
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playQueueViewModel.isPlaying.value = controller.playWhenReady
            }

            override fun onPlayerError(error: PlaybackException) {
                super.onPlayerError(error)
                Log.e("DEBUG", "Playback error: ${error.message}")
                val message = getString(R.string.error_media_service_default)
                Toast.makeText(application, message, Toast.LENGTH_LONG).show()
                refreshMusicLibrary()
            }
        })

        restoreMediaSession()
    }

    private fun processPendingSeekToRequest() {
        val pendingSeekToPosition = playQueueViewModel.pendingSeekToInstruction.value
        if (pendingSeekToPosition != null) {
            Log.i("DEBUG", "Processing the pending seek to request: $pendingSeekToPosition")
            seekTo(pendingSeekToPosition)
            playQueueViewModel.pendingSeekToInstruction.postValue(null)
        }
    }

    private fun updatePlaybackDurationAndPosition() {
        val duration = controller.duration.toInt()
        val position = controller.currentPosition.toInt()
        playQueueViewModel.playbackDuration.value = duration
        playQueueViewModel.playbackPosition.value = position

        if (controller.isPlaying
            && position >= (duration * SONG_NEARLY_FINISHED_THRESHOLD)
            && !songCompleted) {
            Log.i("DEBUG", "Incrementing the song plays for " +
                    "${playQueueViewModel.currentlyPlayingSongMetadata.value!!.title}")
            val mediaId = playQueueViewModel.getCurrentSongMediaId()
            musicLibraryViewModel.addSongByIdToRecentlyPlayedPlaylist(mediaId!!)
            musicLibraryViewModel.increaseSongPlaysBySongId(mediaId)
            songCompleted = true
        }
    }

    /** Process changes to the user's selected language locale, or load its initial value */
    private fun processLanguageLocale() = lifecycleScope.launch(Dispatchers.IO) {
        val selectedLanguageCode = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        var storedLanguageCode = sharedPreferences.getString(APPLICATION_LANGUAGE,
            getString(R.string.english_code))
        val defaultPlaylistLanguageCode = sharedPreferences.getString(DEFAULT_PLAYLIST_LANGUAGE, null)
        val supportedLanguages = resources.getStringArray(R.array.language_values)
        if (selectedLanguageCode != storedLanguageCode && supportedLanguages.contains(selectedLanguageCode)) {
            sharedPreferences.edit().apply {
                putString(APPLICATION_LANGUAGE, selectedLanguageCode)
                apply()
            }
            storedLanguageCode = selectedLanguageCode
        }

        // Update the names of the default application playlists to reflect the active locale
        if (storedLanguageCode != defaultPlaylistLanguageCode) {
            val defaultPlaylistHelper = DefaultPlaylistHelper(this@MainActivity)
            val allPlaylists = musicLibraryViewModel.getAllPlaylists()
            val playlistsToSave = mutableListOf<Playlist>()
            for (pair in defaultPlaylistHelper.playlistPairs) {
                val playlist = allPlaylists.find { it.playlistId == pair.first }?.apply {
                    this.name = pair.second
                }
                if (playlist != null) playlistsToSave.add(playlist)
            }
            if (playlistsToSave.isNotEmpty()) musicLibraryViewModel.updatePlaylists(playlistsToSave)

            sharedPreferences.edit().apply {
                putString(DEFAULT_PLAYLIST_LANGUAGE, storedLanguageCode)
                apply()
            }
        }
    }

    /**
     * Notify the activity of a change to the media associated with a given content URI. This
     * method is used by MediaStoreContentObserver whenever a given URI is associated with
     * media insertion, deletion or update.
     *
     * @param uri The content URI associated with the change.
     */
    fun handleChangeToContentUri(uri: Uri) = lifecycleScope.launch(Dispatchers.IO) {
        val songId = uri.toString().removePrefix(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.toString() + "/")
        try {
            Log.i(LOG_TAG, "Change to content URI for media ID $songId")
            if (handleFileUpdateByMediaId(songId.toLong()) == SONG_DELETED) {
                playQueueViewModel.removeAllOccurrencesOfSong(songId)
            }
        } catch (_: NumberFormatException) { refreshMusicLibrary() }
    }

    /**
     * Notify the media service that a queue item has been moved.
     *
     * @param oldIndex The original index in the play queue of the item that is being moved.
     * @param newIndex The new index in the play queue that the item should occupy.
     */
    fun notifyQueueItemMoved(oldIndex: Int, newIndex: Int) {
        val playQueue = playQueueViewModel.playQueue.value?.toMutableList() ?: return
        val item = playQueue[oldIndex]

        Log.i("DEBUG", "Moving item from index $oldIndex to index $newIndex")

        playQueue.removeAt(oldIndex)
        playQueue.add(newIndex, item)

        if (oldIndex == playQueueViewModel.currentQueueItemIndex.value) {
            saveCurrentlyPlayingIndex(newIndex)
        }

        saveAndPostPlayQueue(playQueue)
    }

    /** Respond to clicks on the play/pause button **/
    fun playPauseControl() {
        if (!playQueueViewModel.playQueue.value.isNullOrEmpty()) {
            if (controller.isPlaying) {
                controller.pause()
            } else {
                play()
            }
        } else {
            lifecycleScope.launch(Dispatchers.Main) {
                val songs = withContext(Dispatchers.IO) {
                    musicLibraryViewModel.getAllSongsOrderByTitle()
                }
                playNewPlayQueue(songs)
            }
        }
    }

    /**
     * Toggle the shuffle mode.
     *
     * @return A Boolean indicating whether the play queue is now shuffled
     */
    fun toggleShuffleMode(): Boolean {
        val shouldShuffle = !sharedPreferences.getBoolean(SHUFFLE_MODE, false)

        setShuffleMode(shouldShuffle)

        if (!shouldShuffle) {
            Toast.makeText(this, getString(R.string.play_queue_unshuffled), Toast.LENGTH_SHORT).show()
        } else Toast.makeText(this, getString(R.string.play_queue_shuffled), Toast.LENGTH_SHORT).show()

        return shouldShuffle
    }

    /**
     * Save the active shuffle mode and notify the media browser service.
     * N.B. This functionality may be called independently of toggleShuffleMode() e.g. when an
     * album is played on shuffle mode directly from the Album view.
     *
     * @param shuffle A Boolean indicating whether the play queue should be shuffled.
     */
    private fun setShuffleMode(shuffle: Boolean) {
        val playQueue = playQueueViewModel.playQueue.value?.toMutableList() ?: return

        if (shuffle) {
            for ((index, i) in playQueue.withIndex()) {
                i.mediaMetadata.extras?.putInt(ORDER_ID, index)
            }
        }

        val currentQueueItemIndex = playQueueViewModel.currentQueueItemIndex.value ?: return
        val currentQueueItem = playQueue[currentQueueItemIndex]

        val newPlayQueue: MutableList<MediaItem>
        val newIndexOfCurrentlyPlaying: Int
        if (shuffle) {
            Log.i("DEBUG", "Shuffling the play queue")

            playQueue.removeAt(currentQueueItemIndex)
            newPlayQueue = playQueue.shuffled().toMutableList()
            newPlayQueue.add(0, currentQueueItem)
            newIndexOfCurrentlyPlaying = 0
        } else {
            Log.i("DEBUG", "Unshuffling the play queue")

            newPlayQueue = playQueue.sortedBy {
                    i -> i.mediaMetadata.extras?.getInt(ORDER_ID)
            }.toMutableList()

            newIndexOfCurrentlyPlaying = newPlayQueue.indexOfFirst { i ->
                i.mediaMetadata.extras?.getInt(ORDER_ID) == currentQueueItem.mediaMetadata.extras?.getInt(ORDER_ID)
            }

            for (i in newPlayQueue) i.mediaMetadata.extras?.remove(ORDER_ID)
        }

        saveCurrentlyPlayingIndex(newIndexOfCurrentlyPlaying)
        saveAndPostPlayQueue(newPlayQueue)

        sharedPreferences.edit {
            putBoolean(SHUFFLE_MODE, shuffle)
        }
    }

    /**
     * Toggle the repeat mode.
     *
     * @return An Integer representing the active repeat mode preference.
     */
    fun toggleRepeatMode(): Int {
        val newRepeatMode = when (sharedPreferences.getInt(REPEAT_MODE, REPEAT_MODE_OFF)) {
            REPEAT_MODE_OFF -> REPEAT_MODE_ALL
            REPEAT_MODE_ALL -> REPEAT_MODE_ONE
            else -> REPEAT_MODE_OFF
        }

        sharedPreferences.edit().apply {
            putInt(REPEAT_MODE, newRepeatMode)
            apply()
        }

        when (newRepeatMode) {
            REPEAT_MODE_OFF -> Toast.makeText(this, getString(R.string.repeat_mode_none), Toast.LENGTH_SHORT).show()
            REPEAT_MODE_ALL -> Toast.makeText(this, getString(R.string.repeat_mode_all), Toast.LENGTH_SHORT).show()
            REPEAT_MODE_ONE -> Toast.makeText(this, getString(R.string.repeat_mode_one), Toast.LENGTH_SHORT).show()
        }

        return newRepeatMode
    }

    /** Skip back to the previous track in the play queue (or restart the current song if less that five seconds in). */
    fun skipBack() {
        if (controller.currentPosition >= 5000L) {
            seekTo(0)
            return
        }

        if (!playQueueViewModel.playQueueContainsMoreThanOneSong()) {
            return
        }

        val newIndex = if (playQueueViewModel.currentQueueItemIndex.value
            == 0) {
            return
        } else {
            (playQueueViewModel.currentQueueItemIndex.value ?: return) - 1
        }
        saveCurrentlyPlayingItemPrepareAndPlay(newIndex, controller.isPlaying)
    }

    /** Skip forward to the next song in the play queue. */
    fun skipForward() {
        if (!playQueueViewModel.playQueueContainsMoreThanOneSong()) {
            return
        }

        val newIndex = if (playQueueViewModel.currentQueueItemIndex.value
            == (playQueueViewModel.playQueue.value?.size ?: return) - 1) {
            if (getRepeatMode() == REPEAT_MODE_ALL) {
                0
            } else return
        } else {
            (playQueueViewModel.currentQueueItemIndex.value ?: return) + 1
        }
        saveCurrentlyPlayingItemPrepareAndPlay(newIndex, controller.isPlaying)
    }

    private fun saveCurrentlyPlayingItemPrepareAndPlay(index: Int, play: Boolean = true) {
        controller.setMediaItem(playQueueViewModel.playQueue.value?.get(index) ?: return)
        controller.prepare()
        if (play) {
            controller.play()
        }
        saveCurrentlyPlayingIndex(index)
    }

    /** Save the index of the currently playing queue item to the shared preferences file. */
    private fun saveCurrentlyPlayingIndex(index: Int) = lifecycleScope.launch(Dispatchers.IO) {
        playQueueViewModel.currentQueueItemIndex.postValue(index)
        sharedPreferences.edit().apply {
            putInt(CURRENT_QUEUE_ITEM_INDEX, index)
            apply()
        }
    }

    /** Rewind the playback of the current song. */
    fun fastRewind() = controller.seekBack()

    /** Fast forward the playback of the current song. */
    fun fastForward() = controller.seekForward()

    private fun saveAndPostPlayQueue(playQueue: List<MediaItem>) {
        playQueueViewModel.playQueue.value = playQueue
        savePlayQueue(playQueue)
    }

    /**
     * Convert the list of MediaDescriptionCompat objects for each item in the play queue to JSON
     * and save it in the shared preferences file.
     */
    private fun savePlayQueue(playQueue: List<MediaItem>) = lifecycleScope.launch(Dispatchers.IO) {
        try {
            val shuffleModeOn = sharedPreferences.getBoolean(SHUFFLE_MODE, false)
            val songsToSave = if (shuffleModeOn) {
                playQueue.map { i ->
                    val orderId = i.mediaMetadata.extras?.getInt(ORDER_ID)
                    val song = SongHelper.buildFromMediaItem(i)
                    SongWithOrderId(orderId, song)
                }
            } else {
                playQueue.map { i ->
                    SongWithOrderId(null, SongHelper.buildFromMediaItem(i))
                }
            }
            val playQueueJson = GsonBuilder().setPrettyPrinting().create().toJson(songsToSave)
            // Log.i("DEBUG", "Storing the following JSON:\n$playQueueJson")
            sharedPreferences.edit().apply {
                putString(PLAY_QUEUE_ITEMS, playQueueJson)
                apply()
            }
        } catch (_: ConcurrentModificationException) {}
    }

    /**
     * Build a play queue using a list of songs and commence playback.
     *
     * @param songs A list containing Song objects that should be added to the play queue.
     * @param startIndex The index of the play queue element at which playback should begin.
     * Default = 0 (the beginning of the play queue).
     * N.B. If shuffle is true then the startIndex is ignored.
     * @param shuffle Indicates whether the play queue should be shuffled.
     */
    fun playNewPlayQueue(songs: List<Song>, startIndex: Int = 0, shuffle: Boolean = false) {
        if (songs.isEmpty() || startIndex >= songs.size) {
            Toast.makeText(this@MainActivity,
                getString(R.string.error_generic_playback), Toast.LENGTH_LONG).show()
            return
        }

        if (controller.isPlaying) controller.stop()

        val playQueue = if (shuffle) {
            songs.mapIndexed { i, s -> s.getMediaItem(i) }.toList().shuffled()
        } else {
            songs.map { s -> s.getMediaItem() }.toList()
        }

        saveAndPostPlayQueue(playQueue)
        saveCurrentlyPlayingItemPrepareAndPlay(startIndex)

        sharedPreferences.edit {
            putBoolean(SHUFFLE_MODE, shuffle)
        }
    }

    fun play() = controller.play()

    /**
     * Add a list of songs to the play queue. The songs can be added to the end of the play queue
     * or after the currently playing song.
     *
     * @param songs A list containing Song objects that should be added to the play queue.
     * @param addSongsAfterCurrentQueueItem A Boolean indicating whether the songs should be added to
     * after the currently playing queue item. Default value = false.
     */
    fun addSongsToPlayQueue(songs: List<Song>, addSongsAfterCurrentQueueItem: Boolean = false)
            = lifecycleScope.launch(Dispatchers.Default) {
        val shuffleModeOn = sharedPreferences.getBoolean(SHUFFLE_MODE, false)
        val playQueue = playQueueViewModel.playQueue.value?.toMutableList() ?: return@launch

        val mediaItems = if (shuffleModeOn) {
            var lastUsedOrderId = playQueue.mapNotNull { i ->
                i.mediaMetadata.extras?.getInt(ORDER_ID)
            }.maxOf { id -> id }
            songs.map { s -> s.getMediaItem(++lastUsedOrderId) }
        } else songs.map { s -> s.getMediaItem() }

        if (addSongsAfterCurrentQueueItem) {
            playQueue.addAll((playQueueViewModel.currentQueueItemIndex.value
                ?: return@launch) + 1, mediaItems)
        } else {
            playQueue.addAll(mediaItems)
        }

        launch(Dispatchers.Main) toast@ {
            saveAndPostPlayQueue(playQueue)
            val message = when {
                songs.size == 1 -> getString(R.string.song_added_play_queue, songs[0].title)
                songs.size > 1 -> getString(R.string.songs_added_play_queue)
                else -> return@toast
            }
            Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Remove a media item from the play queue based on its index in the play queue.
     *
     * @param index The index of the queue items to be removed.
     */
    fun removeQueueItemByIndex(index: Int) {
        val playQueue = playQueueViewModel.playQueue.value?.toMutableList() ?: return
        val currentIndex = playQueueViewModel.currentQueueItemIndex.value ?: return
        playQueue.removeAt(index)
        saveAndPostPlayQueue(playQueue)

        if (index < currentIndex) {
            saveCurrentlyPlayingIndex(currentIndex - 1)
        }
    }

    /**
     * Set the playback position for the currently playing song to a specific location.
     *
     * @param position The desired playback position.
     */
    fun seekTo(position: Long) {
        controller.seekTo(position)
        updatePlaybackDurationAndPosition()
    }

    /**
     * Skip to a specific item in the play queue based on its index in the play queue.
     *
     * @param targetIndex The index in the queue to skip to.
     */
    fun skipToQueueIndex(targetIndex: Int) = lifecycleScope.launch(Dispatchers.Main) {
        val item = playQueueViewModel.playQueue.value?.get(targetIndex) ?: return@launch
        controller.setMediaItem(item)
        saveCurrentlyPlayingItemPrepareAndPlay(targetIndex)
    }

    /**
     * Load backup art for a playlist based on the artwork associated with a given song within
     * that playlist. If the playlist does not contain any songs, then default art will be displayed.
     *
     * @param songIds A list of song IDs that artwork can be randomly sourced from. The list can be empty.
     * @param view The ImageView widget that the artwork should be rendered in.
     */
    fun loadRandomArtworkBySongIds(songIds: List<Long>, view: ImageView) = lifecycleScope.launch(Dispatchers.Main) {
        val songId = if (songIds.isNotEmpty()) songIds.random()
        else null
        val albumId = if (songId != null) withContext(Dispatchers.IO) {
            getSongById(songId)
        }?.albumId else null
        ImageHandlingHelper.loadImageByAlbumId(application, albumId, view)
    }

    /**
     * Hide/reveal the status bars. If the status bars are hidden, then they can be transiently
     * revealed using a swipe motion.
     *
     * @param hide A Boolean indicating whether the status bars should be hidden (true) or
     * revealed (false)
     */
    fun hideStatusBars(hide: Boolean) {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        if (hide) {
            supportActionBar?.setDisplayShowTitleEnabled(false)
            windowInsetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            windowInsetsController.hide(WindowInsetsCompat.Type.statusBars())

            // Hide the toolbar to prevent the SearchView keyboard inadvertently popping up
            binding.toolbar.isGone = true
        } else {
            supportActionBar?.setDisplayShowTitleEnabled(true)
            windowInsetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
            windowInsetsController.show(WindowInsetsCompat.Type.statusBars())

            binding.toolbar.isVisible = true
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)

        searchView = menu.findItem(R.id.search).actionView as SearchView
        searchView.setOnSearchClickListener {
            findNavController(R.id.nav_host_fragment).navigate(R.id.nav_search)
        }

        return super.onCreateOptionsMenu(menu)
    }

    override fun onSupportNavigateUp(): Boolean {
        iconifySearchView()
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    /** Restore an expanded SearchView to its iconified state. */
    fun iconifySearchView() {
        if (!searchView.isIconified) {
            searchView.isIconified = true
            searchView.onActionViewCollapsed()
        }
    }

    /**
     * Delete a Song object based on its ID.
     *
     * @param songId The media ID of the song to be deleted.
     */
    private fun deleteSongById(songId: Long) {
        musicLibraryViewModel.songIdToDelete = songId
        try {
            val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songId)

            val numberDeleted = application.contentResolver.delete(uri, null, null)
            if (numberDeleted > 0) {
                musicLibraryViewModel.songIdToDelete = null
            }
        } catch(exception: RecoverableSecurityException) {
            val intentSender = exception.userAction.actionIntent.intentSender
            val intentSenderRequest = IntentSenderRequest.Builder(intentSender).build()
            mediaDeletionLauncher.launch(intentSenderRequest)
        }
    }

    /**
     * Delete a collection of songs.
     *
     * @param songs A list of Song objects to be deleted
     */
    fun deleteSongs(songs: List<Song>) {
        val uris = songs.map { song ->
            ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.songId)
        }
        val intentSender = MediaStore.createDeleteRequest(application.contentResolver, uris).intentSender
        val intentSenderRequest = IntentSenderRequest.Builder(intentSender).build()
        mediaDeletionLauncher.launch(intentSenderRequest)
    }

    /**
     * Convenience method to open the 'Add to playlist' dialog when only the ID of
     * the given song is available. For example, QueueItem objects may feature incomplete
     * song metadata.
     *
     * @param songId The ID of the song.
     */
    fun openAddToPlaylistDialogForSongById(songId: Long) = lifecycleScope.launch(Dispatchers.Main) {
        val song = withContext(Dispatchers.IO) {
            getSongById(songId)
        } ?: return@launch
        openAddToPlaylistDialog(listOf(song))
    }

    /**
     * Open a dialog window allowing the user to add songs to new and existing playlists.
     *
     * @param songs The list of Song objects to be added to a playlist.
     */
    fun openAddToPlaylistDialog(songs: List<Song>) = lifecycleScope.launch(Dispatchers.Main) {
        val songIds = songs.map { it.songId }

        val positiveButtonClick = { _: DialogInterface, _: Int ->
            openDialog(CreatePlaylist(songIds))
        }

        val userPlaylists = withContext(Dispatchers.IO) {
            musicLibraryViewModel.getAllUserPlaylists()
        }

        // If the user has not created any playlists then skip straight to the create new playlist dialog
        if (userPlaylists.isEmpty()) {
            openDialog(CreatePlaylist(songIds))
            return@launch
        }

        val userPlaylistNames = userPlaylists.map { it.name }.toTypedArray()

        AlertDialog.Builder(this@MainActivity).apply {
            setTitle(getString(R.string.select_playlist))
            setItems(userPlaylistNames) { _, index ->
                val playlist = userPlaylists[index]
                val playlistSongIds = PlaylistHelper.extractSongIds(playlist.songs)
                playlistSongIds.addAll(songIds)
                musicLibraryViewModel.savePlaylistWithSongIds(playlist, playlistSongIds)

                if (songs.size == 1) Toast.makeText(applicationContext, getString(R.string.song_added_playlist,
                    songs[0].title, playlist.name), Toast.LENGTH_SHORT
                ).show()
                else Toast.makeText(applicationContext,
                    getString(R.string.songs_added_playlist, playlist.name), Toast.LENGTH_SHORT
                ).show()
            }
            setNegativeButton(R.string.cancel) { _, _ -> return@setNegativeButton }
            setPositiveButton(R.string.create_playlist, positiveButtonClick)
            show()
        }
    }

    /**
     * Save updates to song metadata to the database. Also update the play queue (if necessary)
     *
     * @param songs The list of Song objects containing updated metadata.
     */
    fun updateSongs(songs: List<Song>) {
        musicLibraryViewModel.updateSongs(songs)

        val playQueue = playQueueViewModel.playQueue.value?.toMutableList() ?: return
        var playQueueUpdated = false
        for (song in songs) {
            // All occurrences of the song need to be updated in the play queue
            val matchingIndices = IntStream.range(0, playQueue.size)
                .filter { i -> song.songId.toString() == playQueue[i].mediaId }
                .toList()

            if (matchingIndices.isEmpty()) continue

            val shuffleModeOn = sharedPreferences.getBoolean(SHUFFLE_MODE, false)

            for (index in matchingIndices) {
                val mediaItem = if (shuffleModeOn) {
                    val orderId = playQueue[index].mediaMetadata.extras?.getInt(ORDER_ID)
                        ?: continue
                    song.getMediaItem(orderId)
                } else {
                    song.getMediaItem()
                }

                if (index == playQueueViewModel.currentQueueItemIndex.value) {
                    controller.replaceMediaItem(0, mediaItem)
                }

                playQueue[index] = mediaItem
                playQueueUpdated = true
            }
        }

        if (playQueueUpdated) {
            saveAndPostPlayQueue(playQueue)
        }
    }

    /**
     * Open a given Dialog Fragment.
     *
     * @param dialog The dialog fragment to load.
     */
    fun openDialog(dialog: DialogFragment) = dialog.show(supportFragmentManager, "")

    /** Hides the soft input keyboard, which can sometimes obstruct views. */
    fun hideKeyboard() {
        this.currentFocus?.let {
            val inputManager = this.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputManager.hideSoftInputFromWindow(it.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (!storagePermissionHelper.hasPermissions()) {
            Toast.makeText(this, getString(R.string.storage_permission_needed),
                Toast.LENGTH_LONG).show()
            if (!storagePermissionHelper.shouldShowPermissionRationale()) {
                // Permission denied with checking "Do not ask again".
                storagePermissionHelper.launchPermissionSettings()
            }
            finish()
        } else refreshMusicLibrary()
    }

    /** Restore the play queue and playback state from the last save. */
    private fun restoreMediaSession() = lifecycleScope.launch {
        if (playQueueViewModel.playQueue.value != null) return@launch

        Log.i("DEBUG", "Restoring the media session")

        val queueItemPairsJson = sharedPreferences.getString(PLAY_QUEUE_ITEMS, null) ?: return@launch

        val itemType = object : TypeToken<List<SongWithOrderId>>() {}.type

        val songs = Gson().fromJson<List<SongWithOrderId>>(queueItemPairsJson, itemType)
        Log.i("DEBUG", "Retrieved the following JSON:\n$songs")
        if (songs.isEmpty()) return@launch
        val playQueue = mutableListOf<MediaItem>()
        for (s in songs) {
            val newItem = s.song?.getMediaItem(s.orderId) ?: continue
            playQueue.add(newItem)
        }

        playQueueViewModel.playQueue.postValue(playQueue)

        val playbackPosition = sharedPreferences.getLong(PLAYBACK_POSITION, 0L)
        if (playbackPosition != 0L) {
            playQueueViewModel.pendingSeekToInstruction.postValue(playbackPosition)
        }

        val currentQueueItemIndex = min(sharedPreferences.getInt(CURRENT_QUEUE_ITEM_INDEX, -1),
            playQueue.size - 1)
        playQueueViewModel.currentQueueItemIndex.postValue(currentQueueItemIndex)
        if (currentQueueItemIndex != -1) {
            controller.setMediaItem(playQueue[currentQueueItemIndex])
            if (!controller.playWhenReady) controller.prepare()
        }
    }

    private fun getRepeatMode(): Int {
        return sharedPreferences.getInt(REPEAT_MODE, REPEAT_MODE_OFF)
    }

    suspend fun getSongById(songId: Long): Song? {
        return musicLibraryViewModel.getSongById(songId)
    }

    /** Refresh the music library. Add new songs, remove deleted songs, and implement language changes. */
    private fun refreshMusicLibrary() = lifecycleScope.launch(Dispatchers.Default) {
        processLanguageLocale()
        val songsToAddToMusicLibrary = mutableListOf<Song>()

        getMediaStoreCursor()?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val songIds = mutableListOf<Long>()
            while (cursor.moveToNext()) {
                val songId = cursor.getLong(idColumn)
                songIds.add(songId)
                val existingSong = getSongById(songId)
                if (existingSong == null) {
                    val song = createSongFromCursor(cursor)
                    songsToAddToMusicLibrary.add(song)
                }
            }

            val chunksToAddToMusicLibrary = songsToAddToMusicLibrary.chunked(25)
            for (chunk in chunksToAddToMusicLibrary) musicLibraryViewModel.saveSongs(chunk)

            val songs = withContext(Dispatchers.IO) {
                musicLibraryViewModel.getAllSongs()
            }
            val songsToBeDeleted = songs.filterNot { songIds.contains(it.songId) }
            songsToBeDeleted.let {
                for (song in songsToBeDeleted) {
                    musicLibraryViewModel.deleteSong(song)
                    playQueueViewModel.removeAllOccurrencesOfSong(song.songId.toString())
                }
            }
            musicLibraryViewModel.refreshSongOfTheDay()
        }
    }

    /**
     * Obtain a Cursor featuring all music entries in the media store that fulfil a given
     * selection criteria.
     *
     * @param selection The WHERE clause for the media store query.
     * Default = standard WHERE clause that selects only music entries.
     * @param selectionArgs An array of String selection arguments that filter the results
     * that are returned in the Cursor.
     * Default = null (no selection arguments).
     * @return A Cursor object detailing all the relevant media store entries.
     */
    private fun getMediaStoreCursor(selection: String = MediaStore.Audio.Media.IS_MUSIC,
                                    selectionArgs: Array<String>? = null): Cursor? {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.YEAR
        )
        val sortOrder = MediaStore.Audio.Media.TITLE + " ASC"
        return contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )
    }

    /**
     * The content observer has been notified that a given media store record has been
     * inserted, deleted or modified. This method evaluates the appropriate action to take
     * based on the media store record's media ID.
     *
     * @param mediaId The ID of the target media store record
     * @return A response code indicating the action taken,
     */
    private suspend fun handleFileUpdateByMediaId(mediaId: Long): Int {
        val selection = MediaStore.Audio.Media._ID + "=?"
        val selectionArgs = arrayOf(mediaId.toString())
        val cursor = getMediaStoreCursor(selection, selectionArgs)

        val existingSong = getSongById(mediaId)
        when {
            existingSong == null && cursor?.count!! > 0 -> {
                cursor.apply {
                    this.moveToNext()
                    val createdSong = createSongFromCursor(this)
                    musicLibraryViewModel.saveSongs(listOf(createdSong))
                    return SONG_UPDATED
                }
            }
            cursor?.count == 0 -> {
                existingSong?.let {
                    musicLibraryViewModel.deleteSong(existingSong)
                    return SONG_DELETED
                }
            }
        }
        return NO_ACTION
    }

    /**
     * Use the media metadata from an entry in a Cursor object to construct a Song object.
     *
     * @param cursor A Cursor object that is set to the row containing the metadata that a Song
     * object should be constructed for.
     */
    private fun createSongFromCursor(cursor: Cursor): Song {
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val trackColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
        val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
        val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
        val albumIDColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
        val yearColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)

        val id = cursor.getLong(idColumn)
        var trackString = cursor.getString(trackColumn) ?: getString(R.string.default_track_number)

        // The Track value will be stored in the format 1xxx where the first digit is the disc number
        val track = try {
            when (trackString.length) {
                4 -> trackString.toInt()
                in 1..3 -> {
                    val numberNeeded = 4 - trackString.length
                    trackString = when (numberNeeded) {
                        1 -> "1$trackString"
                        2 -> "10$trackString"
                        else -> "100$trackString"
                    }
                    trackString.toInt()
                }
                else -> 1001
            }
        } catch (_: NumberFormatException) {
            // If the Track value is unusual (e.g. you can get stuff like "12/23") then use 1001
            1001
        }

        val title: String? = cursor.getString(titleColumn)
        val artist: String? = cursor.getString(artistColumn)
        val album: String? = cursor.getString(albumColumn)
        val year = cursor.getString(yearColumn) ?: getString(R.string.default_year)
        val albumId = cursor.getString(albumIDColumn) ?: getString(R.string.default_album_id)
        val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)

        if (!ImageHandlingHelper.doesAlbumArtExistByResourceId(application, albumId)) {
            val albumArt = try {
                contentResolver.loadThumbnail(uri, Size(640, 640), null)
            } catch (_: FileNotFoundException) { null }
            albumArt?.let {
                ImageHandlingHelper.saveAlbumArtByResourceId(application, albumId, albumArt)
            }
        }

        return Song(id, track, title, artist, album, albumId, year)
    }
}
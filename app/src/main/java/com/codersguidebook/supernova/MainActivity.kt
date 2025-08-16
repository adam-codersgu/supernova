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
import androidx.media3.common.Timeline
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
import com.codersguidebook.supernova.data.MusicDatabase
import com.codersguidebook.supernova.databinding.ActivityMainBinding
import com.codersguidebook.supernova.dialogs.CreatePlaylist
import com.codersguidebook.supernova.entities.Playlist
import com.codersguidebook.supernova.entities.Song
import com.codersguidebook.supernova.entities.SongWithOrderId
import com.codersguidebook.supernova.params.MediaServiceConstants.Companion.ALBUM_ID
import com.codersguidebook.supernova.params.MediaServiceConstants.Companion.MEDIA_ID
import com.codersguidebook.supernova.params.MediaServiceConstants.Companion.NOTIFICATION_CHANNEL_ID
import com.codersguidebook.supernova.params.MediaServiceConstants.Companion.NO_ACTION
import com.codersguidebook.supernova.params.MediaServiceConstants.Companion.ORDER_ID
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

class MainActivity : AppCompatActivity() {

    private val playQueueViewModel: PlayQueueViewModel by viewModels()
    private var mediaStoreContentObserver: MediaStoreContentObserver? = null
    private var musicDatabase: MusicDatabase? = null
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

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        storagePermissionHelper = StorageAccessPermissionHelper(this)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        musicDatabase = MusicDatabase.getDatabase(this, lifecycleScope)
        musicLibraryViewModel = ViewModelProvider(this)[MusicLibraryViewModel::class.java]

        createChannelForMediaPlayerNotification()

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
    }

    override fun onPause() {
        super.onPause()
        val currentMediaId = playQueueViewModel.currentlyPlayingSongMetadata.value
            ?.extras?.getString(MEDIA_ID)?.toLong() ?: return
        musicLibraryViewModel.savePlaybackProgress(currentMediaId, controller.currentPosition.toInt())
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

    // TODO - HAVE A RUNNABLE THAT WHENEVER WE ARE WITHIN 2%
    private fun initController() {
        controller.addListener(object : Player.Listener {

            override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
                updatePlaybackDurationAndPosition()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                super.onMediaItemTransition(mediaItem, reason)

                saveAndPostPlayQueueIndex(controller.currentMediaItemIndex)
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                super.onShuffleModeEnabledChanged(shuffleModeEnabled)
                Log.i("DEBUG", "Shuffle mode enabled changed to $shuffleModeEnabled")
            }

            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                if (timeline.isEmpty) {
                    Log.i("DEBUG", "The timeline is empty")
                    playQueueViewModel.playQueue.postValue(null)
                } else {
                    if (timeline.periodCount != timeline.windowCount) {
                        Log.e("DEBUG", "The period and window counts do not match." +
                                "Period count: ${timeline.periodCount}" +
                                "Window count: ${timeline.windowCount}")
                        return
                    }
                    val firstPeriodId = timeline.getPeriod(0, Timeline.Period()).id.toString()
                    if (firstPeriodId == "-1") {
                        Log.i("DEBUG", "Skipping timeline update.")
                        return
                    }
                    Log.i("DEBUG", "Processing a timeline update.")
                    
                    val playQueue = mutableListOf<MediaItem>()
                    for (i in 0..<timeline.periodCount) {
                        val mediaItem = timeline.getWindow(i, Timeline.Window()).mediaItem
                        playQueue.add(mediaItem)
                    }

                    playQueueViewModel.playQueue.postValue(playQueue)

                    val pendingSkipToIndex = playQueueViewModel.pendingSkipToInstruction.value
                    if (pendingSkipToIndex != null) {
                        skipToQueueIndex(pendingSkipToIndex)
                    }

                    if (playQueueViewModel.pendingPlayInstruction.value == true) {
                        play()
                        playQueueViewModel.pendingPlayInstruction.postValue(null)
                    }

                    savePlayQueue(playQueue)
                    saveAndPostPlayQueueIndex(controller.currentMediaItemIndex)
                }

                super.onTimelineChanged(timeline, reason)
            }

            override fun onMediaMetadataChanged(metadata: MediaMetadata) {
                super.onMediaMetadataChanged(metadata)
                Log.i("DEBUG", "Received the metadata for: ${metadata.title}")

                val expectedSongName = playQueueViewModel.pendingExpectedMetadata.value
                if (expectedSongName != null) {
                    if (expectedSongName != metadata.title) {
                        Log.i("DEBUG", "The metadata is not for $expectedSongName, " +
                                "so therefore will not be processed.")
                        return
                    } else playQueueViewModel.pendingExpectedMetadata.postValue(null)
                }

                val newMediaId = metadata.extras?.getString(MEDIA_ID)
                val prevMediaId = playQueueViewModel.currentlyPlayingSongMetadata.value?.extras?.getString(MEDIA_ID)
                if (newMediaId != prevMediaId) {
                    processPendingSeekToRequest()

                    lifecycleScope.launch(Dispatchers.IO) {
                        withContext(Dispatchers.IO) {
                            getSongById(newMediaId?.toLong() ?: return@withContext null)
                        }?.let { song ->
                            if (song.rememberProgress) seekTo(song.playbackProgress.toInt())
                        }
                    }
                }

                // todo - have a precaution in here if metadata is missing?
                playQueueViewModel.currentlyPlayingSongMetadata.value = metadata
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    playQueueViewModel.isPlaying.value = controller.isPlaying
                } else if (playbackState == Player.STATE_ENDED) {
                    playQueueViewModel.playbackDuration.value = 0
                    playQueueViewModel.playbackPosition.value = 0
                    playQueueViewModel.currentlyPlayingSongMetadata.value = null
                    playQueueViewModel.isPlaying.value = false
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
            seekTo(pendingSeekToPosition.toInt())
            playQueueViewModel.pendingSeekToInstruction.postValue(null)
        }
    }

    private fun updatePlaybackDurationAndPosition() {
        playQueueViewModel.playbackDuration.value = controller.duration.toInt()
        playQueueViewModel.playbackPosition.value = controller.currentPosition.toInt()
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
        val songIdString = uri.toString().removePrefix(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.toString() + "/")
        try {
            val songId = songIdString.toLong()
            if (handleFileUpdateByMediaId(songId) == SONG_DELETED) findSongIdInPlayQueueToRemove(songId)
        } catch (_: NumberFormatException) { refreshMusicLibrary() }
    }

    /**
     * Search for and remove all instances of a given song from the play queue based on its ID.
     *
     * @param songId The ID of the Song to remove from the play queue.
     */
    private fun findSongIdInPlayQueueToRemove(songId: Long) = lifecycleScope.launch(Dispatchers.Default) {
        var index: Int
        do {
            index = getLastIndexOfQueueItemByMediaId(songId.toString())
            if (index != -1) removeQueueItemByIndex(index)
        } while (index != -1)
    }

    private fun getLastIndexOfQueueItemByMediaId(mediaId: String): Int {
        return playQueueViewModel.playQueue.value?.indexOfLast { mediaItem ->
            mediaItem.mediaId == mediaId
        } ?: -1
    }

    /**
     * Notify the media service that a queue item has been moved.
     *
     * @param oldIndex The original index in the play queue of the item that is being moved.
     * @param newIndex The new index in the play queue that the item should occupy.
     */
    fun notifyQueueItemMoved(oldIndex: Int, newIndex: Int) = controller.moveMediaItem(oldIndex, newIndex)

    /** Respond to clicks on the play/pause button **/
    fun playPauseControl() {
        if (!playQueueViewModel.playQueue.value.isNullOrEmpty()) {
            if (controller.isPlaying) {
                controller.pause()
            } else {
                play()
            }
        } else {
            playNewPlayQueue(musicLibraryViewModel.allSongs.value ?: return)
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
        // todo - test this - may need to pause playback and reseek to your song and playback position
        val orderIdOfCurrentSong = playQueueViewModel.currentlyPlayingSongMetadata.value
            ?.extras?.getInt(ORDER_ID) ?: return
        val newPlayQueue = if (shuffle) {
            playQueueViewModel.playQueue.value?.shuffled()
        } else {
            playQueueViewModel.playQueue.value?.sortedBy {
                i -> i.mediaMetadata.extras?.getInt(ORDER_ID)
            }
        } ?: return

        val currentQueueItemIndex = playQueueViewModel.currentQueueItemIndex.value ?: return
        if (currentQueueItemIndex < newPlayQueue.size - 1) {
            controller.removeMediaItems(currentQueueItemIndex + 1, newPlayQueue.size)
        }
        if (currentQueueItemIndex > 0) {
            controller.removeMediaItems(0, currentQueueItemIndex)
        }

        val indexOfCurrentlyPlayingInNewQueue = newPlayQueue.indexOfLast {
                i -> i.mediaMetadata.extras?.getInt(ORDER_ID) == orderIdOfCurrentSong
        }
        if (indexOfCurrentlyPlayingInNewQueue < newPlayQueue.size - 1) {
            controller.addMediaItems(indexOfCurrentlyPlayingInNewQueue + 1,
                newPlayQueue.subList(indexOfCurrentlyPlayingInNewQueue + 1, newPlayQueue.size))
        }
        if (currentQueueItemIndex > 0) {
            controller.addMediaItems(0,
                newPlayQueue.subList(0, indexOfCurrentlyPlayingInNewQueue))
        }

        sharedPreferences.edit().apply {
            putBoolean(SHUFFLE_MODE, shuffle)
            apply()
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

        controller.repeatMode = newRepeatMode

        when (newRepeatMode) {
            REPEAT_MODE_OFF -> Toast.makeText(this, getString(R.string.repeat_mode_none), Toast.LENGTH_SHORT).show()
            REPEAT_MODE_ALL -> Toast.makeText(this, getString(R.string.repeat_mode_all), Toast.LENGTH_SHORT).show()
            REPEAT_MODE_ONE -> Toast.makeText(this, getString(R.string.repeat_mode_one), Toast.LENGTH_SHORT).show()
        }

        return newRepeatMode
    }

    /** Skip back to the previous track in the play queue (or restart the current song if less that five seconds in). */
    fun skipBack() = controller.seekToPreviousMediaItem()

    /** Skip forward to the next song in the play queue. */
    fun skipForward() = controller.seekToNextMediaItem()

    /** Rewind the playback of the current song. */
    fun fastRewind() = controller.seekBack()

    /** Fast forward the playback of the current song. */
    fun fastForward() = controller.seekForward()

    /**
     * Convert the list of MediaDescriptionCompat objects for each item in the play queue to JSON
     * and save it in the shared preferences file.
     */
    private fun savePlayQueue(playQueue: List<MediaItem>) = lifecycleScope.launch(Dispatchers.IO) {
        try {
            val songsToSave = playQueue.map { i ->
                val orderId = i.mediaMetadata.extras?.getInt(ORDER_ID)
                val song = buildSongFromMediaItem(i)
                SongWithOrderId(orderId, song)
            }
            val playQueueJson = GsonBuilder().setPrettyPrinting().create().toJson(songsToSave)
            sharedPreferences.edit().apply {
                putString(PLAY_QUEUE_ITEMS, playQueueJson)
                apply()
            }
        } catch (_: ConcurrentModificationException) {}
    }

    // fixme - don't want this to return null, throw an exception or log error instead
    private fun buildSongFromMediaItem(mediaItem: MediaItem): Song? {
        val metadata = mediaItem.mediaMetadata
        val extras = metadata.extras ?: return null
        return Song(mediaItem.mediaId.toLong(), 0, metadata.title.toString(),
            metadata.artist.toString(), metadata.albumTitle.toString(),
            extras.getString(ALBUM_ID, "-1"), "0")
    }

    /** Save the index of the currently playing queue item to the shared preferences file. */
    private fun saveAndPostPlayQueueIndex(index: Int) = lifecycleScope.launch(Dispatchers.IO) {
        if (index != playQueueViewModel.currentQueueItemIndex.value) {
            playQueueViewModel.currentQueueItemIndex.postValue(index)
            sharedPreferences.edit().apply {
                putInt(CURRENT_QUEUE_ITEM_INDEX, index)
                apply()
            }
            withContext(Dispatchers.Main) {
                Log.i("DEBUG",
                    "The current controller index is ${controller.currentMediaItemIndex}")
            }
        }
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

        if (controller.isPlaying) controller.pause()

        var playQueue = songs.mapIndexed { i, s -> s.getMediaItem(i) }.toList()
        if (shuffle) {
            playQueue = playQueue.shuffled()
        }

        sharedPreferences.edit {
            putBoolean(SHUFFLE_MODE, shuffle)
        }

        controller.setMediaItem(playQueue[0])
        if (!controller.playWhenReady) controller.prepare()
        controller.addMediaItems(playQueue.subList(1, playQueue.size))

        if (startIndex != 0 && !shuffle) {
            playQueueViewModel.pendingSkipToInstruction.postValue(startIndex)
        }
        playQueueViewModel.pendingPlayInstruction.postValue(true)
        if (!shuffle) {
            playQueueViewModel.pendingExpectedMetadata.postValue(
                playQueue[startIndex].mediaMetadata.title.toString())
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
        var lastUsedOrderId = playQueueViewModel.playQueue.value?.mapNotNull { i ->
            i.mediaMetadata.extras?.getInt(ORDER_ID)
        }?.maxOf { id -> id } ?: -1
        val mediaItems = songs.map { s -> s.getMediaItem(++lastUsedOrderId) }

        if (addSongsAfterCurrentQueueItem) {
            withContext(Dispatchers.Main) {
                controller.addMediaItems(controller.currentMediaItemIndex + 1, mediaItems)
            }
        } else {
            withContext(Dispatchers.Main) {
                controller.addMediaItems(mediaItems)
            }
        }

        launch(Dispatchers.Main) toast@ {
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
    fun removeQueueItemByIndex(index: Int) = controller.removeMediaItem(index)

    /**
     * Set the playback position for the currently playing song to a specific location.
     *
     * @param position An Integer representing the desired playback position.
     */
    fun seekTo(position: Int) {
        controller.seekTo(position.toLong())
        updatePlaybackDurationAndPosition()
    }

    /**
     * Skip to a specific item in the play queue based on its index in the play queue.
     *
     * @param targetIndex The index in the queue to skip to.
     */
    fun skipToQueueIndex(targetIndex: Int) {
        // FIXME - NEED TO IMPLEMENT THE REMEMBER PLAYBACK PROGRESS FUNCTIONALITY
        val position = if (targetIndex == 0) playQueueViewModel.pendingSeekToInstruction.value ?: 0L
        else 0L
        controller.seekTo(targetIndex, position)
        if (position != 0L) {
            Log.i("DEBUG", "Pending seek to $position processed.")
            playQueueViewModel.pendingSeekToInstruction.postValue(null)
        }
        playQueueViewModel.pendingSkipToInstruction.postValue(null)
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
    fun deleteSongById(songId: Long) {
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

        for (song in songs) {
            // All occurrences of the song need to be updated in the play queue
            var index: Int
            do {
                index = getLastIndexOfQueueItemByMediaId(song.songId.toString())
                if (index != -1) {
                    val orderId = playQueueViewModel.playQueue.value?.get(index)
                        ?.mediaMetadata?.extras?.getInt(ORDER_ID) ?: continue
                    val mediaItem = song.getMediaItem(orderId)
                    controller.replaceMediaItem(index, mediaItem)
                }
            } while (index != -1)
        }
    }

    /**
     * Open a given Dialog Fragment.
     *
     * @param dialog The dialog fragment to load.
     */
    fun openDialog(dialog: DialogFragment) = dialog.show(supportFragmentManager, "")

    /** Create a channel for displaying application notifications */
    private fun createChannelForMediaPlayerNotification() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID, "Notifications",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "All app notifications"
            setSound(null, null)
            setShowBadge(false)
        }
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

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
    @OptIn(UnstableApi::class)
    private fun restoreMediaSession() = lifecycleScope.launch {
        if (playQueueViewModel.playQueue.value != null) return@launch
fixme for shuffle mode
            // TODO COMMENT OUT AGAIN
        sharedPreferences.edit {
            // remove("play_queue")
            // remove("current_queue_item_id_new")
        }

        Log.i("DEBUG", "Restoring the media session")

        val repeatMode = sharedPreferences.getInt(REPEAT_MODE, REPEAT_MODE_OFF)
        controller.repeatMode = repeatMode

        // TODO - WE COULD EVENTUALLY INCORPORATE A FEATURE FOR RESTORING SHUFFLED PLAY QUEUES
        //  THIS WOULD BE DONE BY SAVING THE UNSHUFFLED TIMELINE
        //  SAVING THE SHUFFLE PREFERENCE
        //  SAVING THE SHUFFLED ORDER OF ITEMS
        //  SETTING THE CONTROLLER TO SHUFFLED (DOES THIS NEED TO BE DONE AFTER PLAY QUEUE RESTORE? - VERIFY)
        //  RESTORING THE PLAY QUEUE ON RESTART
        //  MANUALLY MOVING EACH ITEM TO ITS ORIGINAL SHUFFLED POSITION

        // TODO - RESUME - USE THE SONGWITHORDERID JSON

        val queueItemPairsJson = sharedPreferences.getString(PLAY_QUEUE_ITEMS, null) ?: return@launch

        val itemType = object : TypeToken<List<SongWithOrderId>>() {}.type

        // FIXME RESUME - NEED TO USE THE QUEUE ID TO RESTORE ALSO, WHEN SHUFFLE PREFERENCE SET
        val songs = Gson().fromJson<List<SongWithOrderId>>(queueItemPairsJson, itemType)
        if (songs.isEmpty()) return@launch
        val playQueue = mutableListOf<MediaItem>()
        for (s in songs) {
            val newItem = s.song?.getMediaItem(s.orderId ?: continue) ?: continue
            playQueue.add(newItem)
        }

        controller.setMediaItem(playQueue[0])
        if (!controller.playWhenReady) controller.prepare()
        controller.addMediaItems(playQueue.subList(1, playQueue.size))

        val currentQueueItemIndex = sharedPreferences.getInt(CURRENT_QUEUE_ITEM_INDEX, -1)
        if (currentQueueItemIndex != -1) {
            playQueueViewModel.pendingSkipToInstruction.postValue(currentQueueItemIndex)
            playQueueViewModel.pendingExpectedMetadata.postValue(
                playQueue[currentQueueItemIndex].mediaMetadata.title.toString())
        }
        val playbackPosition = sharedPreferences.getLong(PLAYBACK_POSITION, 0L)
        if (playbackPosition != 0L) {
            playQueueViewModel.pendingSeekToInstruction.postValue(playbackPosition)
        }
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
                    findSongIdInPlayQueueToRemove(song.songId)
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
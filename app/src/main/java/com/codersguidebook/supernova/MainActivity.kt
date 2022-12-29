package com.codersguidebook.supernova

import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.*
import android.media.AudioManager
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat.QueueItem
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.media.session.PlaybackStateCompat.*
import android.view.Menu
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.SearchView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.*
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import com.codersguidebook.supernova.databinding.ActivityMainBinding
import com.codersguidebook.supernova.entities.Song
import com.codersguidebook.supernova.params.MediaServiceConstants.Companion.MOVE_QUEUE_ITEM
import com.codersguidebook.supernova.params.MediaServiceConstants.Companion.NOTIFICATION_CHANNEL_ID
import com.codersguidebook.supernova.params.MediaServiceConstants.Companion.REMOVE_QUEUE_ITEM_BY_ID
import com.codersguidebook.supernova.params.MediaServiceConstants.Companion.SET_REPEAT_MODE
import com.codersguidebook.supernova.params.MediaServiceConstants.Companion.SET_SHUFFLE_MODE
import com.codersguidebook.supernova.params.MediaServiceConstants.Companion.UPDATE_QUEUE_ITEM
import com.codersguidebook.supernova.params.SharedPreferencesConstants.Companion.CURRENT_QUEUE_ITEM_ID
import com.codersguidebook.supernova.params.SharedPreferencesConstants.Companion.PLAYBACK_DURATION
import com.codersguidebook.supernova.params.SharedPreferencesConstants.Companion.PLAYBACK_POSITION
import com.codersguidebook.supernova.params.SharedPreferencesConstants.Companion.PLAY_QUEUE_ITEM_PAIRS
import com.codersguidebook.supernova.params.SharedPreferencesConstants.Companion.REPEAT_MODE
import com.codersguidebook.supernova.params.SharedPreferencesConstants.Companion.SHUFFLE_MODE
import com.codersguidebook.supernova.utils.*
import com.google.android.material.navigation.NavigationView
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private var currentPlaybackPosition = 0
    private var currentPlaybackDuration = 0
    private var currentQueueItemId = -1L
    private var playQueue = listOf<QueueItem>()
    private val playQueueViewModel: PlayQueueViewModel by viewModels()
    private val mediaDescriptionManager = MediaDescriptionCompatManager()
    private var mediaStoreContentObserver: MediaStoreContentObserver? = null
    private var musicDatabase: MusicDatabase? = null
    private lateinit var mediaBrowser: MediaBrowserCompat
    private lateinit var musicLibraryViewModel: MusicLibraryViewModel
    private lateinit var searchView: SearchView
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var storagePermissionHelper: StorageAccessPermissionHelper
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private val connectionCallbacks = object : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            super.onConnected()

            mediaBrowser.sessionToken.also { token ->
                val mediaControllerCompat = MediaControllerCompat(this@MainActivity, token)
                MediaControllerCompat.setMediaController(this@MainActivity, mediaControllerCompat)
            }

            val mediaController = MediaControllerCompat.getMediaController(this@MainActivity)
            mediaController.registerCallback(controllerCallback)

            restoreMediaSession()
        }
    }

    private var controllerCallback = object : MediaControllerCompat.Callback() {

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            super.onPlaybackStateChanged(state)
            refreshPlayQueue()
            if (state?.activeQueueItemId != currentQueueItemId) {
                currentQueueItemId = state?.activeQueueItemId ?: -1
                savePlayQueueId(currentQueueItemId)
            }

            playQueueViewModel.playbackState.value = state?.state ?: STATE_NONE
            when (state?.state) {
                STATE_PLAYING -> {
                    currentPlaybackPosition = state.position.toInt()
                    state.extras?.let {
                        currentPlaybackDuration = it.getInt("duration", 0)
                        playQueueViewModel.playbackDuration.value = currentPlaybackDuration
                    }
                    playQueueViewModel.playbackPosition.value = currentPlaybackPosition
                }
                STATE_PAUSED -> {
                    currentPlaybackPosition = state.position.toInt()
                    state.extras?.let {
                        currentPlaybackDuration = it.getInt("duration", 0)
                        playQueueViewModel.playbackDuration.value = currentPlaybackDuration
                    }
                    playQueueViewModel.playbackPosition.value = currentPlaybackPosition
                }
                STATE_STOPPED -> {
                    currentPlaybackDuration = 0
                    playQueueViewModel.playbackDuration.value = 0
                    currentPlaybackPosition = 0
                    playQueueViewModel.playbackPosition.value = 0
                    playQueueViewModel.currentlyPlayingSongMetadata.value = null
                }
                // Called when playback of a song has completed.
                // Need to increment the song_plays count for that Song object by 1.
                STATE_SKIPPING_TO_NEXT -> {
                    state.extras?.let {
                        val finishedSongId = it.getLong("finishedSongId", -1L)
                        if (finishedSongId == -1L) return@let
                        musicLibraryViewModel.increaseSongPlaysBySongId(finishedSongId)
                        musicLibraryViewModel.addSongByIdToRecentlyPlayedPlaylist(finishedSongId)
                    }
                }
                else -> return
            }
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            super.onMetadataChanged(metadata)

            if (metadata?.description?.mediaId !=
                playQueueViewModel.currentlyPlayingSongMetadata.value?.description?.mediaId) {
                playQueueViewModel.playbackPosition.value = 0
            }

            playQueueViewModel.currentlyPlayingSongMetadata.value = metadata
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
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

        mediaBrowser = MediaBrowserCompat(this, ComponentName(this, MediaPlaybackService::class.java),
            connectionCallbacks, intent.extras)
        mediaBrowser.connect()

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(setOf(R.id.nav_home, R.id.nav_queue, R.id.nav_library, R.id.nav_playlists,
            R.id.nav_playlist, R.id.nav_artists, R.id.nav_artist, R.id.nav_albums, R.id.nav_album, R.id.nav_songs), binding.drawerLayout)

        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.navView.setupWithNavController(navController)

        val mOnNavigationItemSelectedListener = NavigationView.OnNavigationItemSelectedListener { item ->
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
        binding.navView.setNavigationItemSelectedListener(mOnNavigationItemSelectedListener)
        binding.navView.itemIconTintList = null

        val handler = Handler(Looper.getMainLooper())
        mediaStoreContentObserver = MediaStoreContentObserver(handler, this).also {
            this.contentResolver.registerContentObserver(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                true, it)
        }

        // Remove deleted songs from the play queue
        musicLibraryViewModel.deletedSongIds.observe(this) { songIds ->
            if (songIds.isEmpty()) return@observe
            for (songId in songIds) {
                val queueItemsToRemove = playQueue.filter { it.description.mediaId == songId.toString() }
                for (item in queueItemsToRemove) removeQueueItemById(item.queueId)
            }
            musicLibraryViewModel.deletedSongIds.value?.removeAll(songIds)
        }

        if (storagePermissionHelper.hasReadPermission()) {
            musicLibraryViewModel.refreshMusicLibrary()
        } else storagePermissionHelper.requestPermissions()
    }

    override fun onStart() {
        super.onStart()
        musicLibraryViewModel.refreshSongOfTheDay()
    }

    override fun onResume() {
        super.onResume()
        volumeControlStream = AudioManager.STREAM_MUSIC
    }

    /**
     * Notify the activity of a change to the media associated with a given content URI. This
     * method is used by MediaStoreContentObserver whenever a given URI is associated with
     * media insertion, deletion or update.
     *
     * @param uri The content URI associated with the change.
     */
    fun handleChangeToContentUri(uri: Uri) {
        val songIdString = uri.toString().removePrefix(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.toString() + "/")
        try {
            val songId = songIdString.toLong()
            musicLibraryViewModel.handleFileUpdateByMediaId(songId)
        } catch (_: NumberFormatException) { musicLibraryViewModel.refreshMusicLibrary() }
    }

    /** Fetch and save an up-to-date version of the play queue from the media controller. */
    private fun refreshPlayQueue() {
        val mediaControllerCompat = MediaControllerCompat.getMediaController(this@MainActivity)
        playQueue = mediaControllerCompat.queue
        playQueueViewModel.playQueue.postValue(playQueue)
        savePlayQueue()
    }

    /**
     * Notify the media browser service that a queue item has been moved.
     *
     * @param queueId The queue ID of the item to be moved.
     * @param newIndex The new index in the play queue that the item should occupy.
     */
    fun notifyQueueItemMoved(queueId: Long, newIndex: Int) {
        val bundle = Bundle().apply {
            putLong("queueItemId", queueId)
            putInt("newIndex", newIndex)
        }

        mediaController.sendCommand(MOVE_QUEUE_ITEM, bundle, null)
    }

    /** Respond to clicks on the play/pause button **/
    fun playPauseControl() {
        when (mediaController.playbackState?.state) {
            PlaybackState.STATE_PAUSED -> play()
            PlaybackState.STATE_PLAYING -> mediaController.transportControls.pause()
            else -> {
                // Load and play the user's music library if the play queue is empty
                if (playQueue.isEmpty()) {
                    playNewPlayQueue(musicLibraryViewModel.allSongs.value ?: return)
                }
                else {
                    // It's possible a queue has been built without ever pressing play.
                    // In which case, commence playback
                    mediaController.transportControls.prepare()
                    mediaController.transportControls.play()
                }
            }
        }
    }

    /**
     * Toggle the shuffle mode.
     *
     * @return An Integer representing the active shuffle mode preference.
     */
    fun toggleShuffleMode(): Int {
        val newShuffleMode = if (sharedPreferences.getInt(SHUFFLE_MODE, SHUFFLE_MODE_NONE) == SHUFFLE_MODE_NONE) {
            SHUFFLE_MODE_ALL
        } else SHUFFLE_MODE_NONE

        setShuffleMode(newShuffleMode)

        if (newShuffleMode == SHUFFLE_MODE_NONE) {
            Toast.makeText(this, getString(R.string.play_queue_unshuffled), Toast.LENGTH_SHORT).show()
        } else Toast.makeText(this, getString(R.string.play_queue_shuffled), Toast.LENGTH_SHORT).show()

        return newShuffleMode
    }

    /**
     * Save the active shuffle mode and notify the media browser service.
     * N.B. This functionality may be called independently of toggleShuffleMode() e.g. when an
     * album is played on shuffle mode directly from the Album view.
     *
     * @param shuffleMode An Integer representing the active shuffle mode preference.
     */
    private fun setShuffleMode(shuffleMode: Int) {
        sharedPreferences.edit().apply {
            putInt(SHUFFLE_MODE, shuffleMode)
            apply()
        }

        val bundle = Bundle().apply {
            putInt(SHUFFLE_MODE, shuffleMode)
        }

        mediaController.sendCommand(SET_SHUFFLE_MODE, bundle, null)
    }

    /**
     * Toggle the repeat mode.
     *
     * @return An Integer representing the active repeat mode preference.
     */
    fun toggleRepeatMode(): Int {
        val newRepeatMode = when (sharedPreferences.getInt(REPEAT_MODE, REPEAT_MODE_NONE)) {
            REPEAT_MODE_NONE -> REPEAT_MODE_ALL
            REPEAT_MODE_ALL -> REPEAT_MODE_ONE
            else -> REPEAT_MODE_NONE
        }

        sharedPreferences.edit().apply {
            putInt(REPEAT_MODE, newRepeatMode)
            apply()
        }

        val bundle = Bundle().apply {
            putInt(REPEAT_MODE, newRepeatMode)
        }
        mediaController.sendCommand(SET_REPEAT_MODE, bundle, null)

        when (newRepeatMode) {
            REPEAT_MODE_NONE -> Toast.makeText(this, getString(R.string.repeat_mode_none), Toast.LENGTH_SHORT).show()
            REPEAT_MODE_ALL -> Toast.makeText(this, getString(R.string.repeat_mode_all), Toast.LENGTH_SHORT).show()
            REPEAT_MODE_ONE -> Toast.makeText(this, getString(R.string.repeat_mode_one), Toast.LENGTH_SHORT).show()
        }

        return newRepeatMode
    }

    /** Commence playback of the currently loaded song. */
    private fun play() = mediaController.transportControls.play()

    /** Skip back to the previous track in the play queue (or restart the current song if less that five seconds in). */
    fun skipBack() = mediaController.transportControls.skipToPrevious()

    /** Skip forward to the next song in the play queue. */
    fun skipForward() = mediaController.transportControls.skipToNext()

    /** Rewind the playback of the current song. */
    fun fastRewind() = mediaController.transportControls.rewind()

    /** Fast forward the playback of the current song. */
    fun fastForward() = mediaController.transportControls.fastForward()

    /**
     * Convert the list of MediaDescriptionCompat objects for each item in the play queue to JSON
     * and save it in the shared preferences file.
     */
    private fun savePlayQueue() = lifecycleScope.launch(Dispatchers.IO) {
        try {
            // Pair mapping is <Long, Long> -> <QueueId, songId>
            val queueItemPairs = mutableListOf<Pair<Long, Long>>()
            for (item in playQueue) {
                item.description.mediaId?.let {
                    queueItemPairs.add(Pair(item.queueId, it.toLong()))
                }
            }
            val playQueueJson = GsonBuilder().setPrettyPrinting().create().toJson(queueItemPairs)
            sharedPreferences.edit().apply {
                putString(PLAY_QUEUE_ITEM_PAIRS, playQueueJson)
                apply()
            }
        } catch (_: ConcurrentModificationException) {}
    }

    /** Save the queueId of the currently playing queue item to the shared preferences file. */
    private fun savePlayQueueId(queueId: Long) = lifecycleScope.launch(Dispatchers.IO) {
        currentQueueItemId = queueId
        playQueueViewModel.currentQueueItemId.postValue(queueId)
        sharedPreferences.edit().apply {
            putLong(CURRENT_QUEUE_ITEM_ID, currentQueueItemId)
            apply()
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
    fun playNewPlayQueue(songs: List<Song>, startIndex: Int = 0, shuffle: Boolean = false)
            = lifecycleScope.launch(Dispatchers.Default) {
        if (songs.isEmpty() || startIndex >= songs.size) {
            Toast.makeText(this@MainActivity,
                getString(R.string.error_generic_playback), Toast.LENGTH_LONG).show()
            return@launch
        }
        mediaController.transportControls.stop()

        val startSongIndex = if (shuffle) (songs.indices).random()
        else startIndex

        val startSongDesc = mediaDescriptionManager.buildDescription(songs[startSongIndex], startSongIndex.toLong())

        val mediaControllerCompat = MediaControllerCompat.getMediaController(this@MainActivity)
        mediaControllerCompat.addQueueItem(startSongDesc)
        mediaControllerCompat.transportControls.skipToQueueItem(startSongIndex.toLong())
        mediaControllerCompat.transportControls.play()

        for ((index, song) in songs.withIndex()) {
            if (index == startSongIndex) continue
            val songDesc = mediaDescriptionManager.buildDescription(song, index.toLong())
            mediaControllerCompat.addQueueItem(songDesc, index)
        }

        when {
            shuffle -> setShuffleMode(SHUFFLE_MODE_ALL)
            mediaControllerCompat.shuffleMode == SHUFFLE_MODE_ALL -> setShuffleMode(SHUFFLE_MODE_NONE)
        }
    }

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
        val mediaControllerCompat = MediaControllerCompat.getMediaController(this@MainActivity)
        if (addSongsAfterCurrentQueueItem) {
            val index = playQueue.indexOfFirst { it.queueId == currentQueueItemId } + 1

            for (song in songs.asReversed()) {
                val songDesc = mediaDescriptionManager.buildDescription(song)
                mediaControllerCompat.addQueueItem(songDesc, index)
            }
        } else {
            for (song in songs) {
                val songDesc = mediaDescriptionManager.buildDescription(song)
                mediaControllerCompat.addQueueItem(songDesc)
            }
        }
    }

    /**
     * Remove a given QueueItem from the play queue based on its ID.
     *
     * @param queueItemId The ID of the QueueItem to be removed.
     */
    fun removeQueueItemById(queueItemId: Long) {
        if (playQueue.isNotEmpty()) {
            val bundle = Bundle().apply {
                putLong("queueItemId", queueItemId)
            }

            mediaController.sendCommand(REMOVE_QUEUE_ITEM_BY_ID, bundle, null)
        }
    }

    /**
     * Set the playback position for the currently playing song to a specific location.
     *
     * @param position An Integer representing the desired playback position.
     */
    fun seekTo(position: Int) = mediaController.transportControls.seekTo(position.toLong())

    /**
     * Skip to a specific item in the play queue based on its ID.
     *
     * @param queueItemId The ID of the target QueueItem object.
     */
    fun skipToQueueItem(queueItemId: Long) {
        mediaController.transportControls.skipToQueueItem(queueItemId)
        mediaController.transportControls.play()
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
            musicLibraryViewModel.getSongById(songId)
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
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_BARS_BY_SWIPE
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
     * Convenience method to open the 'Add to playlist' dialog when only the ID of
     * the given song is available. For example, QueueItem objects may feature incomplete
     * song metadata.
     *
     * @param songId The ID of the song.
     */
    fun openAddToPlaylistDialogForSongById(songId: Long) = lifecycleScope.launch(Dispatchers.Main) {
        val song = withContext(Dispatchers.IO) {
            musicLibraryViewModel.getSongById(songId)
        } ?: return@launch
        openAddToPlaylistDialog(listOf(song))
    }

    /**
     * Opens a dialog window allowing the user to add a list of songs to new and existing
     * playlists.
     *
     * @param songs The list of Song objects to be added to a playlist.
     */
    fun openAddToPlaylistDialog(songs: List<Song>) {
        val songIds = songs.map { it.songId }

        val positiveButtonClick = { _: DialogInterface, _: Int ->
            openDialog(CreatePlaylist(songIds))
        }

        // Retrieving all the user-created playlists
        val userPlaylists = musicLibraryViewModel.allPlaylists.value?.filterNot {
            it.isDefault
        } ?: listOf()

        // If the user has not created any playlists then skip straight to the create new playlist dialog
        if (userPlaylists.isEmpty()) {
            openDialog(CreatePlaylist(songIds))
            return
        }

        val userPlaylistNames = userPlaylists.map { it.name }.toTypedArray()

        AlertDialog.Builder(this).apply {
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
            val affectedQueueItems = playQueue.filter { it.description.mediaId == song.songId.toString() }
            if (affectedQueueItems.isEmpty()) continue

            val mediaDescriptionBundle = mediaDescriptionManager.getDescriptionAsBundle(song)
            for (queueItem in affectedQueueItems) {
                mediaDescriptionBundle.putLong("queueItemId", queueItem.queueId)
                mediaController.sendCommand(UPDATE_QUEUE_ITEM, mediaDescriptionBundle, null)
            }
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
        if (!storagePermissionHelper.hasReadPermission()) {
            Toast.makeText(this, getString(R.string.storage_permission_needed),
                Toast.LENGTH_LONG).show()
            if (!storagePermissionHelper.shouldShowRequestPermissionRationale()) {
                // Permission denied with checking "Do not ask again".
                storagePermissionHelper.launchPermissionSettings()
            }
            finish()
        } else musicLibraryViewModel.refreshMusicLibrary()
    }

    /** Restore the play queue and playback state from the last save. */
    private fun restoreMediaSession() = lifecycleScope.launch {
        val repeatMode = sharedPreferences.getInt(REPEAT_MODE, REPEAT_MODE_NONE)
        val repeatBundle = Bundle().apply {
            putInt(REPEAT_MODE, repeatMode)
        }
        mediaController.sendCommand(SET_REPEAT_MODE, repeatBundle, null)

        val shuffleMode = sharedPreferences.getInt(SHUFFLE_MODE, SHUFFLE_MODE_NONE)
        val shuffleBundle = Bundle().apply {
            putInt(SHUFFLE_MODE, shuffleMode)
        }
        mediaController.sendCommand(SET_SHUFFLE_MODE, shuffleBundle, null)

        val queueItemPairsJson = sharedPreferences.getString(PLAY_QUEUE_ITEM_PAIRS, null) ?: return@launch
        val currentQueueItemId = sharedPreferences.getLong(CURRENT_QUEUE_ITEM_ID, -1L)

        val itemType = object : TypeToken<List<Pair<Long, Long>>>() {}.type
        // Pair mapping is <Long, Long> -> <QueueId, songId>
        val queueItemPairs = Gson().fromJson<List<Pair<Long, Long>>>(queueItemPairsJson, itemType)

        val mediaControllerCompat = MediaControllerCompat.getMediaController(this@MainActivity)
        for (pair in queueItemPairs) {
            musicLibraryViewModel.getSongById(pair.second)?.let { song ->
                val queueId = pair.first
                val songDesc = mediaDescriptionManager.buildDescription(song, queueId)
                mediaControllerCompat.addQueueItem(songDesc)
            }
        }

        mediaControllerCompat.transportControls.skipToQueueItem(currentQueueItemId)

        val playbackPosition = sharedPreferences.getInt(PLAYBACK_POSITION, 0)
        if (playbackPosition != 0) seekTo(playbackPosition)
    }

    override fun onDestroy() {
        super.onDestroy()

        mediaStoreContentObserver?.let {
            this.contentResolver.unregisterContentObserver(it)
        }

        MediaControllerCompat.getMediaController(this)?.apply {
            transportControls.stop()
            unregisterCallback(controllerCallback)
        }
        mediaBrowser.disconnect()

        sharedPreferences.edit().apply {
            putInt(PLAYBACK_POSITION, currentPlaybackPosition)
            putInt(PLAYBACK_DURATION, currentPlaybackDuration)
            apply()
        }
    }
}
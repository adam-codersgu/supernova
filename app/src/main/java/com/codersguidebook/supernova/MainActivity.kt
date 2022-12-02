package com.codersguidebook.supernova

import android.Manifest
import android.app.Activity
import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.*
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.session.PlaybackState
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.provider.Settings
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat.QueueItem
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.media.session.PlaybackStateCompat.*
import android.util.Size
import android.view.Menu
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.SearchView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
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
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.signature.ObjectKey
import com.codersguidebook.supernova.databinding.ActivityMainBinding
import com.codersguidebook.supernova.entities.Playlist
import com.codersguidebook.supernova.entities.Song
import com.codersguidebook.supernova.params.MediaServiceConstants.Companion.MOVE_QUEUE_ITEM
import com.codersguidebook.supernova.params.MediaServiceConstants.Companion.REMOVE_QUEUE_ITEM_BY_ID
import com.codersguidebook.supernova.params.MediaServiceConstants.Companion.SET_REPEAT_MODE
import com.codersguidebook.supernova.params.MediaServiceConstants.Companion.SET_SHUFFLE_MODE
import com.codersguidebook.supernova.params.MediaServiceConstants.Companion.UPDATE_QUEUE_ITEM
import com.codersguidebook.supernova.params.ResultReceiverConstants.Companion.SUCCESS
import com.codersguidebook.supernova.params.SharedPreferencesConstants.Companion.CURRENT_QUEUE_ITEM_ID
import com.codersguidebook.supernova.params.SharedPreferencesConstants.Companion.PLAYBACK_DURATION
import com.codersguidebook.supernova.params.SharedPreferencesConstants.Companion.PLAYBACK_POSITION
import com.codersguidebook.supernova.params.SharedPreferencesConstants.Companion.PLAY_QUEUE_ITEM_PAIRS
import com.codersguidebook.supernova.params.SharedPreferencesConstants.Companion.REPEAT_MODE
import com.codersguidebook.supernova.params.SharedPreferencesConstants.Companion.SHUFFLE_MODE
import com.codersguidebook.supernova.params.SharedPreferencesConstants.Companion.SONG_OF_THE_DAY_LAST_UPDATED
import com.codersguidebook.supernova.utils.MediaDescriptionCompatManager
import com.google.android.material.navigation.NavigationView
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private val channelID = "supernova"
    private var currentPlaybackPosition = 0
    private var currentPlaybackDuration = 0
    private var currentQueueItemId = -1L
    private var playQueue = listOf<QueueItem>()
    private val playQueueViewModel: PlayQueueViewModel by viewModels()
    private var allPlaylists = listOf<Playlist>()
    private val mediaDescriptionManager = MediaDescriptionCompatManager()
    private var musicDatabase: MusicDatabase? = null
    var completeLibrary = listOf<Song>()
    private lateinit var mediaBrowser: MediaBrowserCompat
    private lateinit var musicLibraryViewModel: MusicLibraryViewModel
    private lateinit var searchView: SearchView
    private lateinit var sharedPreferences: SharedPreferences
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
            when (state?.state) {
                STATE_PLAYING -> {
                    currentPlaybackPosition = state.position.toInt()
                    state.extras?.let {
                        currentPlaybackDuration = it.getInt("duration", 0)
                        playQueueViewModel.playbackDuration.value = currentPlaybackDuration
                    }
                    playQueueViewModel.playbackPosition.value = currentPlaybackPosition
                    playQueueViewModel.isPlaying.value = true
                }
                STATE_PAUSED -> {
                    currentPlaybackPosition = state.position.toInt()
                    state.extras?.let {
                        currentPlaybackDuration = it.getInt("duration", 0)
                        playQueueViewModel.playbackDuration.value = currentPlaybackDuration
                    }
                    playQueueViewModel.playbackPosition.value = currentPlaybackPosition
                    playQueueViewModel.isPlaying.value = false
                }
                STATE_STOPPED -> {
                    playQueueViewModel.isPlaying.value = false
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
                        addSongByIdToRecentlyPlayedPlaylist(finishedSongId)
                    }
                }
                else -> return
            }
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            super.onMetadataChanged(metadata)

            playQueueViewModel.currentlyPlayingSongMetadata.value = metadata
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

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

        // Prevent icon tints from being overwritten
        binding.navView.itemIconTintList = null

        musicLibraryViewModel.allSongs.observe(this) {
            completeLibrary = it.toMutableList()
        }

        musicLibraryViewModel.allPlaylists.observe(this) {
            this.allPlaylists = it
        }

        musicLibraryViewModel.mostPlayedSongsById.observe(this) {
            val playlist = findPlaylist(getString(R.string.most_played))
            if (playlist != null) {
                val mostPlayedSongs = convertSongIdListToJson(it)
                if (mostPlayedSongs != playlist.songs){
                    playlist.songs = mostPlayedSongs
                    musicLibraryViewModel.updatePlaylists(listOf(playlist))
                }
            }
        }
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
     * @param queueId - The queue ID of the item to be moved.
     * @param newIndex - The new index in the play queue that the item should occupy.
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
                if (playQueue.isEmpty()) playNewPlayQueue(completeLibrary)
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
     * @param shuffleMode - An Integer representing the active shuffle mode preference.
     */
    private fun setShuffleMode(shuffleMode: Int) {
        sharedPreferences.edit().apply {
            putInt(SHUFFLE_MODE, shuffleMode)
            apply()
        }

        val bundle = Bundle().apply {
            putInt(SHUFFLE_MODE, shuffleMode)
        }

        val resultReceiver = object : ResultReceiver(Handler(Looper.getMainLooper())) {
            override fun onReceiveResult(resultCode: Int, resultData: Bundle) {
                if (resultCode == SUCCESS) {
                    playQueueViewModel.refreshPlayQueue.postValue(true)
                }
            }
        }

        mediaController.sendCommand(SET_SHUFFLE_MODE, bundle, resultReceiver)
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
     * @param songs - A list containing Song objects that should be added to the play queue.
     * @param startIndex - The index of the play queue element at which playback should begin.
     * Default = 0 (the beginning of the play queue).
     * N.B. If shuffle is true then the startIndex is ignored.
     * @param shuffle - Indicates whether the play queue should be shuffled.
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
     * @param songs - A list containing Song objects that should be added to the play queue.
     * @param addSongsAfterCurrentQueueItem - A Boolean indicating whether the songs should be added to
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
     * @param queueItemId - The ID of the QueueItem to be removed.
     */
    fun removeQueueItemById(queueItemId: Long) {
        if (playQueue.isNotEmpty()) {
            val bundle = Bundle().apply {
                putLong("queueItemId", queueItemId)
            }

            val resultReceiver = object : ResultReceiver(Handler(Looper.getMainLooper())) {
                override fun onReceiveResult(resultCode: Int, resultData: Bundle) {
                    if (resultCode == SUCCESS) {
                        playQueueViewModel.refreshPlayQueue.postValue(true)
                    }
                }
            }

            mediaController.sendCommand(REMOVE_QUEUE_ITEM_BY_ID, bundle, resultReceiver)
        }
    }

    /**
     * Set the playback position for the currently playing song to a specific location.
     *
     * @param position - An Integer representing the desired playback position.
     */
    fun seekTo(position: Int) = mediaController.transportControls.seekTo(position.toLong())

    /**
     * Skip to a specific item in the play queue based on its ID.
     *
     * @param queueItemId - The ID of the target QueueItem object.
     */
    fun skipToQueueItem(queueItemId: Long) {
        mediaController.transportControls.skipToQueueItem(queueItemId)
        mediaController.transportControls.play()
    }

    /**
     * Hide/reveal the status bars. If the status bars are hidden, then they can be transiently
     * revealed using a swipe motion.
     *
     * @param hide - A Boolean indicating whether the status bars should be hidden (true) or
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
            binding.toolbar.visibility = View.GONE
        } else {
            supportActionBar?.setDisplayShowTitleEnabled(true)
            windowInsetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_BARS_BY_SWIPE
            windowInsetsController.show(WindowInsetsCompat.Type.statusBars())

            binding.toolbar.visibility = View.VISIBLE
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)

        val searchItem = menu.findItem(R.id.search)
        searchView = searchItem!!.actionView as SearchView
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
     * @param songId
     */
    fun openAddToPlaylistDialogForSongById(songId: Long) {
        val song = getSongById(songId) ?: return
        openAddToPlaylistDialog(listOf(song))
    }

    /**
     * Retrieve the Song object associated with a given ID.
     *
     * @param songId - The ID of the song.
     * @return The associated Song object, or null.
     */
    fun getSongById(songId: Long) : Song? = completeLibrary.find { it.songId == songId }

    /**
     * Opens a dialog window allowing the user to add a list of songs to new and existing
     * playlists.
     *
     * @param songs - The list of Song objects to be added to a playlist.
     */
    fun openAddToPlaylistDialog(songs: List<Song>) {
        val songIds = songs.map { it.songId }

        val positiveButtonClick = { _: DialogInterface, _: Int ->
            openDialog(CreatePlaylist(songIds))
        }

        // Retrieving all the user-created playlists
        val userPlaylists = allPlaylists.filterNot { it.isDefault }

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
                val playlistSongIds = extractPlaylistSongIds(playlist.songs)
                playlistSongIds.addAll(songIds)
                savePlaylistWithSongIds(playlist, playlistSongIds)

                if (songs.size == 1) Toast.makeText(applicationContext, getString(R.string.song_added_to_playlist,
                    songs[0].title, playlist.name), Toast.LENGTH_SHORT
                ).show()
                else Toast.makeText(applicationContext,
                    getString(R.string.songs_added_to_playlist, playlist.name), Toast.LENGTH_SHORT
                ).show()
            }
            setNegativeButton(R.string.cancel) { _, _ -> return@setNegativeButton }
            setPositiveButton(R.string.create_playlist, positiveButtonClick)
            show()
        }
    }

    fun refreshSongOfTheDay(forceUpdate: Boolean) {
        if (completeLibrary.isEmpty()) return
        val playlist = findPlaylist(getString(R.string.song_day)) ?: Playlist(
            0,
            getString(R.string.song_day),
            null,
            false
        )
        val songIDList = extractPlaylistSongIds(playlist.songs)
        // updating the song of the day, if one has not already been set for today's date
        val date = SimpleDateFormat.getDateInstance().format(Date())
        val lastUpdate = sharedPreferences.getString(SONG_OF_THE_DAY_LAST_UPDATED, null)
        when {
            date != lastUpdate -> {
                val song = completeLibrary.random()
                songIDList.add(0, song.songId)
                if (songIDList.size > 30) songIDList.removeAt(songIDList.size - 1)
                savePlaylistWithSongIds(playlist, songIDList)
                sharedPreferences.edit().apply {
                    putString(SONG_OF_THE_DAY_LAST_UPDATED, date)
                    apply()
                }
            }
            forceUpdate -> {
                // could use removeLast but that command is still experimental at the moment
                if (songIDList.isNotEmpty()) songIDList.removeAt(0)
                val song = completeLibrary.random()
                songIDList.add(0, song.songId)
                savePlaylistWithSongIds(playlist, songIDList)
            }
        }
    }

    /**
     * Find the Playlist object associated with a given name.
     *
     * @param playlistName - The playlist's name.
     * @return The associated Playlist object or null if no match found.
     */
    private fun findPlaylist(playlistName: String): Playlist? {
        return allPlaylists.find { it.name == playlistName }
    }

    /**
     * Update the list of songs associated with a given playlist.
     *
     * @param playlist - The target playlist.
     * @param songIds - The list of song IDs to be saved with the playlist.
     */
    fun savePlaylistWithSongIds(playlist: Playlist, songIds: List<Long>) {
        if (songIds.isNotEmpty()) {
            playlist.songs = convertSongIdListToJson(songIds)
        } else playlist.songs = null
        musicLibraryViewModel.updatePlaylists(listOf(playlist))
    }

    /**
     * Delete a given playlist from the app database and any associated artwork.
     *
     * @param playlist - The Playlist object to be deleted.
     */
    fun deletePlaylist(playlist: Playlist) {
        musicLibraryViewModel.deletePlaylist(playlist)
        val cw = ContextWrapper(application)
        val directory = cw.getDir("playlistArt", Context.MODE_PRIVATE)
        val path = File(directory, playlist.playlistId.toString() + ".jpg")
        if (path.exists()) path.delete()
    }

    /**
     * Save updates to song metadata to the database. Also update the play queue (if necessary)
     *
     * @param songs - The list of Song objects containing updated metadata.
     * @return
     */
    fun updateSongInfo(songs: List<Song>) = lifecycleScope.launch(Dispatchers.Default) {
        musicLibraryViewModel.updateMusicInfo(songs)

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
     * Toggle the isFavourite field for a given Song object. Also update the favourites
     * playlist accordingly.
     *
     * @param song - The target Song object.
     * @return A Boolean indicating the new value of the isFavourite field.
     */
    fun toggleSongFavouriteStatus(song: Song?): Boolean {
        if (song == null) return false

        val favouritesPlaylist = findPlaylist(getString(R.string.favourites))
        if (favouritesPlaylist != null) {
            val songIdList = extractPlaylistSongIds(favouritesPlaylist.songs)
            val matchingSong = songIdList.firstOrNull { it == song.songId }

            if (matchingSong == null) {
                song.isFavourite = true
                songIdList.add(song.songId)
            } else {
                song.isFavourite = false
                songIdList.remove(matchingSong)
            }

            if (songIdList.isNotEmpty()) {
                val newSongListJSON = convertSongIdListToJson(songIdList)
                favouritesPlaylist.songs = newSongListJSON
            } else favouritesPlaylist.songs = null
            musicLibraryViewModel.updatePlaylists(listOf(favouritesPlaylist))
            updateSongInfo(listOf(song))
            if (song.isFavourite) {
                Toast.makeText(this@MainActivity, getString(R.string.added_to_favourites),
                    Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@MainActivity, getString(R.string.removed_from_favourites),
                    Toast.LENGTH_SHORT).show()
            }
        }
        return song.isFavourite
    }

    /**
     * Add a song to the Recently Played playlist.
     *
     * @param songId - The media ID of the song.
     * @return
     */
    private fun addSongByIdToRecentlyPlayedPlaylist(songId: Long) = lifecycleScope.launch(Dispatchers.Main) {
        findPlaylist(getString(R.string.recently_played))?.apply {
            val songIDList = extractPlaylistSongIds(this.songs)
            if (songIDList.isNotEmpty()) {
                val index = songIDList.indexOfFirst {
                    it == songId
                }
                if (index != -1) songIDList.removeAt(index)
                songIDList.add(0, songId)
                if (songIDList.size > 30) songIDList.removeAt(songIDList.size - 1)
            } else songIDList.add(songId)
            this.songs = convertSongIdListToJson(songIDList)
            musicLibraryViewModel.updatePlaylists(listOf(this))
        }
    }

    fun findFirstSongArtwork(songID: Long): String? {
        return completeLibrary.find {
            it.songId == songID
        }?.albumId
    }

    fun convertSongIdListToJson(songIdList: List<Long>): String {
        val gPretty = GsonBuilder().setPrettyPrinting().create()
        return gPretty.toJson(songIdList)
    }

    /**
     * Convert a JSON String representing a playlist's songs to a list of song IDs.
     *
     * @param json - The JSON String representation of a playlist's songs.
     * @return - A list of song IDs.
     */
    fun extractPlaylistSongIds(json: String?): MutableList<Long> {
        return if (json != null) {
            val listType = object : TypeToken<List<Long>>() {}.type
            Gson().fromJson(json, listType)
        } else mutableListOf()
    }

    fun extractPlaylistSongs(json: String?): MutableList<Song> {
        val songIDList = extractPlaylistSongIds(json)
        return if (songIDList.isEmpty()) mutableListOf()
        else {
            val playlistSongs = mutableListOf<Song>()
            for (i in songIDList) {
                val song = completeLibrary.find {
                    it.songId == i
                }
                if (song != null) playlistSongs.add(song)
            }
            playlistSongs
        }
    }

    fun saveNewPlaylist(playlist: Playlist): Boolean{
        val index = allPlaylists.indexOfFirst { item: Playlist ->
            item.name == playlist.name
        }
        // checking if playlist name is unique
        return if (index == -1) {
            musicLibraryViewModel.insertPlaylist(playlist)
            true
        } else false
    }

    fun openDialog(dialog: DialogFragment) = dialog.show(supportFragmentManager, "")

    fun openAlbumDialog(albumID: String) {
        val albumSongs = completeLibrary.filter {
            it.albumId == albumID
        }.sortedBy { it.track }
        (AlbumOptions(albumSongs).show(supportFragmentManager, ""))
    }

    private fun createChannelForMediaPlayerNotification() {
        val channel = NotificationChannel(
            channelID, "Notifications",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "All app notifications"
            setSound(null, null)
            setShowBadge(false)
        }
        val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    fun insertArtwork(albumID: String?, view: ImageView) {
        var file: File? = null
        if (albumID != null) {
            val cw = ContextWrapper(this)
            val directory = cw.getDir("albumArt", Context.MODE_PRIVATE)
            file = File(directory, "$albumID.jpg")
        }
        runGlide(file, view)
    }

    fun insertPlaylistArtwork(playlist: Playlist, view: ImageView) : Boolean {
        val cw = ContextWrapper(this)
        val directory = cw.getDir("playlistArt", Context.MODE_PRIVATE)
        val file = File(directory, playlist.playlistId.toString() + ".jpg")
        return if (file.exists()) {
            runGlide(file, view)
            true
        } else false
    }

    private fun runGlide(file: File?, view: ImageView) {
        Glide.with(this)
            .load(file ?: R.drawable.no_album_artwork)
            .transition(DrawableTransitionOptions.withCrossFade())
            .centerCrop()
            .signature(ObjectKey(file?.path + file?.lastModified()))
            .override(600, 600)
            .error(R.drawable.no_album_artwork)
            .into(view)
    }

    fun changeArtwork(dirName: String, newArtwork: Bitmap, filename: String) {
        val cw = ContextWrapper(application)
        val directory = cw.getDir(dirName, Context.MODE_PRIVATE)
        val path = File(directory, "$filename.jpg")
        saveImage(newArtwork, path)
    }

    /**
     * Initiate music library maintenance tasks such as adding new songs, removing deleted
     * songs, and refreshing the song of the day.
     *
     * @param checkForDeletedSongs - A Boolean value indicating whether the method should
     * check whether songs have been deleted from the user's device.
     * @return
     */
    private fun libraryMaintenance(checkForDeletedSongs: Boolean) = lifecycleScope.launch(
        Dispatchers.Main
    ) {
        val libraryBuilt = libraryRefreshAsync().await()
        if (libraryBuilt && completeLibrary.isNotEmpty()) {
            if (checkForDeletedSongs) {
                val songsToDelete = checkLibrarySongsExistAsync().await()
                if (!songsToDelete.isNullOrEmpty()) deleteSongs(songsToDelete)
            }
            refreshSongOfTheDay(false)
        }
    }

    private fun libraryRefreshAsync(): Deferred<Boolean> = lifecycleScope.async(Dispatchers.IO) {
        var songs = mutableListOf<Song>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.YEAR
        )
        val libraryCursor = musicQueryAsync(projection).await()
        libraryCursor?.use { cursor ->
            // Cache column indices.
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val trackColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIDColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val yearColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)

            while (cursor.moveToNext()) {
                // Get values of columns for a given audio file
                val id = cursor.getLong(idColumn)

                // check song has not been added to library. will return -1 if not in library
                val indexOfSong = completeLibrary.indexOfFirst { song: Song ->
                    song.songId == id
                }
                if (indexOfSong == -1) {
                    var trackString = cursor.getString(trackColumn) ?: "1001"

                    // We need the Track value in the format 1xxx where the first digit is the disc number
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
                    } catch (e: NumberFormatException) {
                        // if track string format is incorrect (e.g. you can get stuff like "12/23") then simply set track to 1001
                        1001
                    }

                    val title = cursor.getString(titleColumn) ?: "Unknown song"
                    val artist = cursor.getString(artistColumn) ?: "Unknown artist"
                    val album = cursor.getString(albumColumn) ?: "Unknown album"
                    val year = cursor.getString(yearColumn) ?: "2000"
                    val albumID = cursor.getString(albumIDColumn) ?: "unknown_album_ID"
                    val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)

                    val cw = ContextWrapper(application)
                    // path to /data/data/this_app/app_data/albumArt
                    val directory = cw.getDir("albumArt", Context.MODE_PRIVATE)
                    val path = File(directory, "$albumID.jpg")
                    // If artwork is not saved then try and find some
                    if (!path.exists()) {
                        val albumArt = try {
                            application.contentResolver.loadThumbnail(uri,
                                Size(640, 640), null)
                        } catch (_: FileNotFoundException) { null }
                        albumArt?.let { saveImage(it, path) }
                    }

                    // Stores column values and the contentUri in a local object
                    // that represents the media file.
                    val song = Song(
                        id,
                        track,
                        title,
                        artist,
                        album,
                        albumID,
                        year,
                        false,
                        0
                    )
                    songs.add(song)
                    if (songs.size > 9) {
                        musicLibraryViewModel.insertSongs(songs)
                        songs = mutableListOf()
                    }
                }
            }
        }
        if (songs.isNotEmpty()) musicLibraryViewModel.insertSongs(songs)
        return@async true
    }

    private fun musicQueryAsync(projection: Array<String>): Deferred<Cursor?> = lifecycleScope.async(Dispatchers.IO) {
        val selection = MediaStore.Audio.Media.IS_MUSIC
        val sortOrder = MediaStore.Audio.Media.TITLE + " ASC"
        return@async application.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            sortOrder
        )
    }

    /**
     * Saves a bitmap representation of an image to a specified file path location.
     *
     * @param bitmap - The Bitmap instance to be saved.
     * @param path - The location at which the image file should be saved.
     */
    private fun saveImage(bitmap: Bitmap, path: File) {
        try {
            val fileOutputStream = FileOutputStream(path)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
            fileOutputStream.close()
        } catch (_: Exception) { }
    }

    /**
     * Check if the album artwork associated with songs that have been deleted from the music library
     * is still required for other songs. If the artwork is no longer used elsewhere, then the image
     * file can be deleted.
     *
     * @param songs - A list of Song objects that have been deleted from the music library.
     */
    private suspend fun deleteRedundantArtworkForDeletedSongs(songs: List<Song>) {
        val contextWrapper = ContextWrapper(application)
        val directory = contextWrapper.getDir("albumArt", Context.MODE_PRIVATE)
        for (song in songs){
            val songsWithAlbumId = musicDatabase!!.musicDao().getSongWithAlbumId(song.albumId)
            if (songsWithAlbumId.isEmpty()){
                val path = File(directory, song.albumId + ".jpg")
                if (path.exists()) path.delete()
            }
        }
    }

    /**
     * Check if the audio file for each song in the user's music library still exists on the device.
     * Any songs that no longer exist should be deleted.
     *
     * @return - A deferred list of Song objects for which the corresponding audio file could not be found.
     * Such songs should be removed from the music library.
     */
    private fun checkLibrarySongsExistAsync(): Deferred<List<Song>?> = lifecycleScope.async(Dispatchers.IO) {
        val projection = arrayOf(MediaStore.Audio.Media._ID)
        val libraryCursor = musicQueryAsync(projection).await()
        libraryCursor?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val songsToBeDeleted = completeLibrary.toMutableList()

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val indexOfSong = songsToBeDeleted.indexOfFirst { song: Song ->
                    song.songId == id
                }
                if (indexOfSong != -1) songsToBeDeleted.removeAt(indexOfSong)
            }
            return@async songsToBeDeleted
        }
    }

    /**
     * Cleanup operations for when songs are to be deleted from the user's music library. The deleted songs must be
     * removed from the play queue and any playlists they feature in. The saved artwork for those songs can also be
     * deleted if no longer required. The Song objects should also be deleted from the application database.
     *
     * @param songs - The list of Song objects to be deleted.
     * @return
     */
    private suspend fun deleteSongs(songs: List<Song>) = lifecycleScope.launch(Dispatchers.Default) {
        for (song in songs) {
            if (allPlaylists.isNotEmpty()) {
                val updatedPlaylists = mutableListOf<Playlist>()
                for (playlist in allPlaylists) {
                    if (playlist.songs != null) {
                        val newSongIDList = extractPlaylistSongIds(playlist.songs)

                        var playlistModified = false
                        fun findIndex(): Int {
                            return newSongIDList.indexOfFirst {
                                it == song.songId
                            }
                        }

                        // Remove all instances of the song from the playlist
                        do {
                            val index = findIndex()
                            if (index != -1) {
                                newSongIDList.removeAt(index)
                                playlistModified = true
                            }
                        } while (index != -1)

                        if (playlistModified) {
                            playlist.songs = convertSongIdListToJson(newSongIDList)
                            updatedPlaylists.add(playlist)
                        }
                    }
                }
                if (updatedPlaylists.isNotEmpty()) musicLibraryViewModel.updatePlaylists(updatedPlaylists)
            }

            val queueItemsToRemove = playQueue.filter { it.description.mediaId == song.songId.toString() }
            for (item in queueItemsToRemove) removeQueueItemById(item.queueId)

            musicLibraryViewModel.deleteSong(song)
        }
        deleteRedundantArtworkForDeletedSongs(songs)
    }

    /**
     * Hides the soft input keyboard, which can sometimes obstruct views.
     *
     * @param activity - The activity that currently has focus
     */
    fun hideKeyboard(activity: Activity) {
        val inputManager = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        // Check if a view has focus
        val currentFocusedView = activity.currentFocus
        if (currentFocusedView != null) inputManager.hideSoftInputFromWindow(
            currentFocusedView.windowToken,
            InputMethodManager.HIDE_NOT_ALWAYS
        )
    }

    /** Helper to ask storage permission.  */
    object MusicPermissionHelper {
        private const val READ_STORAGE_PERMISSION_CODE = 100
        private const val READ_PERMISSION = Manifest.permission.READ_EXTERNAL_STORAGE

        /** Check to see we have the necessary permissions for this app.  */
        fun hasReadPermission(activity: Activity): Boolean {
            return ContextCompat.checkSelfPermission(activity, READ_PERMISSION) == PackageManager.PERMISSION_GRANTED
        }

        /** Check to see we have the necessary permissions for this app, and ask for them if we don't.  */
        fun requestPermissions(activity: Activity) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(READ_PERMISSION),
                READ_STORAGE_PERMISSION_CODE
            )
        }

        /** Check to see if we need to show the rationale for this permission.  */
        fun shouldShowRequestPermissionRationale(activity: Activity): Boolean {
            return ActivityCompat.shouldShowRequestPermissionRationale(activity, READ_PERMISSION)
        }

        /** Launch Application Setting to grant permission.  */
        fun launchPermissionSettings(activity: Activity) {
            val intent = Intent()
            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            intent.data = Uri.fromParts("package", activity.packageName, null)
            activity.startActivity(intent)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (!MusicPermissionHelper.hasReadPermission(this)) {
            Toast.makeText(this, getString(R.string.storage_permission_needed),
                Toast.LENGTH_LONG).show()
            if (!MusicPermissionHelper.shouldShowRequestPermissionRationale(this)) {
                // Permission denied with checking "Do not ask again".
                MusicPermissionHelper.launchPermissionSettings(this)
            }
            finish()
        } else libraryMaintenance(false)
    }

    override fun onResume() {
        super.onResume()
        volumeControlStream = AudioManager.STREAM_MUSIC

        if (MusicPermissionHelper.hasReadPermission(this)) libraryMaintenance(true)
        else MusicPermissionHelper.requestPermissions(this)
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

        val gson = Gson()
        val itemType = object : TypeToken<List<Pair<Long, Long>>>() {}.type
        // Pair mapping is <Long, Long> -> <QueueId, songId>
        val queueItemPairs = gson.fromJson<List<Pair<Long, Long>>>(queueItemPairsJson, itemType)

        val mediaControllerCompat = MediaControllerCompat.getMediaController(this@MainActivity)
        for (pair in queueItemPairs) {
            val song = completeLibrary.find { it.songId == pair.second }
            song?.let {
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
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
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
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
import androidx.core.view.ViewCompat
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
import com.codersguidebook.supernova.utils.MediaDescriptionCompatManager
import com.google.android.material.navigation.NavigationView
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private val channelID = "supernova"
    private var currentPlaybackPosition = 0
    private var currentPlaybackDuration = 0
    private var currentlyPlayingQueueItemId = -1L
    // FIXME: Can we make the below private
    var playQueue = listOf<QueueItem>()
    private val playQueueViewModel: PlayQueueViewModel by viewModels()
    private var allPlaylists = listOf<Playlist>()
    private var musicDatabase: MusicDatabase? = null
    var completeLibrary = listOf<Song>()
    // FIXME: Is the below still required?
    private var playbackState = STATE_STOPPED
    private lateinit var mediaBrowser: MediaBrowserCompat
    private lateinit var musicLibraryViewModel: MusicLibraryViewModel
    private lateinit var searchView: SearchView
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var mediaDescriptionCompatManager: MediaDescriptionCompatManager

    companion object {
        private const val PLAY_QUEUE_MEDIA_DESCRIPTION_LIST = "play_queue_media_description_list"
        private const val CURRENT_QUEUE_ITEM_INDEX = "current_queue_item_index"
        private const val PLAYBACK_POSITION = "playback_position"
        private const val PLAYBACK_DURATION = "playback_duration"
    }

    private val connectionCallbacks = object : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            super.onConnected()

            mediaBrowser.sessionToken.also { token ->
                val mediaControllerCompat = MediaControllerCompat(this@MainActivity, token)
                mediaControllerCompat.registerCallback(controllerCallback)
                MediaControllerCompat.setMediaController(this@MainActivity, mediaControllerCompat)
            }

            val mediaController = MediaControllerCompat.getMediaController(this@MainActivity)
            mediaController.registerCallback(controllerCallback)

            restorePlayQueue()
        }
    }

    private var controllerCallback = object : MediaControllerCompat.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            super.onPlaybackStateChanged(state)
            refreshPlayQueue()
            when (state?.state) {
                STATE_PLAYING -> {
                    playbackState = STATE_PLAYING
                    currentPlaybackPosition = state.position.toInt()
                    state.extras?.let {
                        currentPlaybackDuration = it.getInt("duration", 0)
                        playQueueViewModel.currentPlaybackDuration.value = currentPlaybackDuration
                    }
                    playQueueViewModel.currentPlaybackPosition.value = currentPlaybackPosition
                    playQueueViewModel.isPlaying.value = true
                }
                STATE_PAUSED -> {
                    playbackState = STATE_PAUSED
                    currentPlaybackPosition = state.position.toInt()
                    state.extras?.let {
                        currentPlaybackDuration = it.getInt("duration", 0)
                        playQueueViewModel.currentPlaybackDuration.value = currentPlaybackDuration
                    }
                    playQueueViewModel.currentPlaybackPosition.value = currentPlaybackPosition
                    playQueueViewModel.isPlaying.value = false
                }
                STATE_STOPPED -> {
                    playbackState = STATE_STOPPED
                    currentlyPlayingQueueItemId = -1L
                    playQueueViewModel.isPlaying.value = false
                    savePlayQueueId(0)
                    playQueueViewModel.currentlyPlayingSong.value = null
                    currentPlaybackDuration = 0
                    playQueueViewModel.currentPlaybackDuration.value = 0
                    currentPlaybackPosition = 0
                    playQueueViewModel.currentPlaybackPosition.value = 0
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
                STATE_SKIPPING_TO_QUEUE_ITEM -> {
                    state.extras?.let {
                        val currentQueueItemId = it.getLong("currentQueueItemId", -1L)
                        if (currentQueueItemId == -1L) return@let
                        savePlayQueueId(currentQueueItemId)
                    }
                }
                else -> return
            }
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
        mediaDescriptionCompatManager = MediaDescriptionCompatManager(this)

        // Set up a channel for the music player notification
        createChannel()

        val taskDescription = ActivityManager.TaskDescription("Supernova", R.drawable.no_album_artwork, getColor(R.color.nav_home))
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
                val mostPlayedSongs = convertSongIDListToJson(it)
                if (mostPlayedSongs != playlist.songs){
                    playlist.songs = mostPlayedSongs
                    musicLibraryViewModel.updatePlaylists(listOf(playlist))
                }
            }
        }
    }

    /**
     * Fetch an up-to-date version of the play queue from the media controller.
     *
     */
    private fun refreshPlayQueue() {
        val mediaControllerCompat = MediaControllerCompat.getMediaController(this@MainActivity)
        playQueue = mediaControllerCompat.queue
        playQueueViewModel.currentPlayQueue.value = playQueue
        savePlayQueue()
    }

    fun playPauseControl() {
        when (playbackState) {
            STATE_PAUSED -> play()
            STATE_PLAYING -> mediaController.transportControls.pause()
            else -> {
                // Load and play the user's music library if the play queue is empty
                if (playQueue.isEmpty()) playListOfSongs(completeLibrary, null, false)
                else {
                    // It's possible a queue has been built without ever pressing play. In which case, commence playback here
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
        val newShuffleMode = if (sharedPreferences.getInt("shuffleMode", SHUFFLE_MODE_NONE) == SHUFFLE_MODE_NONE) {
            SHUFFLE_MODE_ALL
        } else SHUFFLE_MODE_NONE

        setShuffleMode(newShuffleMode)

        if (newShuffleMode == SHUFFLE_MODE_NONE) {
            Toast.makeText(this, "Play queue unshuffled", Toast.LENGTH_SHORT).show()
        } else Toast.makeText(this, "Play queue shuffled", Toast.LENGTH_SHORT).show()

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
            putInt("shuffleMode", shuffleMode)
            apply()
        }

        val bundle = Bundle()
        bundle.putInt("shuffleMode", shuffleMode)

        mediaController.sendCommand("setShuffleMode", bundle, null)
    }

    /**
     * Toggle the repeat mode.
     *
     * @return An Integer representing the active repeat mode preference.
     */
    fun toggleRepeatMode(): Int {
        val newRepeatMode = when (sharedPreferences.getInt("repeatMode", REPEAT_MODE_NONE)) {
            REPEAT_MODE_NONE -> REPEAT_MODE_ALL
            REPEAT_MODE_ALL -> REPEAT_MODE_ONE
            else -> REPEAT_MODE_NONE
        }

        sharedPreferences.edit().apply {
            putInt("repeatMode", newRepeatMode)
            apply()
        }

        val bundle = Bundle()
        bundle.putInt("repeatMode", newRepeatMode)
        // TODO: Could eventually have a result callback (instead of null)
        // TODO: Also could we delegate command names to a static constant params class for consistency
        mediaController.sendCommand("setRepeatMode", bundle, null)

        // TODO: Need a ticket to go through and replace all hardcoded strings with string resources
        when (newRepeatMode) {
            REPEAT_MODE_NONE -> Toast.makeText(this, "Repeat mode off", Toast.LENGTH_SHORT).show()
            REPEAT_MODE_ALL -> Toast.makeText(this, "Repeat play queue", Toast.LENGTH_SHORT).show()
            REPEAT_MODE_ONE -> Toast.makeText(this, "Repeat current song", Toast.LENGTH_SHORT).show()
        }

        return newRepeatMode
    }

    /**
     * Commence playback.
     *
     */
    private fun play() = mediaController.transportControls.play()

    /**
     * Skip back to the previous track in the play queue (or restart the current song if less that five seconds in).
     *
     */
    fun skipBack() = mediaController.transportControls.skipToPrevious()

    /**
     * Skip forward to the next song in the play queue.
     *
     */
    fun skipForward() = mediaController.transportControls.skipToNext()

    /**
     * Rewind the playback of the current song.
     *
     */
    fun fastRewind() = mediaController.transportControls.rewind()

    /**
     * Fast forward the playback of the current song.
     *
     */
    fun fastForward() = mediaController.transportControls.fastForward()

    /**
     * Convert the list of MediaDescriptionCompat objects for each item in the play queue to JSON
     * and save it in the shared preferences file.
     *
     * @return
     */
    private fun savePlayQueue() = lifecycleScope.launch(Dispatchers.IO) {
        try {
            val listOfMediaDescriptions = mutableListOf<MediaDescriptionCompat>()
            for (item in playQueue) listOfMediaDescriptions.add(item.description)
            sharedPreferences.edit().apply {
                val playQueueJSON = GsonBuilder().setPrettyPrinting().create().toJson(listOfMediaDescriptions)
                putString(PLAY_QUEUE_MEDIA_DESCRIPTION_LIST, playQueueJSON)
                apply()
            }
        } catch (_: ConcurrentModificationException) {}
    }

    /**
     * Save the queueId of the currently playing queue item to the shared preferences file.
     *
     * @return
     */
    private fun savePlayQueueId(queueId: Long) = lifecycleScope.launch(Dispatchers.IO) {
        currentlyPlayingQueueItemId = queueId
        playQueueViewModel.currentQueueItemId.postValue(queueId)
        val currentQueueItemIndex = playQueue.indexOfFirst { it.queueId == queueId }
        sharedPreferences.edit().apply {
            putInt(CURRENT_QUEUE_ITEM_INDEX, currentQueueItemIndex)
            apply()
        }
    }

    /*
    FIXME: May have massive performance issues here. Need to think of an asynchronous way of building long queues.
        An alternative could be to use a coroutine to add the songs as fast as possible, and begin playback (if required) as soon as one song is added
        Perhaps a custom action will be necessary if the media browser service is not really giving suitable tools
        !!! Or perhaps just don't get an image for each item. Only load the image when required for notification and currently playing view?
     */
    /**
     * Load a list of Song objects into the media player service and commence playback.
     *
     * @param songs - The list of songs to be played.
     * @param startIndex - The index of the song where playback should start from.
     * @param shuffle - Should the play queue be shuffled prior to commencing playback?
     * @return
     */
    fun playListOfSongs(songs: List<Song>, startIndex: Int?, shuffle: Boolean) = lifecycleScope.launch(Dispatchers.Main) {
        if (songs.isNotEmpty()) {
            mediaController.transportControls.stop()

            val mediaControllerCompat = MediaControllerCompat.getMediaController(this@MainActivity)

            for (song in songs) {
                val mediaDescriptionCompat = mediaDescriptionCompatManager.buildDescription(song)
                mediaControllerCompat.addQueueItem(mediaDescriptionCompat)
            }

            if (shuffle) {
                val randomQueueItem = playQueue[Random.nextInt(0, playQueue.size)]
                mediaController.transportControls.skipToQueueItem(randomQueueItem.queueId)
                setShuffleMode(SHUFFLE_MODE_ALL)
            } else {
                setShuffleMode(SHUFFLE_MODE_NONE)
                // FIXME: If the second half of the equation is false but the first half is true, there has been a lag/problem
                if (startIndex != null && playQueue.size > startIndex) {
                    mediaController.transportControls.skipToQueueItem(playQueue[startIndex].queueId)
                } else mediaController.transportControls.prepare()
            }

            mediaController.transportControls.play()
        }
    }

    /**
     * Add a list of songs to the play queue. The songs can be added to the end of the play queue
     * or after the currently playing song.
     *
     * @param songs - A list containing Song objects that should be added to the play queue.
     * @param addSongsToEndOfQueue - A Boolean indicating whether the songs should be added to
     * the end of the play queue (true) or after the currently playing song (false).
     * @return
     */
    fun addSongsToPlayQueue(songs: List<Song>, addSongsToEndOfQueue: Boolean) {
        val mediaControllerCompat = MediaControllerCompat.getMediaController(this@MainActivity)
        for (song in songs) {
            val mediaDescriptionCompat = mediaDescriptionCompatManager.buildDescription(song)

            if (addSongsToEndOfQueue || playQueue.isEmpty()) {
                mediaControllerCompat.addQueueItem(mediaDescriptionCompat)
            } else {
                val indexOfCurrentlyPlayingQueueItem = playQueue.indexOfFirst {
                    it.queueId == currentlyPlayingQueueItemId
                }
                mediaControllerCompat.addQueueItem(mediaDescriptionCompat,
                    indexOfCurrentlyPlayingQueueItem + 1)
            }
        }

        // TODO: In future, could add a parameter called message that provides a better description.
        //  e.g. "Album Dilla Joints added to the play queue" or "Artist Gordon Lightfoot added to the play queue".
        if (songs.size > 1) Toast.makeText(this@MainActivity,
            "Your songs have been added to the play queue", Toast.LENGTH_SHORT).show()
        else Toast.makeText(this@MainActivity,
            songs[0].title + " has been added to the play queue", Toast.LENGTH_SHORT).show()
    }

    /**
     * Remove a given QueueItem from the play queue based on its ID.
     *
     * @param queueItemId - The ID of the QueueItem to be removed.
     */
    fun removeQueueItemById(queueItemId: Long) {
        if (playQueue.isNotEmpty()) {
            val bundle = Bundle()
            bundle.putLong("queueItemId", queueItemId)

            // TODO: Use the result receiver. Also, maybe try and link this method with the one below for simplicity
            val mediaControllerCompat = MediaControllerCompat.getMediaController(this@MainActivity)
            mediaControllerCompat.sendCommand("removeQueueItemById", bundle, null)
        }
    }

    /**
     * Remove all instances of a given song from the play queue.
     *
     * @param song - The Song object to be removed from the play queue.
     */
    private fun removeAllInstancesOfSongFromPlayQueue(song: Song) {
        if (playQueue.isNotEmpty()) {
            val mediaDescriptionCompat = mediaDescriptionCompatManager.buildDescription(song)
            val mediaControllerCompat = MediaControllerCompat.getMediaController(this@MainActivity)
            mediaControllerCompat.removeQueueItem(mediaDescriptionCompat)
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
    fun skipToQueueItem(queueItemId: Long) = mediaController.transportControls.skipToQueueItem(queueItemId)

    fun hideSystemBars(hide: Boolean) {
        val windowInsetsController = ViewCompat.getWindowInsetsController(window.decorView) ?: return
        if (hide) {
            supportActionBar?.setDisplayShowTitleEnabled(false)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Configure the behavior of the hidden system bars
                windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                // Hide both the status bar and the navigation bar
                windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                // TODO: TEST IF YOU CAN USE THE INITIAL IF EXPRESSION CONTENT FOR ALL API LEVELS
                window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        // Set the content to appear under the system bars so that the
                        // content doesn't resize when the system bars hide and show.
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        // Hide the nav bar and status bar
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN)
            }

            // Hide the toolbar to prevent the SearchView keyboard inadvertently popping up
            binding.toolbar.visibility = View.GONE
        } else {
            supportActionBar?.setDisplayShowTitleEnabled(true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_BARS_BY_SWIPE
                windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
            } else {
                window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
            }
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

    override fun onBackPressed() {
        if (!searchView.isIconified) {
            searchView.isIconified = true
            searchView.onActionViewCollapsed()
        }

        val id = findNavController(R.id.nav_controls_fragment).currentDestination?.id ?: 0
        if (id == R.id.nav_currently_playing) {
            findNavController(R.id.nav_controls_fragment).popBackStack()
            hideSystemBars(false)
        }
        else super.onBackPressed()
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        if (!searchView.isIconified) {
            searchView.isIconified = true
            searchView.onActionViewCollapsed()
        }
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

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
            setTitle("Select a playlist or create a new one")
            setItems(userPlaylistNames) { _, index ->
                val playlist = userPlaylists[index]
                val playlistSongIds = extractPlaylistSongIds(playlist.songs)
                playlistSongIds.addAll(songIds)
                savePlaylistWithSongIds(playlist, playlistSongIds)

                if (songs.size == 1) Toast.makeText(applicationContext,
                    songs[0].title + " has been added to " + playlist.name,
                    Toast.LENGTH_SHORT
                ).show()
                else Toast.makeText(applicationContext,
                    "Your songs have been added to " + playlist.name,
                    Toast.LENGTH_SHORT
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
        val lastUpdate = sharedPreferences.getString("songOfTheDayDate", null)
        when {
            date != lastUpdate -> {
                val song = completeLibrary.random()
                songIDList.add(0, song.songId)
                if (songIDList.size > 30) songIDList.removeAt(songIDList.size - 1)
                savePlaylistWithSongIds(playlist, songIDList)
                val editor = sharedPreferences.edit()
                editor.putString("songOfTheDayDate", date)
                editor.apply()
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
            playlist.songs = convertSongIDListToJson(songIds)
        } else playlist.songs = null
        musicLibraryViewModel.updatePlaylists(listOf(playlist))
    }

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
            if (playQueue.isNotEmpty() &&
                playQueue.indexOfFirst { it.description.mediaId == song.songId.toString() } != -1) {
                val mediaDescriptionCompat = mediaDescriptionCompatManager.buildDescription(song)
                val gson = Gson()
                val mediaDescriptionCompatJson = gson.toJson(mediaDescriptionCompat)

                val bundle = Bundle()
                bundle.putString("mediaDescriptionCompatJson", mediaDescriptionCompatJson)

                val mediaControllerCompat = MediaControllerCompat.getMediaController(this@MainActivity)
                mediaControllerCompat.sendCommand("updateQueueItem", bundle, null)
            }
        }
    }

    fun updateFavourites(song: Song): Boolean? {
        val added: Boolean?
        val favouritesPlaylist = findPlaylist(getString(R.string.favourites))
        if (favouritesPlaylist != null) {
            val songIDList = extractPlaylistSongIds(favouritesPlaylist.songs)
            val index = songIDList.indexOfFirst {
                it == song.songId
            }
            val message: String
            if (index == -1) {
                song.isFavourite = true
                songIDList.add(song.songId)
                message = "Added to"
                added = true
            } else {
                song.isFavourite = false
                songIDList.removeAt(index)
                message = "Removed from"
                added = false
            }

            if (songIDList.isNotEmpty()) {
                val newSongListJSON = convertSongIDListToJson(songIDList)
                favouritesPlaylist.songs = newSongListJSON
            } else favouritesPlaylist.songs = null
            musicLibraryViewModel.updatePlaylists(listOf(favouritesPlaylist))
            updateSongInfo(listOf(song))
            Toast.makeText(this@MainActivity, "$message favourites", Toast.LENGTH_SHORT).show()
            return added
        }
        return null
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
            this.songs = convertSongIDListToJson(songIDList)
            musicLibraryViewModel.updatePlaylists(listOf(this))
        }
    }

    fun findFirstSongArtwork(songID: Long): String? {
        return completeLibrary.find {
            it.songId == songID
        }?.albumId
    }

    fun convertSongIDListToJson(songIDList: List<Long>): String {
        val gPretty = GsonBuilder().setPrettyPrinting().create()
        return gPretty.toJson(songIDList)
    }

    // TODO: Revisit whether it is possible to have a many-to-many association between Playlist and Song
    //  See https://www.oreilly.com/library/view/learning-mysql/0596008643/ch04s04.html
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

    private fun createChannel() {
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

                    val uri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                    )
                    // uri needs to be converted to a string for storage
                    val songUri = uri.toString()

                    val cw = ContextWrapper(application)
                    // path to /data/data/yourapp/app_data/albumArt
                    val directory = cw.getDir("albumArt", Context.MODE_PRIVATE)
                    // Create imageDir
                    val path = File(directory, "$albumID.jpg")
                    // if artwork is not saved then try and find some
                    if (!path.exists()) {
                        val albumArt: Bitmap? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            try {
                                application.contentResolver.loadThumbnail(
                                    uri,
                                    Size(640, 640),
                                    null
                                )
                            } catch (e: FileNotFoundException) {
                                null
                            }
                        } else {
                            try {
                                val mmr = MediaMetadataRetriever()
                                mmr.setDataSource(this@MainActivity, uri)
                                var inputStream: InputStream? = null
                                if (mmr.embeddedPicture != null) inputStream = ByteArrayInputStream(
                                    mmr.embeddedPicture
                                )
                                mmr.release()
                                BitmapFactory.decodeStream(inputStream)
                            } catch (e: FileNotFoundException) {
                                null
                            }
                        }
                        if (albumArt != null) saveImage(albumArt, path)
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
                        songUri,
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
                            playlist.songs = convertSongIDListToJson(newSongIDList)
                            updatedPlaylists.add(playlist)
                        }
                    }
                }
                if (updatedPlaylists.isNotEmpty()) musicLibraryViewModel.updatePlaylists(updatedPlaylists)
            }

            removeAllInstancesOfSongFromPlayQueue(song)
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
            Toast.makeText(this, "Storage permission is needed to run this application",
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

    /**
     * Restore the play queue and playback state from the last save.
     *
     * @return
     */
    private fun restorePlayQueue() = lifecycleScope.launch {
        val mediaDescriptionListJson = sharedPreferences.getString(PLAY_QUEUE_MEDIA_DESCRIPTION_LIST, null) ?: return@launch

        val gson = Gson()
        val itemType = object : TypeToken<List<MediaDescriptionCompat>>() {}.type
        val mediaDescriptionList = gson.fromJson<List<MediaDescriptionCompat>>(mediaDescriptionListJson, itemType)

        val mediaControllerCompat = MediaControllerCompat.getMediaController(this@MainActivity)
        for (mediaDescription in mediaDescriptionList) {
            mediaControllerCompat.addQueueItem(mediaDescription)
        }

        val currentlyQueueItemIndex = sharedPreferences.getInt(CURRENT_QUEUE_ITEM_INDEX, -1)
        if (currentlyQueueItemIndex == -1) return@launch
        if (playQueue.size > currentlyQueueItemIndex) {
            currentlyPlayingQueueItemId = playQueue[currentlyQueueItemIndex].queueId
            mediaController.transportControls.skipToQueueItem(currentlyPlayingQueueItemId)
        }

        val position = sharedPreferences.getInt(PLAYBACK_POSITION, 0)
        if (position != 0) seekTo(position)
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
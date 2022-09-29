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
import android.support.v4.media.session.MediaSessionCompat.*
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
import kotlinx.coroutines.*
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {

    private val channelID = "supernova"
    private var currentPlaybackPosition = 0
    private var currentPlaybackDuration = 0
    var playQueue = mutableListOf<QueueItem>()
    private val playQueueViewModel: PlayQueueViewModel by viewModels()
    private var allPlaylists = listOf<Playlist>()
    private var musicDatabase: MusicDatabase? = null
    var completeLibrary = listOf<Song>()
    private var pbState = STATE_STOPPED
    private var currentlyPlayingQueueItemId = 0L
    private lateinit var mediaBrowser: MediaBrowserCompat
    private lateinit var musicViewModel: MusicViewModel
    private lateinit var searchView: SearchView
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var mediaDescriptionCompatManager: MediaDescriptionCompatManager

    companion object {
        private const val QUEUE = "queue"
        private const val CURRENTLY_PLAYING_QUEUE_ID = "queue_id"
        private const val PLAYBACK_POSITION = "playback_position"
        private const val PLAYBACK_DURATION = "playback_duration"
    }

    private val connectionCallbacks = object : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            super.onConnected()

            // Get the token for the MediaSession
            mediaBrowser.sessionToken.also { token ->

                // Create a MediaControllerCompat
                val mediaControllerCompat = MediaControllerCompat(this@MainActivity, token)
                mediaControllerCompat.registerCallback(controllerCallback)
                // FIXME: Can you change the below to access mediaControllerCompat variable?
                MediaControllerCompat.setMediaController(this@MainActivity, mediaControllerCompat)
            }

            // Register a Callback to stay in sync
            val mediaController = MediaControllerCompat.getMediaController(this@MainActivity)
            mediaController.registerCallback(controllerCallback)

            // retrieve playback position and song
            retrievePlaybackState()
        }
    }

    private var controllerCallback = object : MediaControllerCompat.Callback() {

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            super.onPlaybackStateChanged(state)
            val mediaController = MediaControllerCompat.getMediaController(this@MainActivity)
            if (state == null) return
            // handle what happens when playback is active/paused
            when (state.state) {
                STATE_PLAYING -> {
                    pbState = STATE_PLAYING
                    val playbackPosition = state.position.toInt()
                    if (state.extras != null) {
                        val playbackDuration = state.extras!!.getInt("duration")
                        playQueueViewModel.currentPlaybackDuration.value = playbackDuration
                    }
                    playQueueViewModel.currentPlaybackPosition.value = playbackPosition
                    playQueueViewModel.isPlaying.value = true
                }
                STATE_PAUSED -> {
                    pbState = STATE_PAUSED
                    val playbackPosition = state.position.toInt()
                    if (state.extras != null) {
                        val playbackDuration = state.extras!!.getInt("duration")
                        playQueueViewModel.currentPlaybackDuration.value = playbackDuration
                    }
                    playQueueViewModel.currentPlaybackPosition.value = playbackPosition
                    playQueueViewModel.isPlaying.value = false
                }
                STATE_STOPPED -> {
                    pbState = STATE_STOPPED
                    playQueueViewModel.isPlaying.value = false
                    playQueueViewModel.currentPlayQueue.value = mutableListOf()
                    playQueueViewModel.currentlyPlayingQueueID.value = 0
                    playQueueViewModel.currentlyPlayingSong.value = null
                    playQueueViewModel.currentPlaybackDuration.value = 0
                    playQueueViewModel.currentPlaybackPosition.value = 0
                }
                STATE_SKIPPING_TO_NEXT -> {
                    if (state.extras != null) {
                        val songIsFinished = state.extras!!.getBoolean("finished")
                        val currentQueueItem = playQueue.find {
                            it.queueID == currentlyPlayingQueueItemId
                        }
                        if (songIsFinished && currentQueueItem != null) songFinished(currentQueueItem.song)

                        val repeatSetting = sharedPreferences.getInt("repeat", REPEAT_MODE_NONE)
                        when {
                            repeatSetting == REPEAT_MODE_ONE -> {}
                            playQueue.isNotEmpty() && playQueue[playQueue.size - 1].queueID != currentlyPlayingQueueItemId -> {
                                val index = playQueue.indexOfFirst {
                                    it.queueID == currentlyPlayingQueueItemId
                                }
                                currentlyPlayingQueueItemId = playQueue[index + 1].queueID
                            }
                            // we have reached the end of the queue. check whether we should start over from the beginning
                            repeatSetting == REPEAT_MODE_ALL -> currentlyPlayingQueueItemId = playQueue[0].queueID
                            else -> {
                                mediaController.transportControls.stop()
                                return
                            }
                        }

                        lifecycleScope.launch {
                            updateCurrentlyPlaying()
                            if (pbState == STATE_PLAYING) play()
                        }
                    } else mediaController.transportControls.stop()
                }
                STATE_SKIPPING_TO_PREVIOUS -> {
                    if (playQueue.isNotEmpty() && currentlyPlayingQueueItemId != playQueue[0].queueID) {
                        val index = playQueue.indexOfFirst {
                            it.queueID == currentlyPlayingQueueItemId
                        }
                        currentlyPlayingQueueItemId = playQueue[index - 1].queueID
                        lifecycleScope.launch {
                            updateCurrentlyPlaying()
                            if (pbState == STATE_PLAYING) play()
                        }
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

        // load user settings preferences
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        musicDatabase = MusicDatabase.getDatabase(this, lifecycleScope)
        musicViewModel = ViewModelProvider(this)[MusicViewModel::class.java]
        mediaDescriptionCompatManager = MediaDescriptionManager(this)

        // set up channel for music player notification
        createChannel()

        val taskDescription = ActivityManager.TaskDescription("Supernova", R.drawable.no_album_artwork, getColor(R.color.nav_home))
        this.setTaskDescription(taskDescription)

        // connect to the media browser service
        mediaBrowser = MediaBrowserCompat(
            this,
            ComponentName(this, MediaPlaybackService::class.java),
            connectionCallbacks,
            intent.extras
        )
        mediaBrowser.connect()

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(setOf(R.id.nav_home, R.id.nav_queue, R.id.nav_library, R.id.nav_playlists, R.id.nav_playlist, R.id.nav_artists, R.id.nav_artist, R.id.nav_albums, R.id.nav_album, R.id.nav_songs), binding.drawerLayout)

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

        // prevent icon tints from being overwritten
        binding.navView.itemIconTintList = null

        musicViewModel.allSongs.observe(this) { songs ->
            songs.let { completeLibrary = it.toMutableList() }
        }

        playQueueViewModel.currentPlayQueue.observe(this) { queue ->
            queue?.let {
                playQueue = queue.toMutableList()
                savePlayQueue()
            }
        }

        playQueueViewModel.currentlyPlayingQueueID.observe(this) { id ->
            id?.let {
                currentlyPlayingQueueItemId = id
                savePlayQueueID()
            }
        }

        // keep track of currently playing song position
        playQueueViewModel.currentPlaybackPosition.observe(this) { position ->
            position?.let {
                currentPlaybackPosition = it
            }
        }

        // keep track of currently playing song duration
        playQueueViewModel.currentPlaybackDuration.observe(this) { duration ->
            duration?.let {
                currentPlaybackDuration = it
            }
        }

        musicViewModel.allPlaylists.observe(this) { playlists ->
            playlists.let {
                this.allPlaylists = it
            }
        }

        musicViewModel.mostPlayed.observe(this) { songs ->
            songs.let {
                // update most played playlist
                val playlist = findPlaylist(getString(R.string.most_played))
                if (playlist != null){
                    val mostPlayedSongs = convertSongsToSongIDJSON(it)
                    if (mostPlayedSongs != playlist.songs){
                        playlist.songs = mostPlayedSongs
                        musicViewModel.updatePlaylists(listOf(playlist))
                    }
                }
            }
        }
    }

    fun playPauseControl() {
        when (pbState) {
            STATE_PAUSED -> play()
            STATE_PLAYING -> mediaController.transportControls.pause()
            else -> {
                // Play first song in library if the play queue is currently empty
                if (playQueue.isNullOrEmpty()) playNewSongs(completeLibrary, 0, false)
                else {
                    // It's possible a queue has been built without ever pressing play. In which case, commence playback here
                    lifecycleScope.launch {
                        updateCurrentlyPlaying()
                        play()
                    }
                }
            }
        }
    }

    fun setRepeatMode(repeatMode: Int) {
        val editor = sharedPreferences.edit()
        editor.putInt("repeat", repeatMode)
        editor.apply()
    }

    fun skipBack() = mediaController.transportControls.skipToPrevious()

    fun skipForward() = mediaController.transportControls.skipToNext()

    fun fastRewind() {
        val pos = currentPlaybackPosition - 5000
        if (pos < 0) skipBack()
        else mediaController.transportControls.seekTo(pos.toLong())
    }

    fun fastForward() {
        val pos = currentPlaybackPosition + 5000
        if (pos > currentPlaybackDuration) skipForward()
        else mediaController.transportControls.seekTo(pos.toLong())
    }

    // Returns true if play queue has been shuffled, false if unshuffled
    fun shuffleCurrentPlayQueue(): Boolean {
        val isShuffled = sharedPreferences.getBoolean("shuffle", false)
        if (playQueue.isNotEmpty()) {
            if (isShuffled) {
                playQueue.sortBy {
                    it.queueID
                }
                Toast.makeText(applicationContext, "Play queue unshuffled", Toast.LENGTH_SHORT).show()
            } else {
                val currentQueueItem = playQueue.find {
                    it.queueID == currentlyPlayingQueueItemId
                }
                if (currentQueueItem != null) {
                    playQueue.remove(currentQueueItem)
                    playQueue.shuffle()
                    playQueue.add(0, currentQueueItem)
                    Toast.makeText(applicationContext, "Play queue shuffled", Toast.LENGTH_SHORT).show()
                }
            }
            playQueueViewModel.currentPlayQueue.value = playQueue
        }
        
        val editor = sharedPreferences.edit()
        editor.putBoolean("shuffle", !isShuffled)
        editor.apply()
        return !isShuffled
    }

    fun playNewSongs(playlist: List<Song>, startSong: Int?, shuffle: Boolean) = lifecycleScope.launch(Dispatchers.Main) {
        if (playlist.isNotEmpty()) {
            playQueue = mutableListOf()
            for ((i, s) in playlist.withIndex()) {
                val queueItem = QueueItem(i, s)
                playQueue.add(queueItem)
            }
            if (shuffle) playQueue.shuffle()

            currentlyPlayingQueueItemId = if (shuffle) playQueue[0].queueID
            else startSong ?: 0

            val editor = sharedPreferences.edit()
            editor.putBoolean("shuffle", shuffle)
            editor.apply()
            updateCurrentlyPlaying()
            play()
        }
    }

    private suspend fun updateCurrentlyPlaying() {
        val index = playQueue.indexOfFirst {
            it.queueID == currentlyPlayingQueueItemId
        }
        val currentQueueItem = if (playQueue.isNotEmpty() && index != -1) playQueue[index]
        else null
        // need to retrieve up-to-date song info because properties may have changed since the play queue was created
        val upToDateSong = if (currentQueueItem != null) completeLibrary.find { songs ->
            songs.songId == currentQueueItem.song.songID
        } else null
        if (upToDateSong != null) withContext(Dispatchers.IO) {
            playQueueViewModel.currentlyPlayingSong.postValue(upToDateSong)
            playQueueViewModel.currentPlayQueue.postValue(playQueue)
            playQueueViewModel.currentlyPlayingQueueID.postValue(currentlyPlayingQueueItemId)
            val bundle = Bundle()
            // convert the Song object for each song to JSON and store it in a bundle
            val gPretty = GsonBuilder().setPrettyPrinting().create().toJson(upToDateSong)
            bundle.putString("song", gPretty)
            mediaController.transportControls.prepareFromUri(Uri.parse(upToDateSong.uri), bundle)
        }
    }

    private fun savePlayQueue() = lifecycleScope.launch(Dispatchers.IO) {
        try {
            val editor = sharedPreferences.edit()
            val playQueueJSON = GsonBuilder().setPrettyPrinting().create().toJson(playQueue)
            editor.putString(QUEUE, playQueueJSON)
            editor.apply()
        } catch (e: ConcurrentModificationException) {}
    }

    private fun savePlayQueueID() = lifecycleScope.launch(Dispatchers.IO) {
        val editor = sharedPreferences.edit()
        editor.putInt(CURRENTLY_PLAYING_QUEUE_ID, currentlyPlayingQueueItemId)
        editor.apply()
    }

    private fun play() = mediaController.transportControls.play()

    /**
     * Add a list of songs to the play queue. The songs can be added to the end of the play queue
     * or after the currently playing song.
     *
     * @param songs - A list containing Song objects that should be added to the play queue.
     * @param addSongsToEndOfQueue - A Boolean indicating whether the songs should be added to
     * the end of the play queue (true) or after the currently playing song (false).
     * @return
     */
    // TODO: ACTUALLY - MANAGE PLAY QUEUE USING THE SERVICE. AND REGULARLY GET THE QUEUE USING mediaController.queue
    // TODO: WHEN CHANGING THE CURRENTLY PLAYING SONG, SEND THE ID OF THE CURRENTLY PLAYING QUEUE ITEM
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

    fun removeQueueItem(index: Int) {
        // could use mediaControllerCompat.removeQueueItem(mediaDescriptionCompat) to remove all occurences
        // could use mediaControllerCompat.sendCommand to remove specific occurence e.g. queue item. Use queue item ID ideally not index - in case of issues with play queue synchronisation
        // Actually, maybe use sendCommand for all. You could have a const val REMOVE_ALL_OCCURRENCES_FROM_PLAY_QUEUE with value -1 which signals removeAll (e.g. song removed from library), otherwise, remove specific value

        if (playQueue.isNotEmpty() && index != -1) {
            // Check if the currently playing song is being removed from the play queue
            val currentlyPlayingSongRemoved = playQueue[index].queueID == currentlyPlayingQueueItemId

            playQueue.removeAt(index)

            if (currentlyPlayingSongRemoved) {
                currentlyPlayingQueueItemId = when {
                    playQueue.isEmpty() -> {
                        val mediaController = MediaControllerCompat.getMediaController(this@MainActivity)
                        mediaController.transportControls.stop()
                        return
                    }
                    playQueue.size == index -> playQueue[0].queueID
                    else -> playQueue[index].queueID
                }

                lifecycleScope.launch {
                    updateCurrentlyPlaying()
                    if (pbState == STATE_PLAYING) play()
                }
            }
            playQueueViewModel.currentPlayQueue.postValue(playQueue)
        }
    }

    fun seekTo(position: Int) = mediaController.transportControls.seekTo(position.toLong())

    fun skipToQueueItem(position: Int) {
        currentlyPlayingQueueItemId = playQueue[position].queueID
        lifecycleScope.launch {
            updateCurrentlyPlaying()
            play()
        }
    }

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
                // FIXME: TEST IF YOU CAN USE THE INITIAL IF EXPRESSION CONTENT FOR ALL API LEVELS
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

    fun openAddToPlaylistDialog(songs: List<Song>) {
        val positiveButtonClick = { _: DialogInterface, _: Int ->
            openDialog(CreatePlaylist(songs))
        }

        // retrieving names of all user playlists
        val userPlaylists = allPlaylists.toMutableList()
        userPlaylists.removeAll { item: Playlist ->
            item.isDefault
        }

        // if user has not created a playlist before the skip straight to create new playlist dialog
        if (userPlaylists.isEmpty()){
            openDialog(CreatePlaylist(songs))
            return
        }

        val list: ArrayList<String> = ArrayList()
        for (p in userPlaylists) list.add(p.name)

        var items = arrayOfNulls<String>(list.size)
        items = list.toArray(items)

        AlertDialog.Builder(this).apply {
            setTitle("Select a playlist or create a new one")
            setItems(items) { _, which ->
                addSongsToSavedPlaylist(userPlaylists[which].name, songs)
                if (songs.size == 1) Toast.makeText(
                    applicationContext,
                    songs[0].title + " has been added to " + userPlaylists[which].name,
                    Toast.LENGTH_SHORT
                ).show()
                else Toast.makeText(
                    applicationContext,
                    "Your songs have been added to " + userPlaylists[which].name,
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
        val songIDList = extractPlaylistSongIDs(playlist.songs)
        // updating the song of the day, if one has not already been set for today's date
        val date = SimpleDateFormat.getDateInstance().format(Date())
        val lastUpdate = sharedPreferences.getString("songOfTheDayDate", null)
        when {
            date != lastUpdate -> {
                val song = completeLibrary.random()
                songIDList.add(0, song.songId)
                if (songIDList.size > 30) songIDList.removeAt(songIDList.size - 1)
                savePlaylistNewSongIDList(playlist, songIDList)
                val editor = sharedPreferences.edit()
                editor.putString("songOfTheDayDate", date)
                editor.apply()
            }
            forceUpdate -> {
                // could use removeLast but that command is still experimental at the moment
                if (songIDList.isNotEmpty()) songIDList.removeAt(0)
                val song = completeLibrary.random()
                songIDList.add(0, song.songId)
                savePlaylistNewSongIDList(playlist, songIDList)
            }
        }
    }

    private fun findPlaylist(playlistName: String): Playlist? {
        return allPlaylists.find { item: Playlist ->
            item.name == playlistName
        }
    }

    private fun savePlaylistNewSongIDList(playlist: Playlist, songIDList: List<Long>) {
        if (songIDList.isNotEmpty()) {
            val newSongListJSON = convertSongIDListToJSON(songIDList)
            playlist.songs = newSongListJSON
        } else playlist.songs = null
        musicViewModel.updatePlaylists(listOf(playlist))
    }

    fun savePlaylistNewSongList(playlist: Playlist, songList: List<Song>?) {
        if (songList.isNullOrEmpty()) playlist.songs = null
        else {
            val songIDList = mutableListOf<Long>()
            for (s in songList) songIDList.add(s.songId)
            val newSongListJSON = convertSongIDListToJSON(songIDList)
            playlist.songs = newSongListJSON
        }
        musicViewModel.updatePlaylists(listOf(playlist))
    }

    private fun addSongsToSavedPlaylist(playlistName: String, songs: List<Song>) {
        val playlist = findPlaylist(playlistName)
        if (playlist != null) {
            val songIDList = extractPlaylistSongIDs(playlist.songs)
            for (s in songs) songIDList.add(s.songId)
            savePlaylistNewSongIDList(playlist, songIDList)
        }
    }

    fun deletePlaylist(playlist: Playlist) {
        musicViewModel.deletePlaylist(playlist)
        val cw = ContextWrapper(application)
        val directory = cw.getDir("playlistArt", Context.MODE_PRIVATE)
        val path = File(directory, playlist.playlistId.toString() + ".jpg")
        if (path.exists()) path.delete()
    }

    fun updateSongInfo(songs: List<Song>) = lifecycleScope.launch(Dispatchers.Default) {
        musicViewModel.updateMusicInfo(songs)
        for (s in songs) {
            // see if current play queue needs to be updated
            if (playQueue.isNotEmpty()) {
                val newQueue = playQueue

                fun findIndex(): Int {
                    return newQueue.indexOfFirst {
                        it.song.songID == s.songId
                    }
                }

                // key = queue index, value = queue ID
                val queueIndexQueueIDMap = HashMap<Int, Int>()
                do {
                    val index = findIndex()
                    if (index != -1) {
                        queueIndexQueueIDMap[index] = playQueue[index].queueID
                        newQueue.removeAt(index)
                    }
                } while (index != -1)

                for ((index, queueID) in queueIndexQueueIDMap) {
                    val queueItem = QueueItem(queueID, s)
                    newQueue.add(index, queueItem)
                    playQueueViewModel.currentPlayQueue.postValue(newQueue)
                }
            }
        }
        savePlayQueue()
    }

    fun updateFavourites(song: Song): Boolean? {
        val added: Boolean?
        val favouritesPlaylist = findPlaylist(getString(R.string.favourites))
        if (favouritesPlaylist != null) {
            val songIDList = extractPlaylistSongIDs(favouritesPlaylist.songs)
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
                val newSongListJSON = convertSongIDListToJSON(songIDList)
                favouritesPlaylist.songs = newSongListJSON
            } else favouritesPlaylist.songs = null
            musicViewModel.updatePlaylists(listOf(favouritesPlaylist))
            updateSongInfo(listOf(song))
            Toast.makeText(this@MainActivity, "$message favourites", Toast.LENGTH_SHORT).show()
            return added
        }
        return null
    }

    private fun songFinished(song: Song) = lifecycleScope.launch(Dispatchers.Main) {
        ++song.plays
        val library = completeLibrary.toMutableList()
        library.removeIf {
            it.songId == song.songId
        }
        library.add(song)
        completeLibrary = library
        musicViewModel.updateMusicInfo(listOf(song))
        val recentlyPlayedPlaylist = findPlaylist(getString(R.string.recently_played))
        if (recentlyPlayedPlaylist != null) {
            val songIDList = extractPlaylistSongIDs(recentlyPlayedPlaylist.songs)
            if (songIDList.isNotEmpty()) {
                val index = songIDList.indexOfFirst {
                    it == song.songId
                }
                if (index != -1) songIDList.removeAt(index)
                songIDList.add(0, song.songId)
                if (songIDList.size > 30) songIDList.removeAt(songIDList.size - 1)
            } else songIDList.add(song.songId)
            recentlyPlayedPlaylist.songs = convertSongIDListToJSON(songIDList)
            musicViewModel.updatePlaylists(listOf(recentlyPlayedPlaylist))
        }
    }

    fun findFirstSongArtwork(songID: Long): String? {
        return completeLibrary.find {
            it.songId == songID
        }?.albumId
    }

    fun convertSongsToSongIDJSON(songs: List<Song>): String {
        val songIDs = mutableListOf<Long>()
        for (s in songs) songIDs.add(s.songId)
        return convertSongIDListToJSON(songIDs)
    }

    private fun convertSongIDListToJSON(songIDList: List<Long>): String {
        val gPretty = GsonBuilder().setPrettyPrinting().create()
        return gPretty.toJson(songIDList)
    }

    fun extractPlaylistSongIDs(json: String?): MutableList<Long> {
        return if (json != null) {
            val listType = object : TypeToken<List<Long>>() {}.type
            Gson().fromJson(json, listType)
        } else mutableListOf()
    }

    fun extractPlaylistSongs(json: String?): MutableList<Song> {
        val songIDList = extractPlaylistSongIDs(json)
        return if (songIDList.isNullOrEmpty()) mutableListOf()
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
            musicViewModel.insertPlaylist(playlist)
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

    private fun libraryMaintenance(checkForDeletedSongs: Boolean) = lifecycleScope.launch(
        Dispatchers.Main
    ) {
        val libraryBuilt = libraryRefreshAsync().await()
        if (libraryBuilt && !completeLibrary.isNullOrEmpty()) {
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
                        musicViewModel.insertSongs(songs)
                        songs = mutableListOf()
                    }
                }
            }
        }
        if (songs.isNotEmpty()) musicViewModel.insertSongs(songs)
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

    private fun saveImage(bitmapImage: Bitmap, path: File) {
        try {
            val fos = FileOutputStream(path)
            // Use the compress method on the BitMap object to write image to the OutputStream
            bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, fos)
            fos.close()
        } catch (e: Exception) { }
    }

    private suspend fun tidyArtwork(songs: List<Song>) {
        val cw = ContextWrapper(application)
        val directory = cw.getDir("albumArt", Context.MODE_PRIVATE)
        for (s in songs){
            val artworkInUse = musicDatabase!!.musicDao().doesAlbumIDExist(s.albumId)
            if (artworkInUse.isNullOrEmpty()){
                val path = File(directory, s.albumId + ".jpg")
                if (path.exists()) path.delete()
            }
        }
    }

    // find songs that have been deleted from the device
    private fun checkLibrarySongsExistAsync(): Deferred<List<Song>?> = lifecycleScope.async(
        Dispatchers.IO
    ) {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID
        )
        val libraryCursor = musicQueryAsync(projection).await()
        libraryCursor?.use { cursor ->
            // Cache column indices.
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val songsToBeDeleted = completeLibrary.toMutableList()

            while (cursor.moveToNext()) {
                // Get the song ID of each song on the device
                val id = cursor.getLong(idColumn)

                // run through all songs on the device and see if they match a song in the music library
                // when a match is found, that song is removed from the library MutableList
                // the remaining songs in the songsToBeDeleted MutableList will be songs where the music file no longer exists (because it was not found when searching the device)
                val indexOfSong = songsToBeDeleted.indexOfFirst { song: Song ->
                    song.songId == id
                }
                if (indexOfSong != -1) songsToBeDeleted.removeAt(indexOfSong)
            }
            return@async songsToBeDeleted
        }
    }

    private suspend fun deleteSongs(songs: List<Song>) = lifecycleScope.launch(Dispatchers.Default) {
        for (s in songs) {
            // delete the song from any saved playlists it appears in
            if (!allPlaylists.isNullOrEmpty()) {
                val updatedPlaylists = mutableListOf<Playlist>()
                for (p in allPlaylists) {
                    if (p.songs != null) {
                        val newSongIDList = extractPlaylistSongIDs(p.songs)

                        var playlistModified = false
                        fun findIndex(): Int {
                            return newSongIDList.indexOfFirst {
                                it == s.songId
                            }
                        }

                        // if song is found in playlist then remove it (and keep looping until all instances are removed)
                        do {
                            val index = findIndex()
                            if (index != -1) {
                                newSongIDList.removeAt(index)
                                playlistModified = true
                            }
                        } while (index != -1)

                        if (playlistModified) {
                            // update playlist's songs
                            p.songs = convertSongIDListToJSON(newSongIDList)
                            updatedPlaylists.add(p)
                        }
                    }
                }
                // save updated playlists
                if (updatedPlaylists.isNotEmpty()) musicViewModel.updatePlaylists(updatedPlaylists)
            }

            // check if the song is in the current play queue
            if (playQueue.isNotEmpty()) {
                try {
                    do {
                        val index = playQueue.indexOfFirst {
                            it.song.songID == s.songId
                        }

                        // if song is found in current queue then request its removal
                        if (index != -1) removeQueueItem(index)
                    } while (index != -1)
                } catch (e: ConcurrentModificationException) { }
            }
            // once a song has been removed from all playlists and the play queue it can be deleted from the Room database
            musicViewModel.deleteSong(s)
        }
        // also check to see if we can delete the artwork
        tidyArtwork(songs)
        savePlayQueue()
    }

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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (!MusicPermissionHelper.hasReadPermission(this)) {
            Toast.makeText(
                this,
                "Storage permission is needed to run this application",
                Toast.LENGTH_LONG
            ).show()
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

    private fun retrievePlaybackState() = lifecycleScope.launch {
        val queueJSON = sharedPreferences.getString(QUEUE, null)

        playQueue = if (queueJSON != null) {
            val listType = object : TypeToken<List<QueueItem>>() {}.type
            Gson().fromJson(queueJSON, listType)
        } else mutableListOf()

        playQueueViewModel.currentPlayQueue.value = playQueue
        currentlyPlayingQueueItemId = sharedPreferences.getInt(CURRENTLY_PLAYING_QUEUE_ID, 0)
        playQueueViewModel.currentlyPlayingQueueID.value = currentlyPlayingQueueItemId
        updateCurrentlyPlaying()

        val position = sharedPreferences.getInt(PLAYBACK_POSITION, 0)
        if (position != 0){
            seekTo(position)
            playQueueViewModel.currentPlaybackDuration.value = sharedPreferences.getInt(PLAYBACK_DURATION, 0)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        MediaControllerCompat.getMediaController(this)?.apply {
            transportControls.stop()
            unregisterCallback(controllerCallback)
        }
        mediaBrowser.disconnect()

        val editor = sharedPreferences.edit()
        editor.putInt(PLAYBACK_POSITION, currentPlaybackPosition)
        editor.putInt(PLAYBACK_DURATION, currentPlaybackDuration)
        editor.apply()
    }
}
package com.codersguidebook.supernova

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.codersguidebook.supernova.data.MusicDatabase
import com.codersguidebook.supernova.data.MusicRepository
import com.codersguidebook.supernova.entities.Artist
import com.codersguidebook.supernova.entities.Playlist
import com.codersguidebook.supernova.entities.Song
import com.codersguidebook.supernova.params.SharedPreferencesConstants
import com.codersguidebook.supernova.utils.DefaultPlaylistHelper
import com.codersguidebook.supernova.utils.ImageHandlingHelper
import com.codersguidebook.supernova.utils.PlaylistHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date

class MusicLibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val database = MusicDatabase.getDatabase(application, viewModelScope)
    private val musicDao = database.musicDao()
    private val playlistDao = database.playlistDao()
    private val songPlaysDao = database.songPlaysDao()
    private val repository = MusicRepository(musicDao, playlistDao, songPlaysDao)
    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(application)
    val allSongs: LiveData<List<Song>> = repository.allSongs
    val allArtists: LiveData<List<Artist>> = repository.allArtists
    val allPlaylists: LiveData<List<Playlist>> = repository.allPlaylists
    var songIdToDelete: Long? = null
    private val defaultPlaylistHelper: DefaultPlaylistHelper = DefaultPlaylistHelper(application)

    private val activeAlbumId = MutableLiveData<String>()
    val activeAlbumSongs: LiveData<List<Song>> = activeAlbumId.switchMap {
            albumId -> repository.getSongsByAlbumId(albumId)
    }

    private val activeArtistName = MutableLiveData<String>()
    val activeArtistSongs: LiveData<List<Song>> = activeArtistName.switchMap {
            name -> repository.getSongsByArtist(name)
    }

    private val activePlaylistName = MutableLiveData<String>()
    private val activePlaylist: LiveData<Playlist?> = activePlaylistName.switchMap {
            name -> repository.getPlaylistByNameLiveData(name)
    }
    val activePlaylistSongs: LiveData<List<Song>> = activePlaylist.switchMap { playlist ->
        liveData {
            emit(extractPlaylistSongs(playlist?.songs))
        }
    }

    private val mostPlayedSongsObserver: Observer<List<Long>> = Observer<List<Long>> {
        viewModelScope.launch(Dispatchers.IO) {
            getPlaylistById(defaultPlaylistHelper.mostPlayed.first)?.apply {
                val mostPlayedSongs = PlaylistHelper.serialiseSongIds(it)
                if (mostPlayedSongs != this.songs) {
                    this.songs = mostPlayedSongs
                    updatePlaylists(listOf(this))
                }
            }
        }
    }

    init {
        repository.mostPlayedSongsById.observeForever(mostPlayedSongsObserver)
    }

    override fun onCleared() {
        super.onCleared()
        repository.mostPlayedSongsById.removeObserver(mostPlayedSongsObserver)
    }

    /**
     * Delete a given Song object from the database and any playlists it appears in.
     *
     * @param song The Song object to be deleted.
     */
    fun deleteSong(song: Song) = viewModelScope.launch(Dispatchers.Default) {
        val playlists = withContext(Dispatchers.IO) { getAllPlaylists() }

        val updatedPlaylists = mutableListOf<Playlist>()
        for (playlist in playlists) {
            playlist.songs?.let { json ->
                val songIds = PlaylistHelper.extractSongIds(json)

                var playlistModified = false
                fun findIndex(): Int {
                    return songIds.indexOfFirst {
                        it == song.songId
                    }
                }

                // Remove all instances of the song from the playlist
                do {
                    val index = findIndex()
                    if (index != -1) {
                        songIds.removeAt(index)
                        playlistModified = true
                    }
                } while (index != -1)

                if (playlistModified) {
                    playlist.songs = PlaylistHelper.serialiseSongIds(songIds)
                    updatedPlaylists.add(playlist)
                }
            }
        }
        if (updatedPlaylists.isNotEmpty()) updatePlaylists(updatedPlaylists)

        launch(Dispatchers.IO) {
            repository.deleteSong(song)
        }

        deleteRedundantArtworkBySong(song)
    }

    fun deletePlaylist(playlist: Playlist) = viewModelScope.launch(Dispatchers.IO) {
        repository.deletePlaylist(playlist)
        ImageHandlingHelper.deletePlaylistArtByResourceId(getApplication(), playlist.playlistId.toString())
    }

    fun saveSongs(songs: List<Song>) = viewModelScope.launch(Dispatchers.IO) {
        repository.saveSongs(songs)
    }

    /**
     * Check whether a Playlist listed under a given name exists in the database.
     *
     * @param name The name of the playlist.
     * @return A Boolean indicating whether a corresponding playlist exists.
     */
    suspend fun doesPlaylistExistByName(name: String): Boolean {
        return getPlaylistByName(name) != null
    }

    /**
     * Save a new Playlist to the database.
     *
     * @param playlist The playlist to be saved.
     */
    fun savePlaylist(playlist: Playlist) = viewModelScope.launch(Dispatchers.IO) {
        repository.savePlaylist(playlist)
    }

    fun updateSongs(songs: List<Song>) = viewModelScope.launch(Dispatchers.IO) {
        repository.updateSongs(songs)
    }

    fun updatePlaylists(playlists: List<Playlist>) = viewModelScope.launch(Dispatchers.IO) {
        repository.updatePlaylists(playlists)
    }

    fun increaseSongPlaysBySongId(songId: Long) = viewModelScope.launch(Dispatchers.IO) {
        repository.increaseSongPlaysBySongId(songId)
    }

    /**
     * Check if the album artwork associated with a song that has been removed from the music library
     * is still required. If the artwork is no longer used elsewhere, then the image  can be deleted.
     *
     * @param song The Song object that has been removed from the music library.
     */
    private suspend fun deleteRedundantArtworkBySong(song: Song) {
        if (getSongsByAlbumId(song.albumId).isEmpty()) {
            ImageHandlingHelper.deleteAlbumArtByResourceId(getApplication(), song.albumId)
        }
    }

    /**
     * Retrieve the Song object associated with a given ID.
     *
     * @param songId The ID of the song.
     * @return The associated Song object, or null.
     */
    suspend fun getSongById(songId: Long) : Song? = repository.getSongById(songId)

    /**
     * Retrieve the Song objects associated with a given album ID.
     *
     * @param albumId The ID of the album.
     * @return A list of the associated Song objects sorted by track number.
     */
    private suspend fun getSongsByAlbumId(albumId: String): List<Song> {
        return repository.getSongsByAlbumIdOrderByTrack(albumId)
    }

    /**
     * Find the number of songs associated with a given artist.
     *
     * @param artistName The name of the artist.
     * @return An Integer representing the number of songs found.
     */
    suspend fun getSongPlaysByArtist(artistName: String): Int {
        return repository.getSongPlaysByArtist(artistName)
    }

    /**
     * Toggle the isFavourite field for a given Song object. Also update the favourites
     * playlist accordingly.
     *
     * @param song The Song object that should be favourited/unfavourited.
     * @return A Boolean indicating whether the song has been favourited (true) or unfavourited (false)
     * Null will be returned if no change occurred (e.g. due to an error)
     */
    suspend fun toggleSongFavouriteStatus(song: Song): Boolean? {
        getPlaylistById(defaultPlaylistHelper.favourites.first)?.apply {
            val songIdList = PlaylistHelper.extractSongIds(this.songs)
            val matchingSong = songIdList.firstOrNull { it == song.songId }

            if (matchingSong == null) {
                song.isFavourite = true
                songIdList.add(song.songId)
            } else {
                song.isFavourite = false
                songIdList.remove(matchingSong)
            }

            if (songIdList.isNotEmpty()) {
                val newSongListJSON = PlaylistHelper.serialiseSongIds(songIdList)
                this.songs = newSongListJSON
            } else this.songs = null
            updatePlaylists(listOf(this))
            updateSongs(listOf(song))
            return song.isFavourite
        }
        // FIXME: It would perhaps be better to throw an exception if something goes wrong e.g. playlistNotFoundException
        return null
    }

    /**
     * Find the Playlist object associated with a given ID.
     *
     * @param id The playlist's ID.
     * @return The associated Playlist object or null if no match found.
     */
    private suspend fun getPlaylistById(id: Int): Playlist? = repository.getPlaylistById(id)

    /**
     * Find the Playlist object associated with a given name.
     *
     * @param name The playlist's name.
     * @return The associated Playlist object or null if no match found.
     */
    suspend fun getPlaylistByName(name: String): Playlist? = repository.getPlaylistByName(name)

    /**
     * Set the ID of the active album that a LiveData stream of songs should be loaded for.
     *
     * @param albumId The album's ID.
     */
    fun setActiveAlbumId(albumId: String) {
        if (albumId == activeAlbumId.value) return
        activeAlbumId.value = albumId
    }

    /**
     * Set the name of the active artist that a LiveData stream of songs should be loaded for.
     *
     * @param name The artist's name.
     */
    fun setActiveArtistName(name: String) {
        if (name == activeArtistName.value) return
        activeArtistName.value = name
    }

    /**
     * Set the name of the active playlist that a LiveData stream of songs should be loaded for.
     *
     * @param name The playlist's name.
     */
    fun setActivePlaylistName(name: String) {
        if (name == activePlaylistName.value) return
        activePlaylistName.value = name
    }

    /**
     * Extract the corresponding Song objects for a list of Song IDs that have been
     * saved in JSON format. This method helps restore a playlist.
     *
     * @param json A JSON String representation of the playlist's song IDs.
     * @return A mutable list of Song objects or an empty mutable list.
     */
    suspend fun extractPlaylistSongs(json: String?): MutableList<Song> {
        return PlaylistHelper.extractSongIds(json).mapNotNull { songId ->
            getSongById(songId)
        }.toMutableList()
    }

    /**
     * Update the list of songs associated with a given playlist.
     *
     * @param playlist The target playlist.
     * @param songIds The list of song IDs to be saved with the playlist.
     */
    fun savePlaylistWithSongIds(playlist: Playlist, songIds: List<Long>) {
        if (songIds.isNotEmpty()) {
            playlist.songs = PlaylistHelper.serialiseSongIds(songIds)
        } else playlist.songs = null
        updatePlaylists(listOf(playlist))
    }

    /**
     * Add a song to the Recently Played playlist.
     *
     * @param songId The media ID of the song.
     */
    fun addSongByIdToRecentlyPlayedPlaylist(songId: Long) = viewModelScope.launch(Dispatchers.IO) {
        getPlaylistById(defaultPlaylistHelper.recentlyPlayed.first)?.apply {
            val songIdList = PlaylistHelper.extractSongIds(this.songs)
            if (songIdList.isNotEmpty()) {
                val index = songIdList.indexOfFirst { it == songId }
                if (index != -1) songIdList.removeAt(index)
                songIdList.add(0, songId)
                if (songIdList.size > 30) songIdList.removeAt(songIdList.size - 1)
            } else songIdList.add(songId)
            this.songs = PlaylistHelper.serialiseSongIds(songIdList)
            updatePlaylists(listOf(this))
        }
    }

    /**
     * Refresh the song of the day.
     *
     * @param forceUpdate Whether the song of the day should be refreshed even if it has already
     * been updated for the current day (i.e. the refresh is user-initiated).
     * Default = false.
     */
    fun refreshSongOfTheDay(forceUpdate: Boolean = false) = viewModelScope.launch(Dispatchers.IO) {
        getPlaylistById(defaultPlaylistHelper.songOfTheDay.first)?.apply {
            val songIdList = PlaylistHelper.extractSongIds(this.songs)

            val todayDate = SimpleDateFormat.getDateInstance().format(Date())
            val lastUpdate = sharedPreferences.getString(SharedPreferencesConstants.SONG_OF_THE_DAY_LAST_UPDATED, null)
            when {
                todayDate != lastUpdate || songIdList.isEmpty() -> {
                    val song = repository.getRandomSong() ?: return@launch
                    songIdList.add(0, song.songId)
                    if (songIdList.size > 30) songIdList.removeAt(songIdList.size - 1)
                    savePlaylistWithSongIds(this, songIdList)
                    sharedPreferences.edit().apply {
                        putString(SharedPreferencesConstants.SONG_OF_THE_DAY_LAST_UPDATED, todayDate)
                        apply()
                    }
                }
                forceUpdate -> {
                    songIdList.removeAt(0)
                    val song = repository.getRandomSong() ?: return@launch
                    songIdList.add(0, song.songId)
                    savePlaylistWithSongIds(this, songIdList)
                }
            }
        }
    }

    /**
     * Fetch all the songs held by the database.
     *
     * @return A list of Playlist objects.
     */
    suspend fun getAllSongs(): List<Song> = repository.getAllSongs()

    /**
     * Fetch all the playlists held by the database.
     *
     * @return A list of Playlist objects.
     */
    suspend fun getAllPlaylists(): List<Playlist> = repository.getAllPlaylists()

    /**
     * Fetch all the playlists held by the database that were created by the user.
     *
     * @return A list of Playlist objects.
     */
    suspend fun getAllUserPlaylists(): List<Playlist> = repository.getAllUserPlaylists()
}
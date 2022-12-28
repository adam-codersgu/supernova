package com.codersguidebook.supernova

import android.app.Application
import android.content.ContentUris
import android.database.Cursor
import android.provider.MediaStore
import android.util.Size
import android.widget.Toast
import androidx.lifecycle.*
import androidx.lifecycle.Observer
import androidx.preference.PreferenceManager
import com.codersguidebook.supernova.entities.Artist
import com.codersguidebook.supernova.entities.Playlist
import com.codersguidebook.supernova.entities.Song
import com.codersguidebook.supernova.params.SharedPreferencesConstants
import com.codersguidebook.supernova.utils.ImageHandlingHelper
import com.codersguidebook.supernova.utils.PlaylistHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import java.text.SimpleDateFormat
import java.util.*

class MusicLibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val musicDao = MusicDatabase.getDatabase(application, viewModelScope).musicDao()
    private val playlistDao = MusicDatabase.getDatabase(application, viewModelScope).playlistDao()
    private val repository = MusicRepository(musicDao, playlistDao)
    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(application)
    val allSongs: LiveData<List<Song>> = repository.allSongs
    val allArtists: LiveData<List<Artist>> = repository.allArtists
    val allPlaylists: LiveData<List<Playlist>> = repository.allPlaylists
    val deletedSongIds = MutableLiveData<MutableList<Long>>()

    private val mostPlayedSongsObserver: Observer<List<Long>> = Observer<List<Long>> {
        viewModelScope.launch(Dispatchers.IO) {
            repository.findPlaylistByName(getApplication<Application>().getString(R.string.most_played))?.apply {
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

    private fun deleteSong(song: Song) = viewModelScope.launch(Dispatchers.Default) {
        val playlists = withContext(Dispatchers.IO) {
            repository.getAllPlaylists()
        }

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
            val songIds = deletedSongIds.value ?: mutableListOf()
            songIds.add(song.songId)
            deletedSongIds.postValue(songIds)
        }

        deleteRedundantArtworkBySong(song)
    }

    fun deletePlaylist(playlist: Playlist) = viewModelScope.launch(Dispatchers.IO) {
        repository.deletePlaylist(playlist)
        ImageHandlingHelper.deletePlaylistArtByResourceId(getApplication(), playlist.playlistId.toString())
    }

    private fun saveSongs(songs: List<Song>) = viewModelScope.launch(Dispatchers.IO) {
        repository.saveSongs(songs)
    }

    /**
     * Check whether a Playlist listed under a given name exists in the database.
     *
     * @param name The name of the playlist.
     * @return A Boolean indicating whether a corresponding playlist exists.
     */
    suspend fun doesPlaylistExistByName(name: String): Boolean {
        return repository.findPlaylistByName(name) != null
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
        repository.updatePlaylist(playlists)
    }

    fun increaseSongPlaysBySongId(songId: Long) = viewModelScope.launch(Dispatchers.IO) {
        repository.increaseSongPlaysBySongId(songId)
    }

    /** Refresh the music library. Add new songs and remove deleted songs. */
    fun refreshMusicLibrary() = viewModelScope.launch(Dispatchers.Default) {
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
            for (chunk in chunksToAddToMusicLibrary) saveSongs(chunk)

            val songs = withContext(Dispatchers.IO) {
                repository.getAllSongs()
            }
            val songsToBeDeleted = songs.filterNot { songIds.contains(it.songId) }
            songsToBeDeleted.let {
                for (song in songsToBeDeleted) deleteSong(song)
            }
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
        return getApplication<Application>().contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )
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
        var trackString = cursor.getString(trackColumn) ?: "1001"

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

        val title = cursor.getString(titleColumn) ?: "Unknown song"
        val artist = cursor.getString(artistColumn) ?: "Unknown artist"
        val album = cursor.getString(albumColumn) ?: "Unknown album"
        val year = cursor.getString(yearColumn) ?: "2000"
        val albumId = cursor.getString(albumIDColumn) ?: "unknown_album_ID"
        val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)

        if (!ImageHandlingHelper.doesAlbumArtExistByResourceId(getApplication<Application>(), albumId)) {
            val albumArt = try {
                getApplication<Application>().contentResolver.loadThumbnail(uri,
                    Size(640, 640), null)
            } catch (_: FileNotFoundException) { null }
            albumArt?.let {
                ImageHandlingHelper.saveAlbumArtByResourceId(getApplication<Application>(),
                    albumId, albumArt)
            }
        }

        return Song(id, track, title, artist, album, albumId, year)
    }

    /**
     * The content observer has been notified that a given media store record has been
     * inserted, deleted or modified. This method evaluates the appropriate action to take
     * based on the media store record's media ID.
     *
     * @param mediaId The ID of the target media store record
     */
    fun handleFileUpdateByMediaId(mediaId: Long) = viewModelScope.launch(Dispatchers.IO) {
        val selection = MediaStore.Audio.Media._ID + "=?"
        val selectionArgs = arrayOf(mediaId.toString())
        val cursor = getMediaStoreCursor(selection, selectionArgs)

        val existingSong = getSongById(mediaId)
        when {
            existingSong == null && cursor?.count!! > 0 -> {
                cursor.apply {
                    this.moveToNext()
                    val createdSong = createSongFromCursor(this)
                    saveSongs(listOf(createdSong))
                }
            }
            cursor?.count == 0 -> {
                existingSong?.let {
                    deleteSong(existingSong)
                }
            }
        }
    }

    /**
     * Check if the album artwork associated with a song that has been removed from the music library
     * is still required. If the artwork is no longer used elsewhere, then the image  can be deleted.
     *
     * @param song The Song object that has been removed from the music library.
     */
    private suspend fun deleteRedundantArtworkBySong(song: Song) {
        val songsWithAlbumId = repository.getSongByAlbumId(song.albumId)
        if (songsWithAlbumId.isEmpty()) {
            ImageHandlingHelper.deleteAlbumArtByResourceId(getApplication(), song.albumId)
        }
    }

    /**
     * Retrieve the Song object associated with a given ID.
     *
     * @param songId The ID of the song.
     * @return The associated Song object, or null.
     */
    suspend fun getSongById(songId: Long) : Song? = repository.findSongById(songId)

    /**
     * Retrieve the Song objects associated with a given album ID.
     *
     * @param albumId The ID of the album.
     * @return A list of the associated Song objects sorted by track number.
     */
    // fixme - query repo direct
    fun getSongsByAlbumId(albumId: String) : List<Song> = allSongs.value?.filter {
        it.albumId == albumId
    }?.sortedBy { it.track } ?: listOf()

    /**
     * Toggle the isFavourite field for a given Song object. Also update the favourites
     * playlist accordingly.
     *
     * @param song The Song object that should be favourited/unfavourited.
     */
    fun toggleSongFavouriteStatus(song: Song) = viewModelScope.launch(Dispatchers.IO) {
        repository.findPlaylistByName(getApplication<Application>().getString(R.string.favourites))?.apply {
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
            launch(Dispatchers.Main) {
                if (song.isFavourite) {
                    Toast.makeText(getApplication(),
                        getApplication<Application>().getString(R.string.added_to_favourites),
                        Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(getApplication(),
                        getApplication<Application>().getString(R.string.removed_from_favourites),
                        Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Find the Playlist object associated with a given name.
     *
     * @param name The playlist's name.
     * @return A LiveData representation of the associated Playlist object or null if no match found.
     */
    // fixme: a bad practice is used here. Fix it in lines with https://developer.android.com/topic/libraries/architecture/livedata#transform_livedata
    //  set an active playlist to observe
    // TODO: For areas of the codebase like this, can we use a view model method that itself finds the playlist
    //  and extracts their songs in one go? This would save the coroutine code duplication
    //  e.g. see edit playlist fragment
    fun getPlaylistByName(name: String): LiveData<Playlist?> = repository.findPlaylistByNameLiveData(name)

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
        repository.findPlaylistByName(getApplication<Application>().getString(R.string.recently_played))?.apply {
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
        val playlist = repository.findPlaylistByName(getApplication<Application>()
            .getString(R.string.song_day)) ?: Playlist(0, getApplication<Application>()
            .getString(R.string.song_day), null, false)
        val songIdList = PlaylistHelper.extractSongIds(playlist.songs)

        val todayDate = SimpleDateFormat.getDateInstance().format(Date())
        val lastUpdate = sharedPreferences.getString(SharedPreferencesConstants.SONG_OF_THE_DAY_LAST_UPDATED, null)
        when {
            todayDate != lastUpdate -> {
                val song = repository.findRandomSong() ?: return@launch
                songIdList.add(0, song.songId)
                if (songIdList.size > 30) songIdList.removeAt(songIdList.size - 1)
                savePlaylistWithSongIds(playlist, songIdList)
                sharedPreferences.edit().apply {
                    putString(SharedPreferencesConstants.SONG_OF_THE_DAY_LAST_UPDATED, todayDate)
                    apply()
                }
            }
            forceUpdate -> {
                if (songIdList.isNotEmpty()) songIdList.removeAt(0)
                val song = repository.findRandomSong() ?: return@launch
                songIdList.add(0, song.songId)
                savePlaylistWithSongIds(playlist, songIdList)
            }
        }
    }
}
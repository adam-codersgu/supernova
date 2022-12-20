package com.codersguidebook.supernova

import android.app.Application
import android.content.ContentUris
import android.content.Context
import android.content.ContextWrapper
import android.database.Cursor
import android.provider.MediaStore
import android.util.Size
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.codersguidebook.supernova.entities.Artist
import com.codersguidebook.supernova.entities.Playlist
import com.codersguidebook.supernova.entities.Song
import com.codersguidebook.supernova.utils.ImageHandlingHelper
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileNotFoundException
import java.util.Locale.filter

class MusicLibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MusicRepository
    val allSongs: LiveData<List<Song>>
    val mostPlayedSongsById: LiveData<List<Long>>
    val allArtists: LiveData<List<Artist>>
    val allPlaylists: LiveData<List<Playlist>>
    val deletedSongIds = MutableLiveData<MutableList<Long>>()

    init {
        val musicDao = MusicDatabase.getDatabase(application, viewModelScope).musicDao()
        val playlistDao = MusicDatabase.getDatabase(application, viewModelScope).playlistDao()
        repository = MusicRepository(musicDao, playlistDao)
        allSongs = repository.allSongs
        mostPlayedSongsById = repository.mostPlayedSongsById
        allArtists = repository.allArtists
        allPlaylists = repository.allPlaylists
    }

    private fun deleteSong(song: Song) = viewModelScope.launch(Dispatchers.Default) {
        allPlaylists.value?.let { playlists ->
            if (playlists.isNotEmpty()) {
                val updatedPlaylists = mutableListOf<Playlist>()
                for (playlist in playlists) {
                    if (playlist.songs != null) {
                        // fixme: fix below errors with a PlaylistHelper object
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
                if (updatedPlaylists.isNotEmpty()) updatePlaylists(updatedPlaylists)
            }
        }

        launch(Dispatchers.IO) {
            repository.deleteSong(song)
            deletedSongIds.value?.add(song.songId)
        }

        deleteRedundantArtworkBySong(song)
    }

    fun deletePlaylist(playlist: Playlist) = viewModelScope.launch(Dispatchers.IO) {
        repository.deletePlaylist(playlist)
    }

    private fun insertSongs(songs: List<Song>) = viewModelScope.launch(Dispatchers.IO) {
        repository.insertSongs(songs)
    }

    fun insertPlaylist(playlist: Playlist) = viewModelScope.launch(Dispatchers.IO) {
        repository.insertPlaylist(playlist)
    }

    fun updateMusicInfo(songs: List<Song>) = viewModelScope.launch(Dispatchers.IO) {
        repository.updateMusicInfo(songs)
    }

    fun updatePlaylists(playlists: List<Playlist>) = viewModelScope.launch(Dispatchers.IO) {
        repository.updatePlaylist(playlists)
    }

    fun increaseSongPlaysBySongId(songId: Long) = viewModelScope.launch(Dispatchers.IO) {
        repository.increaseSongPlaysBySongId(songId)
    }

    /** Refresh the music library. Add new songs and remove deleted songs. */
    fun refreshMusicLibrary() = viewModelScope.launch(Dispatchers.Main) {
        val cursor = getMediaStoreCursorAsync().await()

        val songsToAddToMusicLibrary = mutableListOf<Song>()
        cursor?.use {
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
            for (chunk in chunksToAddToMusicLibrary) insertSongs(chunk)

            val songsToBeDeleted = allSongs.value?.filterNot { songIds.contains(it.songId) }
            songsToBeDeleted?.let {
                for (song in songsToBeDeleted) deleteSong(song)
            }
        }
    }

    /**
     * Obtain a Cursor featuring all music entries in the media store that fulfil a given
     * selection criteria.
     *
     * @param selection - The WHERE clause for the media store query.
     * Default = standard WHERE clause that selects only music entries.
     * @param selectionArgs - An array of String selection arguments that filter the results
     * that are returned in the Cursor.
     * Default = null (no selection arguments).
     * @return A Cursor object detailing all the relevant media store entries.
     */
    private fun getMediaStoreCursorAsync(selection: String = MediaStore.Audio.Media.IS_MUSIC,
                                         selectionArgs: Array<String>? = null)
            : Deferred<Cursor?> = viewModelScope.async(Dispatchers.IO) {
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
        return@async getApplication<Application>().contentResolver.query(
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
     * @param cursor - A Cursor object that is set to the row containing the metadata that a Song
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
     * @param mediaId - The ID of the target media store record
     */
    fun handleFileUpdateByMediaId(mediaId: Long) = viewModelScope.launch {
        val selection = MediaStore.Audio.Media._ID + "=?"
        val selectionArgs = arrayOf(mediaId.toString())
        val cursor = getMediaStoreCursorAsync(selection, selectionArgs).await()

        val existingSong = getSongById(mediaId)
        when {
            existingSong == null && cursor?.count!! > 0 -> {
                cursor.apply {
                    this.moveToNext()
                    val createdSong = createSongFromCursor(this)
                    insertSongs(listOf(createdSong))
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
     * @param song - The Song object that has been removed from the music library.
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
     * @param songId - The ID of the song.
     * @return The associated Song object, or null.
     */
    fun getSongById(songId: Long) : Song? = allSongs.value?.find { it.songId == songId }

    /**
     * Retrieve the Song objects associated with a given album ID.
     *
     * @param albumId - The ID of the album.
     * @return A list of the associated Song objects sorted by track number.
     */
    fun getSongsByAlbumId(albumId: String) : List<Song> = allSongs.value?.filter {
        it.albumId == albumId
    }?.sortedBy { it.track } ?: listOf()
}
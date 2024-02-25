package com.codersguidebook.supernova

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import com.codersguidebook.supernova.data.MusicRepository
import com.codersguidebook.supernova.entities.Playlist
import com.codersguidebook.supernova.fixture.PlaylistFixture.getMockPlaylist
import com.codersguidebook.supernova.fixture.PlaylistFixture.getMockSong
import com.codersguidebook.supernova.params.SharedPreferencesConstants
import com.codersguidebook.supernova.testutils.ReflectionUtils
import com.codersguidebook.supernova.utils.DefaultPlaylistHelper
import com.codersguidebook.supernova.utils.PlaylistHelper
import io.kotest.inspectors.forAll
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.lang.Thread.sleep
import java.text.SimpleDateFormat
import java.util.Date

@Suppress("UNCHECKED_CAST")
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class MusicLibraryViewModelTest {

    private lateinit var defaultPlaylistHelper: DefaultPlaylistHelper
    private lateinit var musicLibraryViewModel: MusicLibraryViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        defaultPlaylistHelper = DefaultPlaylistHelper(RuntimeEnvironment.getApplication())
        musicLibraryViewModel = MusicLibraryViewModel(RuntimeEnvironment.getApplication())
    }

    @Test
    fun toggleSongFavouriteStatus_success_add_favourite_song() = runTest {
        repositoryShouldReturnFavouritesPlaylistById()
        val songToFavourite = getMockSong(2L, false)

        val isFavourited = musicLibraryViewModel.toggleSongFavouriteStatus(songToFavourite)

        if (isFavourited != null) {
            assertTrue(isFavourited)
        } else {
            fail("isFavourited should not be null")
        }
        assertTrue(songToFavourite.isFavourite)
    }

    @Test
    fun toggleSongFavouriteStatus_success_remove_favourite_song() = runTest {
        repositoryShouldReturnFavouritesPlaylistById()
        val songToFavourite = getMockSong(1L, true)

        val isFavourited = musicLibraryViewModel.toggleSongFavouriteStatus(songToFavourite)

        if (isFavourited != null) {
            assertFalse(isFavourited)
        } else {
            fail("isFavourited should not be null")
        }
        assertFalse(songToFavourite.isFavourite)
    }

    private suspend fun repositoryShouldReturnFavouritesPlaylistById() {
        val mockRepository = mock(MusicRepository::class.java)
        val mockPlaylist = getMockFavouritesPlaylist()
        Mockito.`when`(mockRepository.getPlaylistById(defaultPlaylistHelper.favourites.first)).doReturn(mockPlaylist)
        ReflectionUtils.replaceFieldWithMock(musicLibraryViewModel, "repository", mockRepository)
    }

    @Test
    fun toggleSongFavouriteStatus_error_favourites_playlist_not_found() = runTest {
        val mockRepository = mock(MusicRepository::class.java)
        Mockito.`when`(mockRepository.getPlaylistById(defaultPlaylistHelper.favourites.first)).doReturn(null)
        ReflectionUtils.replaceFieldWithMock(musicLibraryViewModel, "repository", mockRepository)
        val songToFavourite = getMockSong(2L, false)

        val isFavourited = musicLibraryViewModel.toggleSongFavouriteStatus(songToFavourite)

        assertNull(isFavourited)
        assertFalse(songToFavourite.isFavourite)
    }

    @Test
    fun getPlaylistByName_playlist_exists() = runTest {
        val mockPlaylist = whenGetPlaylistByNameReturnPlaylistA()

        val playlist = musicLibraryViewModel.getPlaylistByName("Playlist A")

        assertEquals(mockPlaylist.toString(), playlist.toString())
    }

    @Test
    fun getPlaylistByName_playlist_does_not_exist() = runTest {
        whenGetPlaylistByNameReturnPlaylistA()

        val playlist = musicLibraryViewModel.getPlaylistByName("Playlist B")

        assertNull(playlist)
    }

    private suspend fun whenGetPlaylistByNameReturnPlaylistA(): Playlist {
        val mockRepository = mock(MusicRepository::class.java)
        val mockPlaylist = getMockPlaylist()
        Mockito.`when`(mockRepository.getPlaylistByName("Playlist A")).doReturn(mockPlaylist)
        ReflectionUtils.replaceFieldWithMock(musicLibraryViewModel, "repository", mockRepository)
        return mockPlaylist
    }

    @Test
    fun setActiveAlbumId_success() {
        // Given no album ID is set
        val activeAlbumIdField = ReflectionUtils.setFieldVisible(musicLibraryViewModel, "activeAlbumId")
        val activeAlbumId = activeAlbumIdField.get(musicLibraryViewModel) as MutableLiveData<String>
        assertNull(activeAlbumId.value)
        
        // When setActiveAlbumId is called with a valid String
        val expectedActiveAlbumId = "3"
        musicLibraryViewModel.setActiveAlbumId(expectedActiveAlbumId)

        // Then the supplied String will be assigned to the activeAlbumId field
        assertEquals(expectedActiveAlbumId, activeAlbumId.value)
    }

    @Test
    fun setActiveAlbumId_empty_string_success() {
        // Given the album ID is set to 2
        val activeAlbumIdField = ReflectionUtils.setFieldVisible(musicLibraryViewModel, "activeAlbumId")
        val activeAlbumId = activeAlbumIdField.get(musicLibraryViewModel) as MutableLiveData<String>
        activeAlbumId.value = "2"

        assertEquals("2", activeAlbumId.value)

        // When setActiveAlbumId is called with an empty String
        musicLibraryViewModel.setActiveAlbumId("")

        // Then the supplied String will be assigned to the activeAlbumId field
        assertEquals("", activeAlbumId.value)
    }

    @Test
    fun setActiveArtistName_success() {
        // Given no artist name is set
        val activeArtistNameField = ReflectionUtils.setFieldVisible(musicLibraryViewModel, "activeArtistName")
        val activeArtistName = activeArtistNameField.get(musicLibraryViewModel) as MutableLiveData<String>
        assertNull(activeArtistName.value)

        // When setActiveArtistName is called with a valid String
        val expectedActiveArtistName = "Band B"
        musicLibraryViewModel.setActiveArtistName(expectedActiveArtistName)

        // Then the supplied String will be assigned to the activeArtistName field
        assertEquals(expectedActiveArtistName, activeArtistName.value)
    }

    @Test
    fun setActiveArtistName_empty_string_success() {
        // Given the artist name is set to Band A
        val activeArtistNameField = ReflectionUtils.setFieldVisible(musicLibraryViewModel, "activeArtistName")
        val activeArtistName = activeArtistNameField.get(musicLibraryViewModel) as MutableLiveData<String>
        activeArtistName.value = "Band A"

        assertEquals("Band A", activeArtistName.value)

        // When setActiveArtistName is called with an empty String
        musicLibraryViewModel.setActiveArtistName("")

        // Then the supplied String will be assigned to the activeArtistName field
        assertEquals("", activeArtistName.value)
    }

    @Test
    fun refreshSongOfTheDay_notLoadedForToday_success() = runTest {
        val mockRepository = mock(MusicRepository::class.java)
        val mockSharedPreferences = mock(SharedPreferences::class.java)
        val mockEditor = mock(SharedPreferences.Editor::class.java)
        val mockPlaylist = getMockSongOfTheDayPlaylist()
        Mockito.`when`(mockRepository.getPlaylistById(defaultPlaylistHelper.songOfTheDay.first)).doReturn(mockPlaylist)
        Mockito.`when`(mockRepository.getRandomSong()).doReturn(getMockSong(2L))
        Mockito.`when`(mockSharedPreferences.getString(SharedPreferencesConstants.SONG_OF_THE_DAY_LAST_UPDATED, null))
            .doReturn(null)
        Mockito.`when`(mockSharedPreferences.edit()).doReturn(mockEditor)
        ReflectionUtils.replaceFieldWithMock(musicLibraryViewModel, "repository", mockRepository)
        ReflectionUtils.replaceFieldWithMock(musicLibraryViewModel, "sharedPreferences", mockSharedPreferences)

        refreshSongOfTheDay()

        Mockito.verify(mockRepository).updatePlaylists(listOf(mockPlaylist))
        val todayDate = SimpleDateFormat.getDateInstance().format(Date())
        Mockito.verify(mockEditor).putString(SharedPreferencesConstants.SONG_OF_THE_DAY_LAST_UPDATED, todayDate)
    }

    @Test
    fun refreshSongOfTheDay_alreadyLoadedForToday_success() = runTest {
        val todayDate = SimpleDateFormat.getDateInstance().format(Date())

        val mockRepository = mock(MusicRepository::class.java)
        val mockSharedPreferences = mock(SharedPreferences::class.java)
        val mockEditor = mock(SharedPreferences.Editor::class.java)
        val mockPlaylist = getMockSongOfTheDayPlaylist()
        Mockito.`when`(mockRepository.getPlaylistById(defaultPlaylistHelper.songOfTheDay.first)).doReturn(mockPlaylist)
        Mockito.`when`(mockRepository.getRandomSong()).doReturn(getMockSong(2L))
        Mockito.`when`(mockSharedPreferences.getString(SharedPreferencesConstants.SONG_OF_THE_DAY_LAST_UPDATED, null))
            .doReturn(todayDate)
        Mockito.`when`(mockSharedPreferences.edit()).doReturn(mockEditor)
        ReflectionUtils.replaceFieldWithMock(musicLibraryViewModel, "repository", mockRepository)
        ReflectionUtils.replaceFieldWithMock(musicLibraryViewModel, "sharedPreferences", mockSharedPreferences)

        refreshSongOfTheDay()

        Mockito.verify(mockRepository, never()).updatePlaylists(any())
        Mockito.verify(mockEditor, never()).putString(any(), any())
    }

    private fun refreshSongOfTheDay() {
        musicLibraryViewModel.refreshSongOfTheDay()
        // FIXME - Need to use a better solution for pausing the thread - also other tests already written that could benefit e.g. favourites tests?
        sleep(100)
    }

    // todo Have some tests for the force update
    //  Have tests for a 30 song length playlist

    @Test
    fun getAllSongs_success() = runTest {
        val mockRepository = mock(MusicRepository::class.java)
        val song = getMockSong()
        Mockito.`when`(mockRepository.getAllSongs()).doReturn(listOf(song))
        ReflectionUtils.replaceFieldWithMock(musicLibraryViewModel, "repository", mockRepository)

        val songs = musicLibraryViewModel.getAllSongs()
        assertEquals(1, songs.size)
    }

    @Test
    fun getAllPlaylists_success() = runTest {
        val mockRepository = mock(MusicRepository::class.java)
        val userPlaylist = getMockPlaylist()
        val defaultPlaylist = getMockFavouritesPlaylist()
        Mockito.`when`(mockRepository.getAllPlaylists()).doReturn(listOf(userPlaylist, defaultPlaylist))
        ReflectionUtils.replaceFieldWithMock(musicLibraryViewModel, "repository", mockRepository)

        val playlists = musicLibraryViewModel.getAllPlaylists()
        assertEquals(2, playlists.size)
    }

    @Test
    fun getAllUserPlaylists_success() = runTest {
        val mockRepository = mock(MusicRepository::class.java)
        val userPlaylist = getMockPlaylist()
        Mockito.`when`(mockRepository.getAllUserPlaylists()).doReturn(listOf(userPlaylist))
        ReflectionUtils.replaceFieldWithMock(musicLibraryViewModel, "repository", mockRepository)

        val playlists = musicLibraryViewModel.getAllUserPlaylists()
        assertEquals(1, playlists.size)
        playlists.forAll {
            !it.isDefault
        }
    }

    // TODO: Delegate the below playlist and song data setup methods to a fixture class
    private fun getMockFavouritesPlaylist(): Playlist {
        val songIds = PlaylistHelper.serialiseSongIds(listOf(getMockSong(true).songId))
        return Playlist(defaultPlaylistHelper.favourites.first,
            defaultPlaylistHelper.favourites.second, songIds, true)
    }

    private fun getMockSongOfTheDayPlaylist(): Playlist {
        val songIds = PlaylistHelper.serialiseSongIds(listOf(getMockSong().songId))
        return Playlist(defaultPlaylistHelper.songOfTheDay.first,
            defaultPlaylistHelper.songOfTheDay.second, songIds, true)
    }
}
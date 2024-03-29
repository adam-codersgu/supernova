package com.codersguidebook.supernova

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import com.codersguidebook.supernova.data.MusicRepository
import com.codersguidebook.supernova.entities.Playlist
import com.codersguidebook.supernova.fixture.PlaylistFixture.getMockFavouritesPlaylist
import com.codersguidebook.supernova.fixture.PlaylistFixture.getMockPlaylist
import com.codersguidebook.supernova.fixture.PlaylistFixture.getMockSong
import com.codersguidebook.supernova.fixture.PlaylistFixture.getMockSongOfTheDayPlaylist
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

    private val today = SimpleDateFormat.getDateInstance().format(Date())

    private val mockRepository = mock(MusicRepository::class.java)
    private val mockSharedPreferences = mock(SharedPreferences::class.java)
    private val mockEditor = mock(SharedPreferences.Editor::class.java)

    private lateinit var defaultPlaylistHelper: DefaultPlaylistHelper
    private lateinit var musicLibraryViewModel: MusicLibraryViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        defaultPlaylistHelper = DefaultPlaylistHelper(RuntimeEnvironment.getApplication())
        musicLibraryViewModel = MusicLibraryViewModel(RuntimeEnvironment.getApplication())
        ReflectionUtils.replaceFieldWithMock(musicLibraryViewModel, "repository", mockRepository)
        Mockito.`when`(mockSharedPreferences.edit()).doReturn(mockEditor)
        ReflectionUtils.replaceFieldWithMock(musicLibraryViewModel, "sharedPreferences", mockSharedPreferences)
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
        val mockPlaylist = getMockFavouritesPlaylist()
        Mockito.`when`(mockRepository.getPlaylistById(defaultPlaylistHelper.favourites.first)).doReturn(mockPlaylist)
    }

    @Test
    fun toggleSongFavouriteStatus_error_favourites_playlist_not_found() = runTest {
        Mockito.`when`(mockRepository.getPlaylistById(defaultPlaylistHelper.favourites.first)).doReturn(null)
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
        val mockPlaylist = getMockPlaylist()
        Mockito.`when`(mockRepository.getPlaylistByName("Playlist A")).doReturn(mockPlaylist)
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
        val mockPlaylist = configureSongOfTheDayPlaylistByLastUpdated(null)

        refreshSongOfTheDay()

        assertEquals(2, PlaylistHelper.extractSongIds(mockPlaylist.songs).size)
        assertEquals(2L, PlaylistHelper.extractSongIds(mockPlaylist.songs)[0])
        assertEquals(1L, PlaylistHelper.extractSongIds(mockPlaylist.songs)[1])
        Mockito.verify(mockRepository).updatePlaylists(listOf(mockPlaylist))
        val todayDate = SimpleDateFormat.getDateInstance().format(Date())
        Mockito.verify(mockEditor).putString(SharedPreferencesConstants.SONG_OF_THE_DAY_LAST_UPDATED, todayDate)
    }

    @Test
    fun refreshSongOfTheDay_alreadyLoadedForToday_success() = runTest {
        val mockPlaylist = configureSongOfTheDayPlaylistByLastUpdated(today)

        refreshSongOfTheDay()

        assertEquals(1, PlaylistHelper.extractSongIds(mockPlaylist.songs).size)
        assertEquals(1L, PlaylistHelper.extractSongIds(mockPlaylist.songs)[0])
        Mockito.verify(mockRepository, never()).updatePlaylists(any())
        Mockito.verify(mockEditor, never()).putString(any(), any())
    }

    @Test
    fun refreshSongOfTheDay_forceUpdate_success() = runTest {
        val mockPlaylist = configureSongOfTheDayPlaylistByLastUpdated(today)

        refreshSongOfTheDay(true)

        assertEquals(1, PlaylistHelper.extractSongIds(mockPlaylist.songs).size)
        assertEquals(2L, PlaylistHelper.extractSongIds(mockPlaylist.songs)[0])
        Mockito.verify(mockRepository).updatePlaylists(listOf(mockPlaylist))
        Mockito.verify(mockEditor, never()).putString(any(), any())
    }

    private suspend fun configureSongOfTheDayPlaylistByLastUpdated(dateLastUpdated: String?) : Playlist{
        val mockPlaylist = getMockSongOfTheDayPlaylist()
        Mockito.`when`(mockRepository.getPlaylistById(defaultPlaylistHelper.songOfTheDay.first)).doReturn(mockPlaylist)
        Mockito.`when`(mockRepository.getRandomSong()).doReturn(getMockSong(2L))
        Mockito.`when`(mockSharedPreferences.getString(SharedPreferencesConstants.SONG_OF_THE_DAY_LAST_UPDATED, null))
            .doReturn(dateLastUpdated)
        return mockPlaylist
    }

    @Test
    fun refreshSongOfTheDay_30SongsLimitReached_success() = runTest {
        val mockPlaylist = getMockSongOfTheDayPlaylist(30)
        Mockito.`when`(mockRepository.getPlaylistById(defaultPlaylistHelper.songOfTheDay.first)).doReturn(mockPlaylist)
        Mockito.`when`(mockRepository.getRandomSong()).doReturn(getMockSong(31L))
        Mockito.`when`(mockSharedPreferences.getString(SharedPreferencesConstants.SONG_OF_THE_DAY_LAST_UPDATED, null))
            .doReturn(null)
        assertMaxLengthSongOfTheDayPlaylistElements(mockPlaylist, 1L, 30L)

        refreshSongOfTheDay()

        assertMaxLengthSongOfTheDayPlaylistElements(mockPlaylist, 31L, 29L)
        Mockito.verify(mockRepository).updatePlaylists(listOf(mockPlaylist))
        Mockito.verify(mockEditor).putString(SharedPreferencesConstants.SONG_OF_THE_DAY_LAST_UPDATED, today)
    }

    private fun assertMaxLengthSongOfTheDayPlaylistElements(playlist: Playlist,
                                                            expectedIdOfFirstElement: Long,
                                                            expectedIdOfLastElement: Long) {
        val extractedSongs = PlaylistHelper.extractSongIds(playlist.songs)
        assertEquals(30, extractedSongs.size)
        assertEquals(expectedIdOfFirstElement, extractedSongs[0])
        assertEquals(expectedIdOfLastElement, extractedSongs[extractedSongs.size - 1])
    }

    private fun refreshSongOfTheDay(forceUpdate: Boolean = false) {
        musicLibraryViewModel.refreshSongOfTheDay(forceUpdate)
        // FIXME - Need to use a better solution for pausing the thread - also other tests already written that could benefit e.g. favourites tests?
        sleep(100)
    }

    @Test
    fun getAllSongs_success() = runTest {
        val song = getMockSong()
        Mockito.`when`(mockRepository.getAllSongs()).doReturn(listOf(song))

        val songs = musicLibraryViewModel.getAllSongs()
        assertEquals(1, songs.size)
    }

    @Test
    fun getAllPlaylists_success() = runTest {
        val userPlaylist = getMockPlaylist()
        val defaultPlaylist = getMockFavouritesPlaylist()
        Mockito.`when`(mockRepository.getAllPlaylists()).doReturn(listOf(userPlaylist, defaultPlaylist))

        val playlists = musicLibraryViewModel.getAllPlaylists()
        assertEquals(2, playlists.size)
    }

    @Test
    fun getAllUserPlaylists_success() = runTest {
        val userPlaylist = getMockPlaylist()
        Mockito.`when`(mockRepository.getAllUserPlaylists()).doReturn(listOf(userPlaylist))

        val playlists = musicLibraryViewModel.getAllUserPlaylists()
        assertEquals(1, playlists.size)
        playlists.forAll {
            !it.isDefault
        }
    }
}
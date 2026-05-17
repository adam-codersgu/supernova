package com.codersguidebook.supernova

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import com.codersguidebook.supernova.data.MusicRepository
import com.codersguidebook.supernova.entities.Playlist
import com.codersguidebook.supernova.entities.Song
import com.codersguidebook.supernova.fixture.PlaylistFixture.getMockFavouritesPlaylist
import com.codersguidebook.supernova.fixture.PlaylistFixture.getMockSong
import com.codersguidebook.supernova.testutils.InstantTaskExecutorExtension
import com.codersguidebook.supernova.testutils.ReflectionUtils
import com.codersguidebook.supernova.utils.DefaultPlaylistHelper
import com.codersguidebook.supernova.utils.PlaylistHelper
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.never
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.text.SimpleDateFormat
import java.util.Date

@ExtendWith(MockitoExtension::class, InstantTaskExecutorExtension::class)
class MusicLibraryViewModelTest {

    @Mock
    lateinit var application: Application

    @Mock
    lateinit var defaultPlaylistHelper: DefaultPlaylistHelper

    @Mock
    lateinit var repository: MusicRepository

    @Mock
    lateinit var sharedPreferences: SharedPreferences

    private lateinit var musicLibraryViewModel: MusicLibraryViewModel
    private lateinit var staticMockPreferenceManager: MockedStatic<PreferenceManager>



    private val today = SimpleDateFormat.getDateInstance().format(Date())

    private val mockEditor = mock(SharedPreferences.Editor::class.java)

    /* @BeforeEach
    fun setup() {
        Mockito.`when`(mockSharedPreferences.edit()).doReturn(mockEditor)
        ReflectionUtils.replaceFieldWithMock(musicLibraryViewModel, "sharedPreferences", mockSharedPreferences)
    } */

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        staticMockPreferenceManager = mockStatic(PreferenceManager::class.java)
        staticMockPreferenceManager.`when`<SharedPreferences> {
            PreferenceManager.getDefaultSharedPreferences(application)
        }.thenReturn(sharedPreferences)

        val mostPlayedSongsLiveData = MutableLiveData<List<Long>>()
        `when`(repository.mostPlayedSongsById).thenReturn(mostPlayedSongsLiveData)

        musicLibraryViewModel = MusicLibraryViewModel(application, repository, defaultPlaylistHelper)
    }

    @AfterEach
    fun tearDown() {
        staticMockPreferenceManager.close()
    }

    @Nested
    @DisplayName("When the song is already a favourite")
    inner class ToggleSongFavouriteStatus {
        @Test
        fun success_add_favourite_song() = runTest {
            repositoryShouldReturnFavouritesPlaylistById()
            val songToFavourite = getMockSong(99L, false)

            val isFavourite = musicLibraryViewModel.toggleSongFavouriteStatus(songToFavourite)

            assertTrue(isFavourite)
            assertTrue(songToFavourite.isFavourite)

            val playlistCaptor = argumentCaptor<List<Playlist>>()
            verify(repository).updatePlaylists(playlistCaptor.capture())
            val updatedIds = PlaylistHelper.extractSongIds(playlistCaptor.firstValue.first().songs)
            assertTrue(updatedIds.size > 1)
            assertTrue(updatedIds.contains(99L))
        }

        @Test
        fun success_remove_favourite_song() = runTest {
            repositoryShouldReturnFavouritesPlaylistById()
            val songToFavourite = getMockSong(1L, true)

            val isFavourite = musicLibraryViewModel.toggleSongFavouriteStatus(songToFavourite)

            assertFalse(isFavourite)
            assertFalse(songToFavourite.isFavourite)

            val playlistCaptor = argumentCaptor<List<Playlist>>()
            verify(repository).updatePlaylists(playlistCaptor.capture())
            val updatedIds = PlaylistHelper.extractSongIds(playlistCaptor.firstValue.first().songs)
            assertTrue(updatedIds.isEmpty())
        }

        @Test
        fun failure_playlist_not_found() = runTest {
            val songToFavourite = getMockSong(2L, false)

            runCatching { musicLibraryViewModel.toggleSongFavouriteStatus(songToFavourite) }

            verify(repository, never()).updateSongs(any())
            verify(repository, never()).updatePlaylists(any())
        }
    }

    private suspend fun repositoryShouldReturnFavouritesPlaylistById() {
        `when`(this.defaultPlaylistHelper.favourites).doReturn(Pair(1, "Favourites"))
        val mockPlaylist = getMockFavouritesPlaylist()
        `when`(repository.getPlaylistById(this.defaultPlaylistHelper.favourites.first)).doReturn(mockPlaylist)
    }

    /* @Test
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
    } */
}
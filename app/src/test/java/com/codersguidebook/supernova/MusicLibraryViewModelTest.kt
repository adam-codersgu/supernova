package com.codersguidebook.supernova

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import com.codersguidebook.supernova.data.MusicRepository
import com.codersguidebook.supernova.entities.Playlist
import com.codersguidebook.supernova.entities.Song
import com.codersguidebook.supernova.exception.PlaylistNotFoundException
import com.codersguidebook.supernova.fixture.PlaylistFixture.getMockFavouritesPlaylist
import com.codersguidebook.supernova.fixture.PlaylistFixture.getMockPlaylist
import com.codersguidebook.supernova.fixture.PlaylistFixture.getMockSong
import com.codersguidebook.supernova.fixture.PlaylistFixture.getMockSongOfTheDayPlaylist
import com.codersguidebook.supernova.params.SharedPreferencesConstants
import com.codersguidebook.supernova.testutils.InstantTaskExecutorExtension
import com.codersguidebook.supernova.testutils.ReflectionUtils
import com.codersguidebook.supernova.utils.DefaultPlaylistHelper
import com.codersguidebook.supernova.utils.ImageHandlingHelper
import com.codersguidebook.supernova.utils.PlaylistHelper
import io.kotest.inspectors.forAll
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.never
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.verify
import java.lang.Thread.sleep
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.reflect.full.callSuspend

@ExtendWith(MockitoExtension::class, InstantTaskExecutorExtension::class)
class MusicLibraryViewModelTest {

    @Mock
    lateinit var application: Application

    @Mock
    lateinit var defaultPlaylistHelper: DefaultPlaylistHelper

    @Mock
    lateinit var editor: SharedPreferences.Editor

    @Mock
    lateinit var repository: MusicRepository

    @Mock
    lateinit var sharedPreferences: SharedPreferences

    private lateinit var musicLibraryViewModel: MusicLibraryViewModel
    private lateinit var staticMockPreferenceManager: MockedStatic<PreferenceManager>

    private val today = SimpleDateFormat.getDateInstance().format(Date())

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
    @DisplayName("Toggle the song favourite status")
    inner class ToggleSongFavouriteStatus {
        @Test
        fun success_add_favourite_song() = runTest {
            repositoryShouldReturnFavouritesPlaylistById()
            val song = getMockSong(99L, false)

            val isFavourite = musicLibraryViewModel.toggleSongFavouriteStatus(song)

            // FIXME - Need to use a better solution for pausing the thread - also other tests already written that could benefit e.g. favourites tests?
            sleep(100)

            assertTrue(isFavourite)
            assertTrue(song.isFavourite)

            val playlistCaptor = argumentCaptor<List<Playlist>>()
            verify(repository).updatePlaylists(playlistCaptor.capture())
            val updatedIds = PlaylistHelper.extractSongIds(playlistCaptor.firstValue.first().songs)
            assertTrue(updatedIds.size > 1)
            assertTrue(updatedIds.contains(99L))

            val songCaptor = argumentCaptor<List<Song>>()
            verify(repository).updateSongs(songCaptor.capture())
            assertEquals(song.songId, songCaptor.firstValue.first().songId)
        }

        @Test
        fun success_remove_favourite_song() = runTest {
            repositoryShouldReturnFavouritesPlaylistById()
            val song = getMockSong(1L, true)

            val isFavourite = musicLibraryViewModel.toggleSongFavouriteStatus(song)

            // FIXME - Need to use a better solution for pausing the thread - also other tests already written that could benefit e.g. favourites tests?
            sleep(100)

            assertFalse(isFavourite)
            assertFalse(song.isFavourite)

            val playlistCaptor = argumentCaptor<List<Playlist>>()
            verify(repository).updatePlaylists(playlistCaptor.capture())
            val updatedIds = PlaylistHelper.extractSongIds(playlistCaptor.firstValue.first().songs)
            assertTrue(updatedIds.isEmpty())

            val songCaptor = argumentCaptor<List<Song>>()
            verify(repository).updateSongs(songCaptor.capture())
            assertEquals(song.songId, songCaptor.firstValue.first().songId)
        }

        @Test
        fun failure_playlist_not_found() = runTest {
            `when`(defaultPlaylistHelper.favourites).doReturn(Pair(1, "Favourites"))
            val songToFavourite = getMockSong(2L, false)

            assertThrows(PlaylistNotFoundException::class.java) {
                runBlocking {
                    musicLibraryViewModel.toggleSongFavouriteStatus(songToFavourite)
                }
            }

            verify(repository, never()).updateSongs(any())
            verify(repository, never()).updatePlaylists(any())
        }

        private suspend fun repositoryShouldReturnFavouritesPlaylistById() {
            `when`(defaultPlaylistHelper.favourites).doReturn(Pair(1, "Favourites"))
            val mockPlaylist = getMockFavouritesPlaylist()
            `when`(repository.getPlaylistById(defaultPlaylistHelper.favourites.first)).doReturn(mockPlaylist)
        }
    }

    @Nested
    @DisplayName("Find a playlist by name")
    inner class GetPlaylistByName {

        private val playlistA = "Playlist A"
        private val playlistB = "Playlist B"

        @Test
        fun getPlaylistByName_playlist_exists() = runTest {
            val mockPlaylist = mockGetPlaylistResponse(playlistA)

            val playlist = musicLibraryViewModel.getPlaylistByName(playlistA)

            assertEquals(mockPlaylist.toString(), playlist.toString())
        }

        @Test
        fun getPlaylistByName_playlist_does_not_exist() = runTest {
            val playlist = musicLibraryViewModel.getPlaylistByName(playlistB)

            assertNull(playlist)
        }
    }

    @Suppress("UNCHECKED_CAST")
    @Nested
    @DisplayName("Set the ID of the album being viewed")
    inner class SetActiveAlbumId {
        @Test
        fun setActiveAlbumId_success() {
            val activeAlbumIdField = ReflectionUtils.setFieldVisible(musicLibraryViewModel, "activeAlbumId")
            val activeAlbumId = activeAlbumIdField.get(musicLibraryViewModel) as MutableLiveData<String>
            assertNull(activeAlbumId.value)

            val expectedActiveAlbumId = "3"
            musicLibraryViewModel.setActiveAlbumId(expectedActiveAlbumId)

            assertEquals(expectedActiveAlbumId, activeAlbumId.value)
        }

        @Test
        fun setActiveAlbumId_empty_string_success() {
            val activeAlbumIdField = ReflectionUtils.setFieldVisible(musicLibraryViewModel, "activeAlbumId")
            val activeAlbumId = activeAlbumIdField.get(musicLibraryViewModel) as MutableLiveData<String>
            activeAlbumId.value = "2"

            assertEquals("2", activeAlbumId.value)

            musicLibraryViewModel.setActiveAlbumId("")

            assertEquals("", activeAlbumId.value)
        }
    }

    @Suppress("UNCHECKED_CAST")
    @Nested
    @DisplayName("Set the name of the artist being viewed")
    inner class SetActiveArtistName {
        @Test
        fun setActiveArtistName_success() {
            val activeArtistNameField = ReflectionUtils.setFieldVisible(musicLibraryViewModel, "activeArtistName")
            val activeArtistName = activeArtistNameField.get(musicLibraryViewModel) as MutableLiveData<String>
            assertNull(activeArtistName.value)

            val expectedActiveArtistName = "Band B"
            musicLibraryViewModel.setActiveArtistName(expectedActiveArtistName)

            assertEquals(expectedActiveArtistName, activeArtistName.value)
        }

        @Test
        fun setActiveArtistName_empty_string_success() {
            val activeArtistNameField = ReflectionUtils.setFieldVisible(musicLibraryViewModel, "activeArtistName")
            val activeArtistName = activeArtistNameField.get(musicLibraryViewModel) as MutableLiveData<String>
            activeArtistName.value = "Band A"

            assertEquals("Band A", activeArtistName.value)

            musicLibraryViewModel.setActiveArtistName("")

            assertEquals("", activeArtistName.value)
        }
    }

    @Nested
    @DisplayName("Extract playlist songs")
    inner class ExtractPlaylistSongs {
        @Test
        fun extractPlaylistSongs() = runTest {
            `when`(repository.getSongById(1L)).thenReturn(getMockSong(1L))
            `when`(repository.getSongById(2L)).thenReturn(getMockSong(2L))
            `when`(repository.getSongById(3L)).thenReturn(getMockSong(3L))

            val json = "[1,2,3]"

            val songs = musicLibraryViewModel.extractPlaylistSongs(json)

            assertEquals(3, songs.size)
            assertEquals(1L, songs[0].songId)
            assertEquals(2L, songs[1].songId)
            assertEquals(3L, songs[2].songId)
        }

        @Test
        fun extractPlaylistSongs_ignoreNullSongs() = runTest {
            `when`(repository.getSongById(1L)).thenReturn(getMockSong(1L))
            `when`(repository.getSongById(2L)).thenReturn(null)
            `when`(repository.getSongById(3L)).thenReturn(getMockSong(3L))

            val json = "[1,2,3]"

            val songs = musicLibraryViewModel.extractPlaylistSongs(json)

            assertEquals(2, songs.size)
            assertEquals(1L, songs[0].songId)
            assertEquals(3L, songs[1].songId)
        }

        @Test
        fun extractPlaylistSongs_emptyList() = runTest {
            val json = "[]"

            val songs = musicLibraryViewModel.extractPlaylistSongs(json)

            assertEquals(0, songs.size)
        }

        @Test
        fun extractPlaylistSongs_null() = runTest {
            val songs = musicLibraryViewModel.extractPlaylistSongs(null)

            assertEquals(0, songs.size)
        }
    }

    @Nested
    @DisplayName("Delete redundant artwork by song")
    inner class DeleteRedundantArtworkBySong {

        private val song = getMockSong()

        @Test
        fun deleteRedundantArtworkBySong_songDoesNotExist() = runTest {
            `when`(repository.getSongsByAlbumIdOrderByTrack(song.albumId)).thenReturn(listOf())

            mockkObject(ImageHandlingHelper)

            every { ImageHandlingHelper.deleteAlbumArtByResourceId(application, song.albumId) } just Runs

            val method = ReflectionUtils.setMethodVisible(musicLibraryViewModel, "deleteRedundantArtworkBySong")
            method.callSuspend(musicLibraryViewModel, song)

            Mockito.verify(repository).getSongsByAlbumIdOrderByTrack(song.albumId)

            io.mockk.verify { ImageHandlingHelper.deleteAlbumArtByResourceId(application, song.albumId) }

            unmockkObject(ImageHandlingHelper::class)
        }

        @Test
        fun deleteRedundantArtworkBySong_songExists() = runTest {
            `when`(repository.getSongsByAlbumIdOrderByTrack(song.albumId)).thenReturn(listOf(song))

            mockkObject(ImageHandlingHelper)

            every { ImageHandlingHelper.deleteAlbumArtByResourceId(application, song.albumId) } just Runs

            val method = ReflectionUtils.setMethodVisible(musicLibraryViewModel, "deleteRedundantArtworkBySong")
            method.callSuspend(musicLibraryViewModel, song)

            Mockito.verify(repository).getSongsByAlbumIdOrderByTrack(song.albumId)

            io.mockk.verify(exactly = 0) { ImageHandlingHelper.deleteAlbumArtByResourceId(application, song.albumId) }

            unmockkObject(ImageHandlingHelper::class)
        }
    }

    // todo do deleteSong next

    @Nested
    @DisplayName("Refresh the song of the day")
    inner class RefreshSongOfTheDay {
        @Test
        fun refreshSongOfTheDay_notLoadedForToday_success() = runTest {
            stubEditor()
            val mockPlaylist = mockSongOfTheDayPlaylistWithSong(null)

            refreshSongOfTheDay()

            assertEquals(2, PlaylistHelper.extractSongIds(mockPlaylist.songs).size)
            assertEquals(2L, PlaylistHelper.extractSongIds(mockPlaylist.songs)[0])
            assertEquals(1L, PlaylistHelper.extractSongIds(mockPlaylist.songs)[1])
            Mockito.verify(repository).updatePlaylists(listOf(mockPlaylist))
            Mockito.verify(editor).putString(SharedPreferencesConstants.SONG_OF_THE_DAY_LAST_UPDATED, today)
        }

        @Test
        fun refreshSongOfTheDay_alreadyLoadedForToday_success() = runTest {
            val mockPlaylist = mockSongOfTheDayPlaylist(today)

            refreshSongOfTheDay()

            assertEquals(1, PlaylistHelper.extractSongIds(mockPlaylist.songs).size)
            assertEquals(1L, PlaylistHelper.extractSongIds(mockPlaylist.songs)[0])
            Mockito.verify(repository, never()).updatePlaylists(any())
            Mockito.verify(editor, never()).putString(any(), any())
        }

        @Test
        fun refreshSongOfTheDay_forceUpdate_success() = runTest {
            val mockPlaylist = mockSongOfTheDayPlaylistWithSong(today)

            refreshSongOfTheDay(true)

            assertEquals(1, PlaylistHelper.extractSongIds(mockPlaylist.songs).size)
            assertEquals(2L, PlaylistHelper.extractSongIds(mockPlaylist.songs)[0])
            Mockito.verify(repository).updatePlaylists(listOf(mockPlaylist))
            Mockito.verify(editor, never()).putString(any(), any())
        }

        @Test
        fun refreshSongOfTheDay_30SongsLimitReached_success() = runTest {
            stubEditor()
            `when`(defaultPlaylistHelper.songOfTheDay).doReturn(Pair(3, "Song of the day"))
            val mockPlaylist = getMockSongOfTheDayPlaylist(30)
            `when`(repository.getPlaylistById(defaultPlaylistHelper.songOfTheDay.first)).doReturn(mockPlaylist)
            `when`(repository.getRandomSong()).doReturn(getMockSong(31L))
            `when`(sharedPreferences.getString(SharedPreferencesConstants.SONG_OF_THE_DAY_LAST_UPDATED, null))
                .doReturn(null)
            assertMaxLengthSongOfTheDayPlaylistElements(mockPlaylist, 1L, 30L)

            refreshSongOfTheDay()

            assertMaxLengthSongOfTheDayPlaylistElements(mockPlaylist, 31L, 29L)
            Mockito.verify(repository).updatePlaylists(listOf(mockPlaylist))
            Mockito.verify(editor).putString(SharedPreferencesConstants.SONG_OF_THE_DAY_LAST_UPDATED, today)
        }

        private fun assertMaxLengthSongOfTheDayPlaylistElements(playlist: Playlist,
                                                                expectedIdOfFirstElement: Long,
                                                                expectedIdOfLastElement: Long) {
            val extractedSongs = PlaylistHelper.extractSongIds(playlist.songs)
            assertEquals(30, extractedSongs.size)
            assertEquals(expectedIdOfFirstElement, extractedSongs[0])
            assertEquals(expectedIdOfLastElement, extractedSongs[extractedSongs.size - 1])
        }

        private suspend fun mockSongOfTheDayPlaylist(dateLastUpdated: String?) : Playlist{
            `when`(defaultPlaylistHelper.songOfTheDay).doReturn(Pair(3, "Song of the day"))
            val mockPlaylist = getMockSongOfTheDayPlaylist()
            `when`(repository.getPlaylistById(defaultPlaylistHelper.songOfTheDay.first)).doReturn(mockPlaylist)
            `when`(sharedPreferences.getString(SharedPreferencesConstants.SONG_OF_THE_DAY_LAST_UPDATED, null))
                .doReturn(dateLastUpdated)
            return mockPlaylist
        }

        private suspend fun mockSongOfTheDayPlaylistWithSong(dateLastUpdated: String?) : Playlist{
            `when`(repository.getRandomSong()).doReturn(getMockSong(2L))
            return mockSongOfTheDayPlaylist(dateLastUpdated)
        }

        private fun refreshSongOfTheDay(forceUpdate: Boolean = false) {
            musicLibraryViewModel.refreshSongOfTheDay(forceUpdate)
            // FIXME - Need to use a better solution for pausing the thread - also other tests already written that could benefit e.g. favourites tests?
            sleep(100)
        }
    }

    @Nested
    @DisplayName("Get all songs")
    inner class GetAllSongs {
        @Test
        fun getAllSongs_success() = runTest {
            val song = getMockSong()
            `when`(repository.getAllSongs()).doReturn(listOf(song))

            val songs = musicLibraryViewModel.getAllSongs()
            assertEquals(1, songs.size)
            assertEquals(song.songId, songs[0].songId)
        }
    }

    @Nested
    @DisplayName("Get all songs ordered by title")
    inner class GetAllSongsOrderBySongTitle {
        @Test
        fun getAllSongsOrderBySongTitle_success() = runTest {
            val song1 = getMockSong(1L)
            song1.title = "First title"
            val song2 = getMockSong(2L)
            song2.title = "Second title"
            val song3 = getMockSong(3L)
            song3.title = "Third title"
            `when`(repository.getAllSongsOrderByTitle()).doReturn(listOf(song1, song2, song3))

            val songs = musicLibraryViewModel.getAllSongsOrderByTitle()
            assertEquals(3, songs.size)
            assertEquals(song1.title, songs[0].title)
            assertEquals(song2.title, songs[1].title)
            assertEquals(song3.title, songs[2].title)
        }
    }

    @Nested
    @DisplayName("Get all playlists")
    inner class GetAllPlaylists {
        @Test
        fun getAllPlaylists_success() = runTest {
            val userPlaylist = getMockPlaylist()
            val defaultPlaylist = getMockFavouritesPlaylist()
            `when`(repository.getAllPlaylists()).doReturn(listOf(userPlaylist, defaultPlaylist))

            val playlists = musicLibraryViewModel.getAllPlaylists()
            assertEquals(2, playlists.size)
        }
    }

    @Nested
    @DisplayName("Get all user playlists")
    inner class GetAllUserPlaylists {
        @Test
        fun getAllUserPlaylists_success() = runTest {
            val userPlaylist = getMockPlaylist()
            `when`(repository.getAllUserPlaylists()).doReturn(listOf(userPlaylist))

            val playlists = musicLibraryViewModel.getAllUserPlaylists()
            assertEquals(1, playlists.size)
            playlists.forAll {
                !it.isDefault
            }
        }
    }

    @Nested
    @DisplayName("Get updated navigation argument")
    inner class GetUpdatedNavigationArgument {

        private val navigationArgument = "arg1"

        @Test
        fun getUpdatedNavigationArgument_success() {
            musicLibraryViewModel.navigationArgument = navigationArgument

            val argument = musicLibraryViewModel.getUpdatedNavigationArgument()

            assertEquals(navigationArgument, argument)
            assertEquals(null, musicLibraryViewModel.navigationArgument)
        }
    }

    private suspend fun mockGetPlaylistResponse(playlistName: String): Playlist {
        val mockPlaylist = getMockPlaylist()
        `when`(repository.getPlaylistByName(playlistName)).doReturn(mockPlaylist)
        return mockPlaylist
    }

    private fun stubEditor() {
        `when`(sharedPreferences.edit()).doReturn(editor)
        ReflectionUtils.replaceFieldWithMock(musicLibraryViewModel, "sharedPreferences", sharedPreferences)
    }
}
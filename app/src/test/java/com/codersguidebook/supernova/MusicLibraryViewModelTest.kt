package com.codersguidebook.supernova

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
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
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
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
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.reflect.full.callSuspend

@ExtendWith(MockKExtension::class, InstantTaskExecutorExtension::class)
class MusicLibraryViewModelTest {

    @RelaxedMockK
    lateinit var application: Application

    @RelaxedMockK
    lateinit var defaultPlaylistHelper: DefaultPlaylistHelper

    @RelaxedMockK
    lateinit var editor: SharedPreferences.Editor

    @RelaxedMockK
    lateinit var repository: MusicRepository

    @RelaxedMockK
    lateinit var sharedPreferences: SharedPreferences

    private lateinit var musicLibraryViewModel: MusicLibraryViewModel

    private val today = SimpleDateFormat.getDateInstance().format(Date())

    @BeforeEach
    fun setUp() {
        musicLibraryViewModel = MusicLibraryViewModel(application, repository, defaultPlaylistHelper)
    }

    @Nested
    @DisplayName("Toggle the song favourite status")
    inner class ToggleSongFavouriteStatus {
        @Test
        fun success_add_favourite_song() = runTest {
            repositoryShouldReturnFavouritesPlaylistById()
            val song = getMockSong(99L, false)

            val isFavourite = musicLibraryViewModel.toggleSongFavouriteStatus(song)

            val playlistSlot = slot<List<Playlist>>()
            coVerify(timeout = 1000L) { repository.updatePlaylists(capture(playlistSlot)) }
            val updatedIds = PlaylistHelper.extractSongIds(playlistSlot.captured.first().songs)
            assertTrue(updatedIds.size > 1)
            assertTrue(updatedIds.contains(99L))

            assertTrue(isFavourite)
            assertTrue(song.isFavourite)

            val songSlot = slot<List<Song>>()
            coVerify { repository.updateSongs(capture(songSlot)) }
            assertEquals(song.songId, songSlot.captured.first().songId)
        }

        @Test
        fun success_remove_favourite_song() = runTest {
            repositoryShouldReturnFavouritesPlaylistById()
            val song = getMockSong(1L, true)

            val isFavourite = musicLibraryViewModel.toggleSongFavouriteStatus(song)

            val playlistSlot = slot<List<Playlist>>()
            coVerify(timeout = 1000L) { repository.updatePlaylists(capture(playlistSlot)) }
            val updatedIds = PlaylistHelper.extractSongIds(playlistSlot.captured.first().songs)
            assertTrue(updatedIds.isEmpty())

            assertFalse(isFavourite)
            assertFalse(song.isFavourite)

            val songSlot = slot<List<Song>>()
            coVerify { repository.updateSongs(capture(songSlot)) }
            assertEquals(song.songId, songSlot.captured.first().songId)
        }

        @Test
        fun failure_playlist_not_found() = runTest {
            every { defaultPlaylistHelper.favourites } returns Pair(1, "Favourites")
            val songToFavourite = getMockSong(2L, false)
            coEvery { repository.getPlaylistById(any()) } returns null

            assertThrows(PlaylistNotFoundException::class.java) {
                runBlocking {
                    musicLibraryViewModel.toggleSongFavouriteStatus(songToFavourite)
                }
            }

            coVerify(exactly = 0) { repository.updateSongs(any()) }
            coVerify(exactly = 0) { repository.updatePlaylists(any()) }
        }

        private fun repositoryShouldReturnFavouritesPlaylistById() {
            every { defaultPlaylistHelper.favourites } returns Pair(1, "Favourites")
            val mockPlaylist = getMockFavouritesPlaylist()
            coEvery { repository.getPlaylistById(defaultPlaylistHelper.favourites.first) } returns mockPlaylist
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
            coEvery { repository.getPlaylistByName(any()) } returns null
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
            val activeAlbumId = activeAlbumIdField.getter.call(musicLibraryViewModel) as MutableLiveData<String>
            assertNull(activeAlbumId.value)

            val expectedActiveAlbumId = "3"
            musicLibraryViewModel.setActiveAlbumId(expectedActiveAlbumId)

            assertEquals(expectedActiveAlbumId, activeAlbumId.value)
        }

        @Test
        fun setActiveAlbumId_empty_string_success() {
            val activeAlbumIdField = ReflectionUtils.setFieldVisible(musicLibraryViewModel, "activeAlbumId")
            val activeAlbumId = activeAlbumIdField.getter.call(musicLibraryViewModel) as MutableLiveData<String>
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
            val activeArtistName = activeArtistNameField.getter.call(musicLibraryViewModel) as MutableLiveData<String>
            assertNull(activeArtistName.value)

            val expectedActiveArtistName = "Band B"
            musicLibraryViewModel.setActiveArtistName(expectedActiveArtistName)

            assertEquals(expectedActiveArtistName, activeArtistName.value)
        }

        @Test
        fun setActiveArtistName_empty_string_success() {
            val activeArtistNameField = ReflectionUtils.setFieldVisible(musicLibraryViewModel, "activeArtistName")
            val activeArtistName = activeArtistNameField.getter.call(musicLibraryViewModel) as MutableLiveData<String>
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
            coEvery { repository.getSongById(1L) } returns getMockSong(1L)
            coEvery { repository.getSongById(2L) } returns getMockSong(2L)
            coEvery { repository.getSongById(3L) } returns getMockSong(3L)

            val json = "[1,2,3]"

            val songs = musicLibraryViewModel.extractPlaylistSongs(json)

            assertEquals(3, songs.size)
            assertEquals(1L, songs[0].songId)
            assertEquals(2L, songs[1].songId)
            assertEquals(3L, songs[2].songId)
        }

        @Test
        fun extractPlaylistSongs_ignoreNullSongs() = runTest {
            coEvery { repository.getSongById(1L) } returns getMockSong(1L)
            coEvery { repository.getSongById(2L) } returns null
            coEvery { repository.getSongById(3L) } returns getMockSong(3L)

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
    @DisplayName("Save the playback progress of a song")
    inner class SavePlaybackProgress {
        @Test
        fun savePlaybackProgress() = runTest {
            val mediaId = 1L
            val playbackPosition = 1000

            musicLibraryViewModel.savePlaybackProgress(mediaId, playbackPosition)

            coVerify { repository.savePlaybackProgress(mediaId, playbackPosition) }
        }
    }

    @Nested
    @DisplayName("Delete redundant artwork by song")
    inner class DeleteRedundantArtworkBySong {

        private val song = getMockSong()

        @Test
        fun deleteRedundantArtworkBySong_songDoesNotExist() = runTest {
            coEvery { repository.getSongsByAlbumIdOrderByTrack(song.albumId) } returns listOf()

            mockkObject(ImageHandlingHelper)
            every { ImageHandlingHelper.deleteAlbumArtByResourceId(application, song.albumId) } just Runs

            val method = ReflectionUtils.setMethodVisible(musicLibraryViewModel, "deleteRedundantArtworkBySong")
            method.callSuspend(musicLibraryViewModel, song)

            coVerify { repository.getSongsByAlbumIdOrderByTrack(song.albumId) }

            verify { ImageHandlingHelper.deleteAlbumArtByResourceId(application, song.albumId) }

            unmockkObject(ImageHandlingHelper::class)
        }

        @Test
        fun deleteRedundantArtworkBySong_songExists() = runTest {
            coEvery { repository.getSongsByAlbumIdOrderByTrack(song.albumId) } returns listOf(song)

            mockkObject(ImageHandlingHelper)
            every { ImageHandlingHelper.deleteAlbumArtByResourceId(application, song.albumId) } just Runs

            val method = ReflectionUtils.setMethodVisible(musicLibraryViewModel, "deleteRedundantArtworkBySong")
            method.callSuspend(musicLibraryViewModel, song)

            coVerify { repository.getSongsByAlbumIdOrderByTrack(song.albumId) }

            verify(exactly = 0) { ImageHandlingHelper.deleteAlbumArtByResourceId(application, song.albumId) }

            unmockkObject(ImageHandlingHelper::class)
        }
    }

    @Nested
    @DisplayName("Delete a song")
    inner class DeleteSong {

        private val songToDelete = getMockSong()

        @Test
        fun deleteSong_songAppearsInPlaylistMultipleTimes() = runTest {
            setUpImageHandlingHelper()

            val mockPlaylist = getMockPlaylist(listOf(songToDelete, getMockSong(2L),
                songToDelete, getMockSong(3L)))
            coEvery { repository.getAllPlaylists() } answers { listOf(mockPlaylist) }

            musicLibraryViewModel.deleteSong(songToDelete)

            val mockPlaylistWithSongRemoved = getMockPlaylist(listOf(getMockSong(2L), getMockSong(3L)))
            coVerify(timeout = 1000L) { repository.updatePlaylists(listOf(mockPlaylistWithSongRemoved)) }
            coVerify { repository.deleteSong(songToDelete) }
            verify { ImageHandlingHelper.deleteAlbumArtByResourceId(application, songToDelete.albumId) }
            unmockkObject(ImageHandlingHelper::class)
        }

        @Test
        fun deleteSong_songNotInPlaylist() = runTest {
            setUpImageHandlingHelper()

            val mockPlaylist = getMockPlaylist(listOf(getMockSong(2L)))
            coEvery { repository.getAllPlaylists() } answers { listOf(mockPlaylist) }

            musicLibraryViewModel.deleteSong(songToDelete)

            coVerify(exactly = 0) { repository.updatePlaylists(any()) }
            coVerify { repository.deleteSong(songToDelete) }
            verify { ImageHandlingHelper.deleteAlbumArtByResourceId(application, songToDelete.albumId) }
            unmockkObject(ImageHandlingHelper::class)
        }

        @Test
        fun deleteSong_songAppearsInMultiplePlaylists() = runTest {
            setUpImageHandlingHelper()

            val mockPlaylist1 = getMockPlaylist(1, listOf(songToDelete, getMockSong(2L)))
            val mockPlaylist2 = getMockPlaylist(2, listOf(getMockSong(3L), songToDelete))
            coEvery { repository.getAllPlaylists() } answers { listOf(mockPlaylist1, mockPlaylist2) }

            musicLibraryViewModel.deleteSong(songToDelete)

            val mockPlaylistWithSongRemoved1 = getMockPlaylist(1, listOf(getMockSong(2L)))
            val mockPlaylistWithSongRemoved2 = getMockPlaylist(2, listOf(getMockSong(3L)))
            coVerify(timeout = 1000L) { repository.updatePlaylists(listOf(mockPlaylistWithSongRemoved1, mockPlaylistWithSongRemoved2)) }
            coVerify { repository.deleteSong(songToDelete) }
            verify { ImageHandlingHelper.deleteAlbumArtByResourceId(application, songToDelete.albumId) }
            unmockkObject(ImageHandlingHelper::class)
        }

        @Test
        fun deleteSong_songAppearsInOnePlaylistOnly() = runTest {
            setUpImageHandlingHelper()

            val mockPlaylist1 = getMockPlaylist(1, listOf(getMockSong(2L), getMockSong(3L)))
            val mockPlaylist2 = getMockPlaylist(2, listOf(getMockSong(4L), songToDelete))
            coEvery { repository.getAllPlaylists() } answers { listOf(mockPlaylist1, mockPlaylist2) }

            musicLibraryViewModel.deleteSong(songToDelete)

            val mockPlaylistWithSongRemoved = getMockPlaylist(2, listOf(getMockSong(4L)))
            coVerify(timeout = 1000L) { repository.updatePlaylists(listOf(mockPlaylistWithSongRemoved)) }
            coVerify { repository.deleteSong(songToDelete) }
            verify { ImageHandlingHelper.deleteAlbumArtByResourceId(application, songToDelete.albumId) }
            unmockkObject(ImageHandlingHelper::class)
        }

        private fun setUpImageHandlingHelper() {
            mockkObject(ImageHandlingHelper)
            every { ImageHandlingHelper.deleteAlbumArtByResourceId(application, songToDelete.albumId) } just Runs
        }
    }

    @Nested
    @DisplayName("Delete a playlist")
    inner class DeletePlaylist {

        private val playlistToDelete = getMockPlaylist()

        @Test
        fun deletePlaylist() = runTest {
            setUpImageHandlingHelper()

            musicLibraryViewModel.deletePlaylist(playlistToDelete)

            coVerify { repository.deletePlaylist(playlistToDelete) }
            verify { ImageHandlingHelper.deletePlaylistArtByResourceId(application,
                playlistToDelete.playlistId.toString()) }
            unmockkObject(ImageHandlingHelper::class)
        }

        private fun setUpImageHandlingHelper() {
            mockkObject(ImageHandlingHelper)
            every { ImageHandlingHelper.deletePlaylistArtByResourceId(application,
                playlistToDelete.playlistId.toString()) } just Runs
        }
    }

    @Nested
    @DisplayName("Save a list of songs")
    inner class SaveSongs {

        @Test
        fun saveSongs() = runTest {
            val songs = listOf(getMockSong(), getMockSong(2L))

            musicLibraryViewModel.saveSongs(songs)

            coVerify { repository.saveSongs(songs) }
        }
    }

    @Nested
    @DisplayName("Save a playlist")
    inner class SavePlaylist {

        @Test
        fun savePlaylist() = runTest {
            val playlist = getMockPlaylist()

            musicLibraryViewModel.savePlaylist(playlist)

            coVerify { repository.savePlaylist(playlist) }
        }
    }

    @Nested
    @DisplayName("Refresh the song of the day")
    inner class RefreshSongOfTheDay {
        @Test
        fun refreshSongOfTheDay_notLoadedForToday_success() = runTest {
            stubEditor()
            val mockPlaylist = mockSongOfTheDayPlaylistWithSong(null)

            musicLibraryViewModel.refreshSongOfTheDay(false)

            coVerify(timeout = 1000L) { repository.updatePlaylists(listOf(mockPlaylist)) }
            assertEquals(2, PlaylistHelper.extractSongIds(mockPlaylist.songs).size)
            assertEquals(2L, PlaylistHelper.extractSongIds(mockPlaylist.songs)[0])
            assertEquals(1L, PlaylistHelper.extractSongIds(mockPlaylist.songs)[1])
            verify { editor.putString(SharedPreferencesConstants.SONG_OF_THE_DAY_LAST_UPDATED, today) }
        }

        @Test
        fun refreshSongOfTheDay_alreadyLoadedForToday_success() = runTest {
            stubEditor()
            val mockPlaylist = mockSongOfTheDayPlaylist(today)

            musicLibraryViewModel.refreshSongOfTheDay(false)

            assertEquals(1, PlaylistHelper.extractSongIds(mockPlaylist.songs).size)
            assertEquals(1L, PlaylistHelper.extractSongIds(mockPlaylist.songs)[0])
            coVerify(exactly = 0) { repository.updatePlaylists(any()) }
            verify(exactly = 0) { editor.putString(any(), any()) }
        }

        @Test
        fun refreshSongOfTheDay_forceUpdate_success() = runTest {
            stubEditor()
            val mockPlaylist = mockSongOfTheDayPlaylistWithSong(today)

            musicLibraryViewModel.refreshSongOfTheDay(true)

            coVerify(timeout = 1000L) { repository.updatePlaylists(listOf(mockPlaylist)) }
            assertEquals(1, PlaylistHelper.extractSongIds(mockPlaylist.songs).size)
            assertEquals(2L, PlaylistHelper.extractSongIds(mockPlaylist.songs)[0])
            verify(exactly = 0) { editor.putString(any(), any()) }
        }

        @Test
        fun refreshSongOfTheDay_30SongsLimitReached_success() = runTest {
            stubEditor()
            every { defaultPlaylistHelper.songOfTheDay } returns Pair(3, "Song of the day")
            val mockPlaylist = getMockSongOfTheDayPlaylist(30)
            coEvery { repository.getPlaylistById(defaultPlaylistHelper.songOfTheDay.first) } returns mockPlaylist
            coEvery { repository.getRandomSong() } returns getMockSong(31L)
            every { sharedPreferences.getString(SharedPreferencesConstants.SONG_OF_THE_DAY_LAST_UPDATED, null) } returns null
            assertMaxLengthSongOfTheDayPlaylistElements(mockPlaylist, 1L, 30L)

            musicLibraryViewModel.refreshSongOfTheDay(false)

            coVerify(timeout = 1000L) { repository.updatePlaylists(listOf(mockPlaylist)) }
            assertMaxLengthSongOfTheDayPlaylistElements(mockPlaylist, 31L, 29L)
            verify { editor.putString(SharedPreferencesConstants.SONG_OF_THE_DAY_LAST_UPDATED, today) }
        }

        private fun assertMaxLengthSongOfTheDayPlaylistElements(playlist: Playlist,
                                                                expectedIdOfFirstElement: Long,
                                                                expectedIdOfLastElement: Long) {
            val extractedSongs = PlaylistHelper.extractSongIds(playlist.songs)
            assertEquals(30, extractedSongs.size)
            assertEquals(expectedIdOfFirstElement, extractedSongs[0])
            assertEquals(expectedIdOfLastElement, extractedSongs[extractedSongs.size - 1])
        }

        private fun mockSongOfTheDayPlaylist(dateLastUpdated: String?) : Playlist{
            every { defaultPlaylistHelper.songOfTheDay } returns Pair(3, "Song of the day")
            val mockPlaylist = getMockSongOfTheDayPlaylist()
            coEvery { repository.getPlaylistById(defaultPlaylistHelper.songOfTheDay.first) } returns mockPlaylist
            every { sharedPreferences.getString(SharedPreferencesConstants.SONG_OF_THE_DAY_LAST_UPDATED, null) } returns dateLastUpdated
            return mockPlaylist
        }

        private fun mockSongOfTheDayPlaylistWithSong(dateLastUpdated: String?) : Playlist{
            coEvery { repository.getRandomSong() } returns getMockSong(2L)
            return mockSongOfTheDayPlaylist(dateLastUpdated)
        }
    }

    @Nested
    @DisplayName("Get all songs")
    inner class GetAllSongs {
        @Test
        fun getAllSongs_success() = runTest {
            val song = getMockSong()
            coEvery { repository.getAllSongs() } returns listOf(song)

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
            coEvery { repository.getAllSongsOrderByTitle() } returns listOf(song1, song2, song3)

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
            coEvery { repository.getAllPlaylists() } returns listOf(userPlaylist, defaultPlaylist)

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
            coEvery { repository.getAllUserPlaylists() } returns listOf(userPlaylist)

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

    private fun mockGetPlaylistResponse(playlistName: String): Playlist {
        val mockPlaylist = getMockPlaylist()
        coEvery { repository.getPlaylistByName(playlistName) } returns mockPlaylist
        return mockPlaylist
    }

    private fun stubEditor() {
        every { sharedPreferences.edit() } returns editor
        ReflectionUtils.replaceFieldWithMock(musicLibraryViewModel, "sharedPreferences", sharedPreferences)
    }
}
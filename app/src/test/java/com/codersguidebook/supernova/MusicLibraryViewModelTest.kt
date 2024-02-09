package com.codersguidebook.supernova

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.codersguidebook.supernova.data.MusicRepository
import com.codersguidebook.supernova.entities.Playlist
import com.codersguidebook.supernova.testutils.ReflectionUtils
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.doReturn
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@Suppress("UNCHECKED_CAST")
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class MusicLibraryViewModelTest {

    private lateinit var musicLibraryViewModel: MusicLibraryViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        musicLibraryViewModel = MusicLibraryViewModel(RuntimeEnvironment.getApplication())
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

    private fun getMockPlaylist(): Playlist {
        return Playlist(1, "Playlist A", "1", false)
    }
}
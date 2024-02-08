package com.codersguidebook.supernova

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.codersguidebook.supernova.entities.Playlist
import com.codersguidebook.supernova.testutils.ReflectionUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockitoAnnotations
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
        // Given the album ID is set to 2
        val activeArtistNameField = ReflectionUtils.setFieldVisible(musicLibraryViewModel, "activeArtistName")
        val activeArtistName = activeArtistNameField.get(musicLibraryViewModel) as MutableLiveData<String>
        activeArtistName.value = "Band A"

        assertEquals("Band A", activeArtistName.value)

        // When setActiveAlbumId is called with an empty String
        musicLibraryViewModel.setActiveArtistName("")

        // Then the supplied String will be assigned to the activeArtistName field
        assertEquals("", activeArtistName.value)
    }


    /* = runTest {
        Mockito.`when`(repository.getAllPlaylists()).doReturn(getMockPlaylists())
        // assertTrue(musicLibraryViewModel.getAllPlaylists().isEmpty())
        assertEquals("test", musicLibraryViewModel.getAllPlaylists()[0].name)
    } */

    private fun getMockPlaylists(): List<Playlist> {
        val playlist = Playlist(1, "test", "1", false)
        return listOf(playlist)
        // return listOf()
    }
}
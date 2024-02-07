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
        val activeAlbumId = (activeAlbumIdField.get(musicLibraryViewModel) as MutableLiveData<*>).value as String?
        assertNull(activeAlbumId)
        
        // When setActiveAlbumId is called with a valid String
        val expectedActiveAlbumId = "3"
        musicLibraryViewModel.setActiveAlbumId(expectedActiveAlbumId)

        // Then the supplied String will be assigned to the activeAlbumId field
        val actualActiveAlbumId = (activeAlbumIdField.get(musicLibraryViewModel) as MutableLiveData<*>).value as String?
        assertEquals(expectedActiveAlbumId, actualActiveAlbumId)
    }

    @Test
    fun setActiveAlbumId_empty_string_success() {
        // Given the album ID is set to 2
        val activeAlbumIdField = ReflectionUtils.setFieldVisible(musicLibraryViewModel, "activeAlbumId")
        @SuppressWarnings("UNCHECKED_CAST")
        (activeAlbumIdField.get(musicLibraryViewModel) as MutableLiveData<String>).value = "2"

        val initialActiveAlbumId = (activeAlbumIdField.get(musicLibraryViewModel) as MutableLiveData<*>).value as String?
        assertEquals("2", initialActiveAlbumId)

        // When setActiveAlbumId is called with an empty String
        musicLibraryViewModel.setActiveAlbumId("")

        // Then the supplied String will be assigned to the activeAlbumId field
        val actualActiveAlbumId = (activeAlbumIdField.get(musicLibraryViewModel) as MutableLiveData<*>).value as String?
        assertEquals("", actualActiveAlbumId)


        // TODO: RESUME
        //  Write further tests for setActiveAlbumId
        //  Also a test if the new value is the same value - see if there's a way to preset the value using reflection
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
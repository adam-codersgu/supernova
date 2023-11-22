package com.codersguidebook.supernova

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.codersguidebook.supernova.entities.Playlist
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
        val activeAlbumIdField = musicLibraryViewModel.javaClass.getDeclaredField("activeAlbumId")
        activeAlbumIdField.isAccessible = true
        val activeAlbumId = (activeAlbumIdField.get(musicLibraryViewModel) as MutableLiveData<*>).value as String?
        assertNull(activeAlbumId)

        // When setActiveAlbumId is called with a valid String
        val expectedActiveAlbumId = "3"
        musicLibraryViewModel.setActiveAlbumId(expectedActiveAlbumId)

        // Then the supplied String will be assigned to the activeAlbumId field
        val actualActiveAlbumId = (activeAlbumIdField.get(musicLibraryViewModel) as MutableLiveData<*>).value as String?
        assertEquals(expectedActiveAlbumId, actualActiveAlbumId)


        // TODO: RESUME
        //  Assign some of the above reflection steps to test helper methods?
        //  Write further tests for setActiveAlbumId e.g. play around with null values?
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
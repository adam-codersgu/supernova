package com.codersguidebook.supernova

import android.app.Application
import com.codersguidebook.supernova.data.MusicRepository
import com.codersguidebook.supernova.entities.Playlist
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.doReturn
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config


@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class MusicLibraryViewModelTest {

    @Mock
    private val repository: MusicRepository = Mockito.mock(MusicRepository::class.java)

    private lateinit var musicLibraryViewModel: MusicLibraryViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        musicLibraryViewModel = MusicLibraryViewModel(RuntimeEnvironment.getApplication())
        musicLibraryViewModel.repository = repository
    }

    @Test
    fun getAllPlaylists_success_emptyList() = runTest {
        Mockito.`when`(repository.getAllPlaylists()).doReturn(getMockPlaylists())
        // assertTrue(musicLibraryViewModel.getAllPlaylists().isEmpty())
        assertEquals("test", musicLibraryViewModel.getAllPlaylists()[0].name)
    }

    private fun getMockPlaylists(): List<Playlist> {
        val playlist = Playlist(1, "test", "1", false)
        return listOf(playlist)
        // return listOf()
    }
}
package com.codersguidebook.supernova.ui.album

import android.os.Bundle
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.MutableLiveData
import com.codersguidebook.supernova.MusicLibraryViewModel
import com.codersguidebook.supernova.entities.Song
import com.codersguidebook.supernova.fixture.PlaylistFixture.getMockSong
import com.codersguidebook.supernova.testutils.ReflectionUtils
import com.codersguidebook.supernova.utils.ImageHandlingHelper
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.verify
import io.mockk.unmockkObject
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import tech.apter.junit.jupiter.robolectric.RobolectricExtension
import kotlin.reflect.KMutableProperty1

@ExtendWith(RobolectricExtension::class)
class EditAlbumFragmentTest {

    companion object {
        const val ALBUM_ID = "album-123"
    }

    private lateinit var musicLibraryViewModel: MusicLibraryViewModel
    private lateinit var activeAlbumSongsLiveData: MutableLiveData<List<Song>>

    @BeforeEach
    fun setUp() {
        musicLibraryViewModel = mockk(relaxed = true)
        activeAlbumSongsLiveData = MutableLiveData()

        every { musicLibraryViewModel.setActiveAlbumId(any()) } just runs
        every { musicLibraryViewModel.activeAlbumSongs } returns activeAlbumSongsLiveData

        mockkObject(MusicLibraryViewModel.Factory)
        every {
            MusicLibraryViewModel.Factory.create(MusicLibraryViewModel::class.java)
        } returns musicLibraryViewModel
        every {
            MusicLibraryViewModel.Factory.create(MusicLibraryViewModel::class.java, any())
        } returns musicLibraryViewModel

        mockkObject(ImageHandlingHelper)
        every {
            ImageHandlingHelper.loadImageByAlbumId(any(), any(), any())
        } just runs
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(MusicLibraryViewModel.Factory)
        unmockkObject(ImageHandlingHelper)
    }

    /**
     * TODO
     * RESUME
     *  START ABSTRACTING MAINACTIVITY METHODS USED BY FRAGMENTS INTO AN INTERFACE AND USE THAT IN BASEFRAGMENT
     *  E.G. activity as? MusicHost
     *  Don’t touch viewLifecycleOwner inside onCreateView:
     *  Move setupMenu() into onViewCreated(view, savedInstanceState) (which is the standard Fragment lifecycle hook for view/menu setup once view is guaranteed non-null), or pass a MenuHost / helper in so onCreateView only inflates the binding and reads arguments.
     *
     */
    @Suppress("UNCHECKED_CAST")
    @Nested
    inner class OnCreateView {

        /**
         * TODO
         *  A TEST WITH NO FRAGMENT ARGS
         */

        @Test
        fun onCreateView_fragmentArgs() {
            val fragmentArgs = Bundle()
            fragmentArgs.putString("albumId", ALBUM_ID)

            val scenario = launchFragmentInContainer<EditAlbumFragment>(
                fragmentArgs = fragmentArgs,
            )

            scenario.onFragment { fragment ->
                assertNotNull(fragment._binding)
                assertNotNull(fragment.binding)
                val albumIdField = ReflectionUtils.setFieldVisible(fragment, "albumId") as KMutableProperty1<Any, String>
                assertEquals(ALBUM_ID, albumIdField.get(fragment))
            }
        }
    }

    @Nested
    inner class OnViewCreated {

        @Test
        fun onViewCreated() {
            // FIXME - OVERRIDE ALBUM ID
            val songs = listOf(
                getMockSong(1L),
                getMockSong(2L)
            )

            val fragmentArgs = Bundle().apply {
                putString("albumId", "1")
            }

            val scenario = launchFragmentInContainer<EditAlbumFragment>(
                fragmentArgs = fragmentArgs
            )

            scenario.onFragment { fragment ->
                verify { musicLibraryViewModel.setActiveAlbumId("1") }

                activeAlbumSongsLiveData.value = songs

                assertEquals("Album", fragment.binding.editAlbumTitle.text.toString())
                assertEquals("2024", fragment.binding.editAlbumYear.text.toString())
                assertEquals("1", fragment.binding.editAlbumId.text.toString())
                assertFalse(fragment.binding.rememberPlaybackProgress.isChecked)

                verify {
                    ImageHandlingHelper.loadImageByAlbumId(
                        fragment.requireActivity().application,
                        "1",
                        fragment.binding.editAlbumArtwork
                    )
                }
            }
        }

        /**
         * TODO
         *  REMEMBER PLAYBACK PROGRESS TRUE FOR ALL SONGS
         *  ONLY SOME SONGS IN THE ALBUM HAVE REMEMBER PLAYBACK PROGRESS TRUE
         *  SONGS USED FOR DISPLAY IS EMPTY
         */
    }
}
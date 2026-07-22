package com.codersguidebook.supernova

import android.app.Application
import android.content.ContentResolver
import android.content.ContentUris
import android.content.SharedPreferences
import android.database.Cursor
import android.graphics.Bitmap
import android.provider.MediaStore
import android.util.Size
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import com.codersguidebook.supernova.entities.Song
import com.codersguidebook.supernova.fixture.PlayQueueFixture.getMediaItem
import com.codersguidebook.supernova.fixture.PlayQueueFixture.getPlayQueue
import com.codersguidebook.supernova.fixture.PlaylistFixture.getMockSongs
import com.codersguidebook.supernova.params.MediaServiceConstants.Companion.ORDER_ID
import com.codersguidebook.supernova.params.SharedPreferencesConstants.Companion.CURRENT_QUEUE_ITEM_INDEX
import com.codersguidebook.supernova.params.SharedPreferencesConstants.Companion.SHUFFLE_MODE
import com.codersguidebook.supernova.testutils.DispatcherUtils.resetDispatchers
import com.codersguidebook.supernova.testutils.DispatcherUtils.stubIODispatcher
import com.codersguidebook.supernova.testutils.InstantTaskExecutorExtension
import com.codersguidebook.supernova.testutils.ReflectionUtils
import com.codersguidebook.supernova.utils.DefaultPlaylistHelper
import com.codersguidebook.supernova.utils.ImageHandlingHelper
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.MoreExecutors
import io.kotest.matchers.ints.exactly
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.robolectric.Robolectric
import tech.apter.junit.jupiter.robolectric.RobolectricExtension
import java.lang.reflect.Method

@ExtendWith(MockKExtension::class, RobolectricExtension::class, InstantTaskExecutorExtension::class)
@OptIn(ExperimentalCoroutinesApi::class)
class MainActivityTest {

    @RelaxedMockK
    lateinit var application: Application

    @RelaxedMockK
    lateinit var controller: MediaController

    @RelaxedMockK
    lateinit var defaultPlaylistHelper: DefaultPlaylistHelper

    @RelaxedMockK
    lateinit var editor: SharedPreferences.Editor

    @RelaxedMockK
    lateinit var playQueueViewModel: PlayQueueViewModel

    @RelaxedMockK
    lateinit var mockLiveData: MutableLiveData<Int>

    @RelaxedMockK
    lateinit var musicLibraryViewModel: MusicLibraryViewModel

    @RelaxedMockK
    lateinit var sharedPreferences: SharedPreferences

    private lateinit var mainActivity: MainActivity

    @BeforeEach
    fun setUp() {

        clearAllMocks()

        mockkConstructor(ViewModelProvider::class)

        every {
            anyConstructed<ViewModelProvider>()[MusicLibraryViewModel::class.java]
        } returns musicLibraryViewModel

        every {
            anyConstructed<ViewModelProvider>()[PlayQueueViewModel::class.java]
        } returns playQueueViewModel

        // 4. Stub any internal LiveData dependencies that onCreate / fragments observe
        // Example (repeat for any fields that your activity registers observers on):
        // every { musicLibraryViewModel.someLiveDataField } returns mockLiveData

        val controllerActivity = Robolectric.buildActivity(MainActivity::class.java)
        mainActivity = controllerActivity.get()
        controllerActivity.create()

        val immediateFuture = Futures.immediateFuture(controller)

        ReflectionUtils.replaceFieldWithMock(mainActivity, "controllerFuture", immediateFuture)

        immediateFuture.addListener({
            ReflectionUtils.replaceFieldWithMock(mainActivity, "controller", immediateFuture.get())

            // If your MainActivity has an initController() method, invoke it here via reflection if private:
            /* val initMethod = MainActivity::class.java.getDeclaredMethod("initController")
                initMethod.isAccessible = true
                initMethod.invoke(mainActivity) */
        }, MoreExecutors.directExecutor())

        controllerActivity.start()
    }

    @Nested
    @DisplayName("Commence/resume playback")
    inner class Play {

        @Test
        fun play() {
            mainActivity.play()

            verify { controller.play() }
        }
    }

    @Nested
    @DisplayName("Fast forward playback")
    inner class FastForward {

        @Test
        fun fastForward() {
            mainActivity.fastForward()

            verify { controller.seekForward() }
        }
    }

    @Nested
    @DisplayName("Shuffle or unshuffle the play queue")
    inner class SetShuffleMode {

        @Test
        fun setShuffleMode_true() {
            stubEditor()
            val method = setMethodVisibleForInvoke(mainActivity)
            val playQueue = getPlayQueue(5)

            every { playQueueViewModel.playQueue.value } returns playQueue
            every { playQueueViewModel.currentQueueItemIndex.value } returns 1
            stubPlayQueueViewModel()

            method.invoke(mainActivity, true)

            verify { playQueueViewModel.currentQueueItemIndex.postValue(0) }
            verify { editor.putInt(CURRENT_QUEUE_ITEM_INDEX, 0) }
            val playQueueSlot = slot<List<MediaItem>>()
            verify { playQueueViewModel.playQueue.value = capture(playQueueSlot) }
            assertEquals(playQueue[1], playQueueSlot.captured.first())
            verify { editor.putBoolean(SHUFFLE_MODE, true) }
        }

        @Test
        fun setShuffleMode_false() {
            stubEditor()
            val method = setMethodVisibleForInvoke(mainActivity)
            val playQueue = getPlayQueue(5)
            playQueue[0].mediaMetadata.extras?.putInt(ORDER_ID, 1)
            playQueue[1].mediaMetadata.extras?.putInt(ORDER_ID, 2)
            playQueue[2].mediaMetadata.extras?.putInt(ORDER_ID, 4)
            playQueue[3].mediaMetadata.extras?.putInt(ORDER_ID, 3)
            playQueue[4].mediaMetadata.extras?.putInt(ORDER_ID, 0)

            every { playQueueViewModel.playQueue.value } returns playQueue
            every { playQueueViewModel.currentQueueItemIndex.value } returns 1
            stubPlayQueueViewModel()

            method.invoke(mainActivity, false)

            verify { playQueueViewModel.currentQueueItemIndex.postValue(2) }
            verify { editor.putInt(CURRENT_QUEUE_ITEM_INDEX, 2) }
            val playQueueSlot = slot<List<MediaItem>>()
            verify { playQueueViewModel.playQueue.value = capture(playQueueSlot) }
            assertEquals(playQueue[4], playQueueSlot.captured.first())
            assertEquals(playQueue[2], playQueueSlot.captured.last())
            verify { editor.putBoolean(SHUFFLE_MODE, false) }
        }

        @Test
        fun setShuffleMode_playQueueEmpty() {
            stubEditor()
            val method = setMethodVisibleForInvoke(mainActivity)

            every { playQueueViewModel.playQueue.value } returns listOf()
            stubPlayQueueViewModel()

            method.invoke(mainActivity, false)

            verify(exactly = 0) { playQueueViewModel.currentQueueItemIndex.postValue(any()) }
            confirmVerified(editor)
        }

        private fun setMethodVisibleForInvoke(targetObject: Any): Method {
            val targetMethod = targetObject.javaClass.getDeclaredMethod("setShuffleMode", Boolean::class.java)
            targetMethod.isAccessible = true
            return targetMethod
        }
    }

    @Nested
    @DisplayName("Update a list of songs")
    inner class UpdateSongs {

        @Test
        fun updateSongs_noMatchingSongsInPlayQueue() {
            val playQueue = listOf(getMediaItem("999"))
            every { playQueueViewModel.playQueue.value } returns playQueue
            stubPlayQueueViewModel()

            val songs = getMockSongs(5)

            mainActivity.updateSongs(songs)

            verify { musicLibraryViewModel.updateSongs(songs) }
            verify(exactly = 0) { controller.replaceMediaItem(any(), any()) }
            verify { playQueueViewModel.playQueue.value }
            confirmVerified(playQueueViewModel)
        }

        @Test
        fun updateSongs_matchingSongsInPlayQueue() {
            val songs = getMockSongs(5)
            val playQueue = mutableListOf(
                getMediaItem("777"),
                getMediaItem(songs[2].songId.toString()),
                getMediaItem(songs[4].songId.toString()),
                getMediaItem("888"),
                getMediaItem("999")
            )
            every { playQueueViewModel.playQueue.value } returns playQueue
            every { playQueueViewModel.currentQueueItemIndex.value } returns 3
            stubPlayQueueViewModel()

            every { sharedPreferences.getBoolean(SHUFFLE_MODE, false) } returns false

            mainActivity.updateSongs(songs)

            verify { musicLibraryViewModel.updateSongs(songs) }
            confirmVerified(controller)
            playQueue[1] = songs[2].getMediaItem(songs[2].songId.toInt())
            playQueue[2] = songs[4].getMediaItem(songs[4].songId.toInt())
            verify { playQueueViewModel.playQueue.value = playQueue }
        }

        @Test
        fun updateSongs_matchingSongsInShuffledPlayQueue() {
            val songs = getMockSongs(5)
            val playQueue = mutableListOf(
                getMediaItem("777", 777),
                getMediaItem(songs[2].songId.toString(), songs[2].songId.toInt()),
                getMediaItem(songs[4].songId.toString(), songs[4].songId.toInt()),
                getMediaItem("888", 888),
                getMediaItem("999", 999)
            )
            every { playQueueViewModel.playQueue.value } returns playQueue
            every { playQueueViewModel.currentQueueItemIndex.value } returns 1
            stubPlayQueueViewModel()

            every { sharedPreferences.getBoolean(SHUFFLE_MODE, false) } returns true

            mainActivity.updateSongs(songs)

            verify { musicLibraryViewModel.updateSongs(songs) }
            verify { controller.replaceMediaItem(0, songs[2].getMediaItem(songs[2].songId.toInt())) }
            confirmVerified(controller)
            playQueue[1] = songs[2].getMediaItem(songs[2].songId.toInt())
            playQueue[2] = songs[4].getMediaItem(songs[4].songId.toInt())
            verify { playQueueViewModel.playQueue.value = playQueue }
        }
    }

    @Nested
    @DisplayName("Extract song metadata from a cursor")
    inner class CreateSongFromCursor {

        private val albumId = "434356556"
        private val songId = 11L

        @Test
        fun createSongFromCursor() {
            val cursor = getMockCursor()

            val method = setMethodVisibleForInvoke(mainActivity)
            mockkObject(ImageHandlingHelper)

            val song = method.invoke(mainActivity, cursor) as Song

            assertEquals(songId, song.songId)
            assertEquals(albumId, song.albumId)
        }

        @Test
        fun createSongFromCursor_artworkNotFound() {
            val cursor = getMockCursor()

            val spyActivity = spyk(mainActivity)
            val mockContentResolver = mockk<ContentResolver>(relaxed = true)
            every { spyActivity.contentResolver } returns mockContentResolver

            val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songId)
            val mockBitmap = mockk<Bitmap>()
            every {
                mockContentResolver.loadThumbnail(uri, Size(640, 640), null)
            } returns mockBitmap

            val method = setMethodVisibleForInvoke(spyActivity)
            mockkObject(ImageHandlingHelper)
            every { ImageHandlingHelper.doesAlbumArtExistByResourceId(application, albumId) } returns false
            every { ImageHandlingHelper.saveAlbumArtByResourceId(any(), any(), any()) } just Runs

            method.invoke(spyActivity, cursor)

            verify { mockContentResolver.loadThumbnail(uri, Size(640, 640), null) }
            verify { ImageHandlingHelper.saveAlbumArtByResourceId(any(), albumId, mockBitmap) }
        }

        @ParameterizedTest
        @CsvSource("1, 1001", "12, 1012", "123, 1123", "4321, 4321", "43/21, 1001")
        fun createSongFromCursor_trackString(cursorTrack: String, songTrack: Int) {
            val cursor = getMockCursor()
            every { cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK) } returns 2
            every { cursor.getString(2) } returns cursorTrack

            val method = setMethodVisibleForInvoke(mainActivity)
            mockkObject(ImageHandlingHelper)

            val song = method.invoke(mainActivity, cursor) as Song

            assertEquals(songTrack, song.track)
        }

        private fun getMockCursor(): Cursor {
            val cursor = mockk<Cursor>(relaxed = true)
            every { cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID) } returns 0
            every { cursor.getLong(0) } returns songId
            every { cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID) } returns 1
            every { cursor.getString(1) } returns albumId
            return cursor
        }

        private fun setMethodVisibleForInvoke(targetObject: Any): Method {
            val targetMethod = targetObject.javaClass.getDeclaredMethod("createSongFromCursor", Cursor::class.java)
            targetMethod.isAccessible = true
            return targetMethod
        }
    }

    @Test
    fun saveCurrentlyPlayingIndex() = runTest {
        stubIODispatcher(testScheduler)

        try {
            every { playQueueViewModel.currentQueueItemIndex } returns mockLiveData

            stubPlayQueueViewModel()
            stubEditor()

            val index = 2

            val method = ReflectionUtils
                .setMethodVisibleForInvokeIntParam(mainActivity, "saveCurrentlyPlayingIndex")
            method.invoke(mainActivity, index)

            advanceUntilIdle()

            verify { mockLiveData.postValue(index) }
            verify { editor.putInt(CURRENT_QUEUE_ITEM_INDEX, index) }
        } finally {
            resetDispatchers()
        }
    }

    private fun stubEditor() {
        every { sharedPreferences.edit() } returns editor
        ReflectionUtils.replaceFieldWithMock(mainActivity, "sharedPreferences", sharedPreferences)
    }

    private fun stubPlayQueueViewModel() {
        val lazyMock = lazy { playQueueViewModel }
        ReflectionUtils.replaceFieldWithMock(mainActivity, "playQueueViewModel", lazyMock)
    }
}
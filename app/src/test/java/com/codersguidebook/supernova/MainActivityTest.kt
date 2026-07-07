package com.codersguidebook.supernova

import android.app.Application
import android.content.SharedPreferences
import android.database.Cursor
import android.provider.MediaStore
import androidx.lifecycle.MutableLiveData
import androidx.media3.session.MediaController
import com.codersguidebook.supernova.entities.Song
import com.codersguidebook.supernova.params.SharedPreferencesConstants
import com.codersguidebook.supernova.testutils.DispatcherUtils.resetDispatchers
import com.codersguidebook.supernova.testutils.DispatcherUtils.stubIODispatcher
import com.codersguidebook.supernova.testutils.ReflectionUtils
import com.codersguidebook.supernova.utils.DefaultPlaylistHelper
import com.codersguidebook.supernova.utils.ImageHandlingHelper
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.MoreExecutors
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.extension.ExtendWith
import org.robolectric.Robolectric
import tech.apter.junit.jupiter.robolectric.RobolectricExtension
import java.lang.reflect.Method

@ExtendWith(MockKExtension::class, RobolectricExtension::class)
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
    @DisplayName("Extract song metadata from a cursor")
    inner class CreateSongFromCursor {

        @Test
        fun createSongFromCursor() {
            val cursor = mockk<Cursor>(relaxed = true)
            every { cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID) } returns 0
            every { cursor.getLong(0) } returns 11L

            val method = setMethodVisibleForInvoke(mainActivity)
            mockkObject(ImageHandlingHelper)

            val song = method.invoke(mainActivity, cursor) as Song

            assertEquals(11L, song.songId)
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
            verify { editor.putInt(SharedPreferencesConstants.CURRENT_QUEUE_ITEM_INDEX, index) }
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
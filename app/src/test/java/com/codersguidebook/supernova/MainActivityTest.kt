package com.codersguidebook.supernova

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import androidx.media3.session.MediaController
import com.codersguidebook.supernova.params.SharedPreferencesConstants
import com.codersguidebook.supernova.testutils.ReflectionUtils
import com.codersguidebook.supernova.utils.DefaultPlaylistHelper
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.MoreExecutors
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.robolectric.Robolectric
import tech.apter.junit.jupiter.robolectric.RobolectricExtension

@ExtendWith(MockKExtension::class, RobolectricExtension::class)
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
    lateinit var musicLibraryViewModel: MusicLibraryViewModel

    @RelaxedMockK
    lateinit var sharedPreferences: SharedPreferences

    private lateinit var mainActivity: MainActivity
    private val mockLiveData = mockk<MutableLiveData<Int>>(relaxed = true)

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

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun saveCurrentlyPlayingIndex() = runTest {
        // TODO - TIDY AND ABSTRACT OUT THIS CODE
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        mockkStatic(Dispatchers::class)
        every { Dispatchers.IO } returns testDispatcher

        try {
            every { playQueueViewModel.currentQueueItemIndex } returns mockLiveData

            val lazyMock = lazy { playQueueViewModel }
            ReflectionUtils.replaceFieldWithMock(mainActivity, "playQueueViewModel", lazyMock)

            stubEditor()

            val index = 2

            val method = mainActivity.javaClass.getDeclaredMethod("saveCurrentlyPlayingIndex", Int::class.java)
            method.isAccessible = true
            method.invoke(mainActivity, index)

            advanceUntilIdle()

            verify { mockLiveData.postValue(index) }
            verify { editor.putInt(SharedPreferencesConstants.CURRENT_QUEUE_ITEM_INDEX, index) }

        } finally {
            Dispatchers.resetMain()
            unmockkStatic(Dispatchers::class)
        }
    }

    private fun stubEditor() {
        every { sharedPreferences.edit() } returns editor
        ReflectionUtils.replaceFieldWithMock(mainActivity, "sharedPreferences", sharedPreferences)
    }
}
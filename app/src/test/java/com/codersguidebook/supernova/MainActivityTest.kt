package com.codersguidebook.supernova

import android.app.Application
import android.content.SharedPreferences
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.codersguidebook.supernova.utils.DefaultPlaylistHelper
import com.google.common.util.concurrent.Futures
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.robolectric.Robolectric
import org.robolectric.annotation.Config
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
    lateinit var musicLibraryViewModel: MusicLibraryViewModel

    @RelaxedMockK
    lateinit var sharedPreferences: SharedPreferences

    private lateinit var mainActivity: MainActivity

    @BeforeEach
    fun setUp() {
        // 1. Tell MockK to look at the Builder inner class inside MediaController
        mockkStatic(MediaController.Builder::class)

        // 2. Create a fake Builder mock
        val mockBuilder = mockk<MediaController.Builder>(relaxed = true)

        // 3. Make MediaController.Builder(any(), any()) return our fake Builder mock
        every {
            MediaController.Builder(any(), any<SessionToken>())
        } returns mockBuilder

        // 4. Wrap your @RelaxedMockK controller into an immediately successful future
        val immediateFuture = Futures.immediateFuture(controller)

        // 5. Make the fake Builder return our completed future when buildAsync() runs
        every { mockBuilder.buildAsync() } returns immediateFuture

        // 6. Now safely drive the lifecycle through Robolectric
        val controllerActivity = Robolectric.buildActivity(MainActivity::class.java)
        mainActivity = controllerActivity.get()

        controllerActivity.create().start()
    }

    @AfterEach
    fun tearDown() {
        // Always clear static mocks
        unmockkStatic(MediaController.Builder::class)
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
}
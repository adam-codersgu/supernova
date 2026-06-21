package com.codersguidebook.supernova

import android.app.Application
import android.content.SharedPreferences
import androidx.media3.session.MediaController
import com.codersguidebook.supernova.testutils.ReflectionUtils
import com.codersguidebook.supernova.utils.DefaultPlaylistHelper
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.MoreExecutors
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
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
}
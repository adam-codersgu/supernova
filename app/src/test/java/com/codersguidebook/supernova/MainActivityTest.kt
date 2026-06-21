package com.codersguidebook.supernova

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
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
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.robolectric.Robolectric
import org.robolectric.annotation.Config
import tech.apter.junit.jupiter.robolectric.RobolectricExtension
import java.lang.reflect.Field

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
        // 1. Initialize the activity instance via Robolectric (triggers onCreate)
        val controllerActivity = Robolectric.buildActivity(MainActivity::class.java)
        mainActivity = controllerActivity.get()
        controllerActivity.create()

        // 2. Wrap your @RelaxedMockK controller inside an immediate future
        val immediateFuture = Futures.immediateFuture(controller)

        // 3. Inject the future using Reflection to bypass MediaController.Builder completely
        /* val futureField: Field = MainActivity::class.java.getDeclaredField("controllerFuture")
        futureField.isAccessible = true
        futureField.set(mainActivity, immediateFuture) */
        ReflectionUtils.replaceFieldWithMock(mainActivity, "controllerFuture", immediateFuture)

        // 4. Manually trigger the listener logic found inside your onStart()
        // to simulate the asynchronous load completing instantly
        immediateFuture.addListener({
            // This mirrors exactly what production code expects onStart to finish with:
            val fieldController = MainActivity::class.java.getDeclaredField("controller")
            fieldController.isAccessible = true
            fieldController.set(mainActivity, immediateFuture.get())

            // If your MainActivity has an initController() method, invoke it here via reflection if private:
            try {
                val initMethod = MainActivity::class.java.getDeclaredMethod("initController")
                initMethod.isAccessible = true
                initMethod.invoke(mainActivity)
            } catch (e: NoSuchMethodException) {
                // Skip if initController doesn't exist or is named differently
            }
        }, MoreExecutors.directExecutor())

        // 5. Fire onStart() safely. Since the fields are populated, it won't crash
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
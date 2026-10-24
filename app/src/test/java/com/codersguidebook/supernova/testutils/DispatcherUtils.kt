package com.codersguidebook.supernova.testutils

import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
object DispatcherUtils {

    fun stubDispatchers(testScheduler: TestCoroutineScheduler) {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        val mainTestDispatcher = Dispatchers.Main
        mockkStatic(Dispatchers::class)
        every { Dispatchers.IO } returns testDispatcher
        every { Dispatchers.Main } returns mainTestDispatcher
        every { Dispatchers.Default } returns testDispatcher
    }

    fun resetDispatchers() {
        Dispatchers.resetMain()
        unmockkStatic(Dispatchers::class)
    }
}
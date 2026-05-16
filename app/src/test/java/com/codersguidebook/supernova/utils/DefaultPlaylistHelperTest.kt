package com.codersguidebook.supernova.utils

import android.content.Context
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock

class DefaultPlaylistHelperTest {

    private val mockContext = mock(Context::class.java)

    @Test
    // FIXME MAKE THIS TEST MORE RELEVANT
    fun `playlist list should contain four playlist names`() {
        val playlistHelper = DefaultPlaylistHelper(mockContext)

        val playlistNames = playlistHelper.getDefaultPlaylistNames()

        assertEquals(4, playlistNames.size)
    }
}
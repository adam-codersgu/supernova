package com.codersguidebook.supernova.utils

import android.content.Context
import com.codersguidebook.supernova.R
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class DefaultPlaylistHelperTest {

    @RelaxedMockK
    lateinit var mockContext: Context

    @BeforeEach
    fun beforeEach() {
        every { mockContext.getString(R.string.favourites) } returns "Favourites"
        every { mockContext.getString(R.string.recently_played) } returns "Recently played"
        every { mockContext.getString(R.string.song_day) } returns "Song of the day"
        every { mockContext.getString(R.string.most_played) } returns "Most played"
    }

    @Test
    fun `get default playlist names`() {
        val playlistHelper = DefaultPlaylistHelper(mockContext)

        val playlistNames = playlistHelper.getDefaultPlaylistNames()

        assertEquals(4, playlistNames.size)
        assertEquals("Favourites", playlistNames[0])
        assertEquals("Recently played", playlistNames[1])
        assertEquals("Song of the day", playlistNames[2])
        assertEquals("Most played", playlistNames[3])
    }
}
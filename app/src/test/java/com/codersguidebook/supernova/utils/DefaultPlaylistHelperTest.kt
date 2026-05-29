package com.codersguidebook.supernova.utils

import android.content.Context
import com.codersguidebook.supernova.R
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.kotlin.doReturn

class DefaultPlaylistHelperTest {

    private val mockContext = mock(Context::class.java)

    @BeforeEach
    fun beforeEach() {
        Mockito.`when`(mockContext.getString(R.string.favourites)).doReturn("Favourites")
        Mockito.`when`(mockContext.getString(R.string.recently_played)).doReturn("Recently played")
        Mockito.`when`(mockContext.getString(R.string.song_day)).doReturn("Song of the day")
        Mockito.`when`(mockContext.getString(R.string.most_played)).doReturn("Most played")
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
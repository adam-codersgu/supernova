package com.codersguidebook.supernova.utils

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaDescription
import com.codersguidebook.supernova.R
import com.codersguidebook.supernova.entities.Song
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException

/**
 * Utility class for building and handling MediaDescription objects.
 *
 * @param context - A Context instance from which the application context can be sourced.
 */
// TODO: Should maybe initialise an instance of this utils class when MainActivity is created,
//    so the context may be supplied.
class MediaDescriptionManager(context: Context) {
    private val applicationContext: Context

    init {
        this.applicationContext = context.applicationContext
    }

    /**
     * Uses the data for a given Song object to construct a MediaDescription instance.
     *
     * @param song - The Song object that the MediaDescription object should be built for
     * @return MediaDescription object detailing the details of the supplied song
     */
    fun buildMediaDescription(song: Song): MediaDescription {
        return MediaDescription.Builder()
            .setMediaId(song.songID.toString())
            .setIconBitmap(getArtwork(song.albumID))
            .build()
    }

    /**
     * TODO: Implement
     *  Returns default bitmap if an error occurs
     */
    private fun getArtwork(albumArtwork: String?) : Bitmap {
        // set album artwork on player controls
        try {
            return BitmapFactory.Options().run {
                inJustDecodeBounds = true
                val cw = ContextWrapper(applicationContext)
                val directory = cw.getDir("albumArt", Context.MODE_PRIVATE)
                val f = File(directory, "$albumArtwork.jpg")
                BitmapFactory.decodeStream(FileInputStream(f))

                // Calculate inSampleSize. width and height are in pixels
                inSampleSize = calculateInSampleSize(this)

                // Decode bitmap with inSampleSize set
                inJustDecodeBounds = false

                BitmapFactory.decodeStream(FileInputStream(f))
            }
        } catch (_: FileNotFoundException) { }
        // FIXME: Could maybe try Resources.getSystem() for resources - either way this needs testing
        return BitmapFactory.decodeResource(applicationContext.resources, R.drawable.no_album_artwork)
    }
}
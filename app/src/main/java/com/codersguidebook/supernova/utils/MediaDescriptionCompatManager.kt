package com.codersguidebook.supernova.utils

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaDescriptionCompat
import com.codersguidebook.supernova.R
import com.codersguidebook.supernova.entities.Song
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException

/**
 * Utility class for building and handling MediaDescriptionCompat objects.
 *
 * @param context - A Context instance from which the application context can be sourced.
 */
// TODO: Should maybe initialise an instance of this utils class when MainActivity is created,
//    so the context may be supplied.
class MediaDescriptionCompatManager(context: Context) {
    private val applicationContext: Context

    init {
        this.applicationContext = context.applicationContext
    }

    /**
     * Uses the data for a given Song object to construct a MediaDescriptionCompat instance.
     *
     * @param song - The Song object that the MediaDescriptionCompat object should be built for.
     * @return MediaDescriptionCompat object detailing the details of the supplied song.
     */
    fun buildDescription(song: Song): MediaDescriptionCompat {
        val bundle = Bundle()
        bundle.putString("album", song.albumName)
        bundle.putString("album_id", song.albumId)

        return MediaDescriptionCompat.Builder()
            .setExtras(bundle)
            .setIconBitmap(getArtworkAsBitmap(song.albumId))
            .setMediaId(song.songId.toString())
            .setMediaUri(Uri.parse(song.uri))
            .setSubtitle(song.artist)
            .setTitle(song.title)
            .build()
    }

    /**
     * Retrieve the album artwork for a given album ID. If no artwork is found,
     * then a default artwork image is returned instead.
     *
     * @param albumId - The ID of the album that artwork should be retrieved for.
     * @return A Bitmap representation of the album artwork.
     */
    private fun getArtworkAsBitmap(albumId: String?) : Bitmap {
        if (albumId != null) {
            try {
                return BitmapFactory.Options().run {
                    inJustDecodeBounds = true
                    val contextWrapper = ContextWrapper(applicationContext)
                    val imageDirectory = contextWrapper.getDir("albumArt", Context.MODE_PRIVATE)
                    val imageFile = File(imageDirectory, "$albumId.jpg")
                    BitmapFactory.decodeStream(FileInputStream(imageFile))

                    inSampleSize = calculateInSampleSize(this)
                    inJustDecodeBounds = false
                    BitmapFactory.decodeStream(FileInputStream(imageFile))
                }
            } catch (_: FileNotFoundException) { }
        }
        // FIXME: Could maybe try Resources.getSystem() for resources - either way this needs testing
        // If an error has occurred or the album ID is null, then return a default artwork image
        return BitmapFactory.decodeResource(applicationContext.resources, R.drawable.no_album_artwork)
    }

    /**
     * Calculate an inSampleSize value that can be used to scale the
     * dimensions of a Bitmap image. Useful for compressing large images.
     *
     * @param options - The BitmapFactory.Options instance the should be used to
     * calculate the inSampleSize value for.
     * @return An integer inSampleSize value.
     */
    private fun calculateInSampleSize(options: BitmapFactory.Options): Int {
        val reqWidth = 100; val reqHeight = 100
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2; val halfWidth = width / 2

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }
}
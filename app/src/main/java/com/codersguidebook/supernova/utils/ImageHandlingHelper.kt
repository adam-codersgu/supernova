package com.codersguidebook.supernova.utils

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.signature.ObjectKey
import com.codersguidebook.supernova.R
import com.codersguidebook.supernova.entities.Playlist
import java.io.File
import java.io.FileOutputStream

/** Helper that assists with the rendering and storage of image files. */
class ImageHandlingHelper(private val application: Application) {

    companion object {
        private const val ALBUM_ART_DIRECTORY = "albumArt"
        private const val ANIMATION_IMAGE_DIRECTORY = ""
        private const val PLAYLIST_ART_DIRECTORY = "playlistArt"
    }

    /**
     * Load album art into a user interface View.
     *
     * @param albumId - The ID of the album that artwork should be loaded for.
     * @param view - The user interface View that the artwork should be displayed in.
     */
    fun loadImageByAlbumId(albumId: String?, view: ImageView) {
        var file: File? = null
        if (albumId != null) {
            val directory = ContextWrapper(application).getDir(ALBUM_ART_DIRECTORY, Context.MODE_PRIVATE)
            file = File(directory, "$albumId.jpg")
        }
        displayImageByFile(file, view)
    }

    /**
     * Create a File object for the image file associated with a given playlist
     * and load the artwork into a user interface View.
     *
     * @param playlist - The Playlist object that artwork should be loaded for.
     * @param view - The user interface View that the artwork should be displayed in.
     * @return A Boolean indicating whether artwork was successfully found and loaded.
     */
    fun insertPlaylistArtwork(playlist: Playlist, view: ImageView) : Boolean {
        val directory = ContextWrapper(application).getDir(PLAYLIST_ART_DIRECTORY, Context.MODE_PRIVATE)
        val file = File(directory, playlist.playlistId.toString() + ".jpg")
        return if (file.exists()) {
            displayImageByFile(file, view)
            true
        } else false
    }

    /**
     * Display a given image using Glide.
     *
     * @param file - A File object that references the image to be displayed.
     * @param view - The user interface View in which the image should be rendered.
     */
    private fun displayImageByFile(file: File?, view: ImageView) {
        Glide.with(application)
            .load(file ?: R.drawable.no_album_artwork)
            .transition(DrawableTransitionOptions.withCrossFade())
            .centerCrop()
            .signature(ObjectKey(file?.path + file?.lastModified()))
            .override(600, 600)
            .error(R.drawable.no_album_artwork)
            .into(view)
    }

    /**
     * Create a File object for the image file associated with a given resource ID
     * and save the image to a target directory.
     *
     * @param directoryName - The name of the directory storing the image.
     * @param image - A Bitmap representation of the image to be saved.
     * @param resourceId - The ID of the resource that an image should be loaded for.
     */
    fun saveImageByResourceId(directoryName: String, image: Bitmap, resourceId: String) {
        val directory = ContextWrapper(application).getDir(directoryName, Context.MODE_PRIVATE)
        val path = File(directory, "$resourceId.jpg")
        saveImage(image, path)
    }

    /**
     * Saves a bitmap representation of an image to a specified file path location.
     *
     * @param bitmap - The Bitmap instance to be saved.
     * @param path - The location at which the image file should be saved.
     */
    fun saveImage(bitmap: Bitmap, path: File) {
        FileOutputStream(path).use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
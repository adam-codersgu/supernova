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
object ImageHandlingHelper {

    private const val ALBUM_ART_DIRECTORY = "albumArt"
    private const val ANIMATION_IMAGE_DIRECTORY = "customAnimation"
    private const val PLAYLIST_ART_DIRECTORY = "playlistArt"

    /**
     * Load album art into a user interface View.
     *
     * @param application - The application that should serve as the context.
     * @param albumId - The ID of the album that artwork should be loaded for.
     * @param view - The user interface View that the artwork should be displayed in.
     */
    fun loadImageByAlbumId(application: Application, albumId: String?, view: ImageView) {
        var file: File? = null
        if (albumId != null) {
            val directory = ContextWrapper(application).getDir(ALBUM_ART_DIRECTORY, Context.MODE_PRIVATE)
            file = File(directory, "$albumId.jpg")
        }
        displayImageByFile(application, file, view)
    }

    /**
     * Create a File object for the image file associated with a given playlist
     * and load the artwork into a user interface View.
     *
     * @param application - The application that should serve as the context.
     * @param playlist - The Playlist object that artwork should be loaded for.
     * @param view - The user interface View that the artwork should be displayed in.
     * @return A Boolean indicating whether artwork was successfully found and loaded.
     */
    fun loadImageByPlaylist(application: Application, playlist: Playlist, view: ImageView) : Boolean {
        val directory = ContextWrapper(application).getDir(PLAYLIST_ART_DIRECTORY, Context.MODE_PRIVATE)
        val file = File(directory, playlist.playlistId.toString() + ".jpg")
        return if (file.exists()) {
            displayImageByFile(application, file, view)
            true
        } else false
    }

    /**
     * Create a File object for the image file associated with a given custom
     * animation object and load the image into a user interface View.
     *
     * @param application - The application that should serve as the context.
     * @param imageId - The image ID that an image should be loaded for.
     * @param view - The user interface View that the artwork should be displayed in.
     * @return A Boolean indicating whether artwork was successfully found and loaded.
     */
    fun loadImageByCustomAnimationImageId(application: Application, imageId: String, view: ImageView) {
        val directory = ContextWrapper(application).getDir(ANIMATION_IMAGE_DIRECTORY, Context.MODE_PRIVATE)
        val file = File(directory, "$imageId.jpg")
        displayImageByFile(application, file, view)
    }

    /**
     * Display a given image using Glide.
     *
     * @param application - The application that should serve as the context.
     * @param file - A File object that references the image to be displayed.
     * @param view - The user interface View in which the image should be rendered.
     */
    private fun displayImageByFile(application: Application, file: File?, view: ImageView) {
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
     * Delete the image file associated with a given album ID.
     *
     * @param application - The application that should serve as the context.
     * @param resourceId - The ID of the resource that should be evaluated.
     */
    fun deleteAlbumArtByResourceId(application: Application, resourceId: String) {
        val directory = ContextWrapper(application).getDir(ALBUM_ART_DIRECTORY, Context.MODE_PRIVATE)
        deleteFileIfExists(directory, resourceId)
    }

    /**
     * Delete the image file associated with a given playlist ID.
     *
     * @param application - The application that should serve as the context.
     * @param resourceId - The ID of the resource that an image should be loaded for.
     */
    fun deletePlaylistArtByResourceId(application: Application, resourceId: String) {
        val directory = ContextWrapper(application).getDir(PLAYLIST_ART_DIRECTORY, Context.MODE_PRIVATE)
        deleteFileIfExists(directory, resourceId)
    }

    /**
     * Delete a given file if it exists.
     *
     * @param directory - A File object detailing the directory that contains the file to delete.
     * @param resourceId - The ID that identifies the file to delete.
     */
    private fun deleteFileIfExists(directory: File, resourceId: String) {
        val path = File(directory, "$resourceId.jpg")
        if (path.exists()) path.delete()
    }

    /**
     * Determines whether an image file matching a given resource ID is present in the album
     * art directory.
     *
     * @param application - The application that should serve as the context.
     * @param resourceId - The ID of the resource that should be evaluated.
     * @return A Boolean indicating whether a matching image file was found.
     */
    fun doesAlbumArtExistByResourceId(application: Application, resourceId: String) : Boolean {
        val directory = ContextWrapper(application).getDir(ALBUM_ART_DIRECTORY, Context.MODE_PRIVATE)
        return File(directory, "$resourceId.jpg").exists()
    }

    /**
     * Save an image to the album art directory.
     *
     * @param application - The application that should serve as the context.
     * @param resourceId - The ID of the resource that an image should be loaded for.
     * @param image - A Bitmap representation of the image to be saved.
     */
    fun saveAlbumArtByResourceId(application: Application, resourceId: String, image: Bitmap) {
        val directory = ContextWrapper(application).getDir(ALBUM_ART_DIRECTORY, Context.MODE_PRIVATE)
        createFileAndSaveImage(directory, resourceId, image)
    }

    /**
     * Save an image to the custom animation directory.
     *
     * @param application - The application that should serve as the context.
     * @param resourceId - The ID of the resource that an image should be loaded for.
     * @param image - A Bitmap representation of the image to be saved.
     */
    fun saveCustomAnimationImageByResourceId(application: Application, resourceId: String, image: Bitmap) {
        val directory = ContextWrapper(application).getDir(ANIMATION_IMAGE_DIRECTORY, Context.MODE_PRIVATE)
        createFileAndSaveImage(directory, resourceId, image)
    }

    /**
     * Save an image to the playlist art directory.
     *
     * @param application - The application that should serve as the context.
     * @param resourceId - The ID of the resource that an image should be loaded for.
     * @param image - A Bitmap representation of the image to be saved.
     */
    fun savePlaylistArtByResourceId(application: Application, resourceId: String, image: Bitmap) {
        val directory = ContextWrapper(application).getDir(PLAYLIST_ART_DIRECTORY, Context.MODE_PRIVATE)
        createFileAndSaveImage(directory, resourceId, image)
    }

    /**
     * Create a File object for a given directory path and save an image to that directory.
     *
     * @param directory - A File object detailing the directory in which the image should be saved.
     * @param resourceId - The resource ID that should be used in the file name.
     * @param image - A Bitmap representation of the image to be saved.
     */
    private fun createFileAndSaveImage(directory: File, resourceId: String, image: Bitmap) {
        val path = File(directory, "$resourceId.jpg")
        FileOutputStream(path).use {
            image.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
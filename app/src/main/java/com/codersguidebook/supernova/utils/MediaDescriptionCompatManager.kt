package com.codersguidebook.supernova.utils

import android.os.Bundle
import android.support.v4.media.MediaDescriptionCompat
import com.codersguidebook.supernova.entities.Song

/**
 * Utility class for building and handling MediaDescriptionCompat objects.
 */
class MediaDescriptionCompatManager {

    /**
     * Uses the data for a given Song object to construct a MediaDescriptionCompat instance.
     *
     * @param song - The Song object that the MediaDescriptionCompat object should be built for.
     * @return MediaDescriptionCompat object containing the details of the supplied song.
     */
    fun buildDescription(song: Song): MediaDescriptionCompat {
        val extrasBundle = Bundle().apply {
            putString("album", song.albumName)
            putString("album_id", song.albumId)
        }

        return MediaDescriptionCompat.Builder()
            .setExtras(extrasBundle)
            .setMediaId(song.songId.toString())
            .setSubtitle(song.artist)
            .setTitle(song.title)
            .build()
    }

    /**
     * Uses the data for a given Song object to create a Bundle containing the information
     * necessary to construct a MediaDescriptionCompat instance. Obtaining the data in Bundle
     * format is useful for dispatching the data via custom commands to the media browser service.
     *
     * @param song - The Song object that the MediaDescriptionCompat object should be built for.
     * @return Bundle containing the details of the supplied song.
     */
    fun getDescriptionAsBundle(song: Song): Bundle {
        val extrasBundle = Bundle().apply {
            putString("album", song.albumName)
            putString("album_id", song.albumId)
        }
        return Bundle().apply {
            putBundle("extras", extrasBundle)
            putString("mediaId", song.songId.toString())
            putString("subtitle", song.artist)
            putString("title", song.title)
        }
    }
}
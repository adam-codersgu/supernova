package com.codersguidebook.supernova.utils

import android.net.Uri
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
     * @return MediaDescriptionCompat object detailing the details of the supplied song.
     */
    fun buildDescription(song: Song): MediaDescriptionCompat {
        val bundle = Bundle()
        bundle.putString("album", song.albumName)
        bundle.putString("album_id", song.albumId)

        return MediaDescriptionCompat.Builder()
            .setExtras(bundle)
            .setMediaId(song.songId.toString())
            .setMediaUri(Uri.parse(song.uri))
            .setSubtitle(song.artist)
            .setTitle(song.title)
            .build()
    }
}
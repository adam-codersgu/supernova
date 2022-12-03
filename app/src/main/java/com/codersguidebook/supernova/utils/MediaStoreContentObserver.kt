package com.codersguidebook.supernova.utils

import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import com.codersguidebook.supernova.MainActivity

class MediaStoreContentObserver(handler: Handler, activity: MainActivity): ContentObserver(handler) {

    private val activity: MainActivity

    init {
        this.activity = activity
    }

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        super.onChange(selfChange, uri)

        uri?.let {
            activity.handleChangeToContentUri(uri)
        }
    }
}
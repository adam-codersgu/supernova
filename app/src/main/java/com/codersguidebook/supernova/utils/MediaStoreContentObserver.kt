package com.codersguidebook.supernova.utils

import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.util.Log
import com.codersguidebook.supernova.MainActivity

class MediaStoreContentObserver(handler: Handler, activity: MainActivity): ContentObserver(handler) {

    private val activity: MainActivity

    init {
        this.activity = activity
    }

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        super.onChange(selfChange, uri)

        Log.e("DEBUGGING", "onChange 2 called. selfChange: $selfChange")
        Log.e("DEBUGGING", "onChange 2 called. uri: $uri")

        uri?.let {
            activity.changeToContentUri(uri)
        }
    }
}
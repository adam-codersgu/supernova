package com.codersguidebook.supernova.utils

import android.annotation.TargetApi
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.util.Log
import com.codersguidebook.supernova.MainActivity

class MediaStoreContentObserver(handler: Handler, activity: MainActivity): ContentObserver(handler) {
    override fun onChange(selfChange: Boolean) {
        super.onChange(selfChange)
        Log.d("DEBUGGING", "onChange 1 called. selfChange: $selfChange")
    }

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        super.onChange(selfChange, uri)
        Log.d("DEBUGGING", "onChange 2 called. selfChange: $selfChange")
        Log.d("DEBUGGING", "onChange 2 called. uri: $uri")
    }

    override fun onChange(selfChange: Boolean, uri: Uri?, flags: Int) {
        super.onChange(selfChange, uri, flags)
        Log.d("DEBUGGING", "onChange 3 called. selfChange: $selfChange")
        Log.d("DEBUGGING", "onChange 3 called. uri: $uri")
        Log.d("DEBUGGING", "onChange 3 called. flags: $flags")
    }

    @TargetApi(Build.VERSION_CODES.R)
    override fun onChange(selfChange: Boolean, uris: MutableCollection<Uri>, flags: Int) {
        super.onChange(selfChange, uris, flags)
        Log.d("DEBUGGING", "onChange 4 called. selfChange: $selfChange")
        Log.d("DEBUGGING", "onChange 4 called. uris: $uris")
        Log.d("DEBUGGING", "onChange 4 called. flags: $flags")
    }
}
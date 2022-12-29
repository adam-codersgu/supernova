package com.codersguidebook.supernova.utils

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.checkSelfPermission
import com.codersguidebook.supernova.params.PermissionConstants.Companion.EXTERNAL_STORAGE_PERMISSION

/** Helper to access the device's storage. */
class StorageAccessPermissionHelper(private val activity: Activity) {

    /** Check to see we have the necessary permissions for this app. */
    fun hasReadPermission(): Boolean {
        return checkSelfPermission(activity, READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    /** Check to see we have the necessary permissions for this app, and ask for them if we don't. */
    fun requestPermissions() {
        ActivityCompat.requestPermissions(activity, arrayOf(READ_EXTERNAL_STORAGE),
            EXTERNAL_STORAGE_PERMISSION)
    }

    /** Check to see if we need to show the rationale for this permission. */
    fun shouldShowRequestPermissionRationale(): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, READ_EXTERNAL_STORAGE)
    }

    /** Launch Application Setting to grant permission. */
    fun launchPermissionSettings() {
        val intent = Intent().apply {
            this.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            this.data = Uri.fromParts("package", activity.packageName, null)
        }
        activity.startActivity(intent)
    }
}
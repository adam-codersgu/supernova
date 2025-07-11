package com.codersguidebook.supernova.utils

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_AUDIO
import android.Manifest.permission.READ_MEDIA_IMAGES
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.checkSelfPermission
import com.codersguidebook.supernova.params.PermissionConstants.Companion.EXTERNAL_STORAGE_PERMISSION

/** Helper to access the device's storage. */
class StorageAccessPermissionHelper(private val activity: Activity) {

    /** Check to see we have the necessary permissions for this app. */
    fun hasPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            // Pre-SDK 33
            checkSelfPermission(activity, READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        } else {
            // SDK 33 and up
            checkSelfPermission(activity, READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(activity, READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        }
    }

    /** Check to see we have the necessary permissions for this app, and ask for them if we don't. */
    fun requestPermissions() {
        Log.i("PERMISSIONS", "Requesting read permissions")

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            // Pre-SDK 33
            ActivityCompat.requestPermissions(activity, arrayOf(READ_EXTERNAL_STORAGE),
                EXTERNAL_STORAGE_PERMISSION)
        } else {
            // SDK 33 and up
            ActivityCompat.requestPermissions(activity, arrayOf(READ_MEDIA_AUDIO, READ_MEDIA_IMAGES),
                EXTERNAL_STORAGE_PERMISSION)
        }
    }

    /** Check to see if we need to show the rationale for this permission. */
    fun shouldShowPermissionRationale(): Boolean {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            // Pre-SDK 33
            ActivityCompat.shouldShowRequestPermissionRationale(activity, READ_EXTERNAL_STORAGE)
        } else {
            // SDK 33 and up
            ActivityCompat.shouldShowRequestPermissionRationale(activity, READ_MEDIA_AUDIO) &&
                    ActivityCompat.shouldShowRequestPermissionRationale(activity, READ_MEDIA_IMAGES)
        }
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
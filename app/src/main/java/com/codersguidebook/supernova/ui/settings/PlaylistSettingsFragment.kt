package com.codersguidebook.supernova.ui.settings

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.codersguidebook.supernova.R

class PlaylistSettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.playlist_preferences, rootKey)
    }
}
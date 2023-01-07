package com.codersguidebook.supernova.ui.settings

import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.preference.PreferenceFragmentCompat
import com.codersguidebook.supernova.R
import com.codersguidebook.supernova.params.SharedPreferencesConstants.Companion.ANIMATION_TYPE
import com.codersguidebook.supernova.params.SharedPreferencesConstants.Companion.APPLICATION_LANGUAGE
import com.codersguidebook.supernova.params.SharedPreferencesConstants.Companion.CUSTOM_ANIMATION_IMAGE_IDS
import com.codersguidebook.supernova.ui.currentlyPlaying.CustomAnimationFragment

class SettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        preferenceManager.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onResume() {
        super.onResume()
        preferenceManager.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        preferenceManager.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(
        sharedPreferences: SharedPreferences?,
        key: String?
    ) {
        sharedPreferences?.run {
            when (key) {
                ANIMATION_TYPE -> {
                    if (sharedPreferences.getString(key, getString(R.string.leaves)) == getString(R.string.custom_image)) {
                        if (sharedPreferences.getString(CUSTOM_ANIMATION_IMAGE_IDS, null) == null) {
                            requireActivity().supportFragmentManager
                                .beginTransaction()
                                .replace(R.id.settings, CustomAnimationFragment())
                                .addToBackStack(null)
                                .commit()
                        }
                    }
                }
                APPLICATION_LANGUAGE -> {
                    val selectedLanguage = sharedPreferences.getString(key, getString(R.string.english_code))
                    val appLocale = LocaleListCompat.forLanguageTags(selectedLanguage)
                    AppCompatDelegate.setApplicationLocales(appLocale)
                }
            }
        }
    }
}
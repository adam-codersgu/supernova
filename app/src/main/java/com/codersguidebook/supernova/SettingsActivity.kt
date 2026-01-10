package com.codersguidebook.supernova

import android.os.Bundle
import android.view.MenuItem
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.codersguidebook.supernova.databinding.SettingsActivityBinding
import com.codersguidebook.supernova.ui.settings.SettingsFragment

class SettingsActivity : AppCompatActivity(), PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    companion object {
        const val PREFERENCE_FRAGMENT_TAG = "PreferenceFragment"
    }

    private lateinit var binding: SettingsActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SettingsActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, SettingsFragment())
            .commit()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = insets.left
                bottomMargin = insets.bottom
                rightMargin = insets.right
                topMargin = insets.top
            }

            WindowInsetsCompat.CONSUMED
        }
    }

    override fun onPreferenceStartFragment(caller: PreferenceFragmentCompat, pref: Preference): Boolean {
        val args = pref.extras
        val fragment = supportFragmentManager.fragmentFactory.instantiate(
            classLoader,
            pref.fragment!!)
        fragment.arguments = args

        supportFragmentManager.beginTransaction()
            .replace(R.id.settings, fragment, PREFERENCE_FRAGMENT_TAG)
            .addToBackStack(null)
            .commit()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                val preferenceFragment = supportFragmentManager.findFragmentByTag(PREFERENCE_FRAGMENT_TAG)
                if (preferenceFragment != null && preferenceFragment.isVisible) {
                    supportFragmentManager.popBackStack()
                    return true
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
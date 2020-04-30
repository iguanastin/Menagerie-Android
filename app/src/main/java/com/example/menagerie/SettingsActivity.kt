package com.example.menagerie

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager


private const val PICK_DOWNLOAD_FOLDER_RESULT = 5

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, SettingsFragment())
            .commit()

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            val clearCacheButton: Preference = findPreference("clear-cache")!!
            val summary: String = clearCacheButton.summary.toString()
            clearCacheButton.summary = summary + " (${byteSizeToString(getDirectorySize(requireContext().cacheDir))})"
            clearCacheButton.setOnPreferenceClickListener {
                context?.cacheDir?.deleteRecursively()

                clearCacheButton.summary = summary + " (${byteSizeToString(getDirectorySize(requireContext().cacheDir))})"

                true
            }
        }
    }

}
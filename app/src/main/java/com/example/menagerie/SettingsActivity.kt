package com.example.menagerie

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat


class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, SettingsFragment())
            .commit()

        setSupportActionBar(findViewById(R.id.settingsToolbar))
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
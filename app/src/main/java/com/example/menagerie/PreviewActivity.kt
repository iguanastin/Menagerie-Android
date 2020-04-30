package com.example.menagerie

import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import java.io.IOException

const val PREVIEW_EXTRA_ID = "preview_extra_id"

class PreviewActivity : AppCompatActivity() {

    private lateinit var model: MenagerieViewModel
    private lateinit var preferences: SharedPreferences


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preview)

        setSupportActionBar(findViewById(R.id.previewToolbar))

        model = ViewModelProvider.AndroidViewModelFactory.getInstance(application)
            .create(MenagerieViewModel::class.java)
        preferences = PreferenceManager.getDefaultSharedPreferences(this)

        val url = intent.getStringExtra(PREVIEW_EXTRA_ID)
        if (url == null) {
            finish()
            return
        }

        model.requestImage(url, success = { code, image ->
            runOnUiThread {
                findViewById<ImageView>(R.id.previewImageView).setImageDrawable(image)
            }
        }, failure = { e: IOException? ->
            simpleAlert(this, "Failed to load", "Unexpected error trying to load file", "Back") {
                finish()
            }
        })
    }

    /**
     * Called when options menu is created
     */
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.preview_toolbar_menu, menu)
        return true
    }

    /**
     * Called when a menu option is selected
     */
    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.toolbar_forget -> {
                // TODO
                true
            }
            R.id.toolbar_delete -> {
                // TODO
                true
            }
            R.id.toolbar_download -> {
                // TODO
                true
            }
            R.id.toolbar_share -> {
                // TODO
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

}

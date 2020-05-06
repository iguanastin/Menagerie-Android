package com.example.menagerie

import android.Manifest
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import java.io.IOException


const val PREVIEW_URL_EXTRA_ID = "preview_extra_id"
const val PREVIEW_TYPE_EXTRA_ID = "preview_type_extra_id"
const val PREVIEW_TYPE_IMAGE = "image"
const val PREVIEW_TYPE_VIDEO = "video"

const val PERMISSIONS_WRITE_STORAGE_FOR_DOWNLOAD = 5

class PreviewActivity : AppCompatActivity() {

    private lateinit var preferences: SharedPreferences

    private lateinit var imagePreview: ImageView
    private lateinit var videoPreview: VideoView

    private var position: Int = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.preview_activity)

        imagePreview = findViewById(R.id.previewImageView)
        videoPreview = findViewById(R.id.previewVideoView)

        setSupportActionBar(findViewById(R.id.previewToolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        preferences = PreferenceManager.getDefaultSharedPreferences(this)

//        val tv = findViewById<AutoCompleteTextView>(R.id.autoCompleteTextView)
//        tv.setAdapter(ArrayAdapter(this, android.R.layout.select_dialog_item, arrayOf("asdf", "testing", "tesT_tag")))
//        tv.setOnItemClickListener { adapterView: AdapterView<*>, view1: View, i: Int, l: Long ->
//            hideKeyboard(view1)
//        }

        initializePreview()
    }

    private fun initializePreview() {
        val url = intent.getStringExtra(PREVIEW_URL_EXTRA_ID)
        val type = intent.getStringExtra(PREVIEW_TYPE_EXTRA_ID)
        if (url.isNullOrEmpty() || type.isNullOrEmpty()) {
            finish()
            return
        }

        when (type) {
            PREVIEW_TYPE_IMAGE -> {
                displyImageType(url)
            }
            PREVIEW_TYPE_VIDEO -> {
                displayVideoType(Uri.parse(url))
            }
            else -> {
                simpleAlert(
                    this,
                    "Unknown type",
                    "Cannot display unknown type: $type",
                    "Close"
                ) {
                    finish()
                }
            }
        }
    }

    private fun displayVideoType(uri: Uri?) {
        imagePreview.visibility = View.GONE
        videoPreview.visibility = View.VISIBLE

        val mediaController = MediaController(this)
        videoPreview.setMediaController(mediaController)
        mediaController.setAnchorView(videoPreview)

        videoPreview.setOnPreparedListener {
            videoPreview.seekTo(position)

            if (position == 0) {
                videoPreview.start()
            } else {
                videoPreview.pause()
            }
        }

        videoPreview.setVideoURI(uri)
        videoPreview.requestFocus()
    }

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)
        savedInstanceState.putInt("Position", videoPreview.currentPosition)
        videoPreview.pause()
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        position = savedInstanceState.getInt("Position")
        videoPreview.seekTo(position)
    }

    private fun displyImageType(url: String) {
        imagePreview.visibility = View.VISIBLE
        videoPreview.visibility = View.GONE
        APIClient.requestImage(url, success = { code, image ->
            runOnUiThread {
                imagePreview.setImageBitmap(image)
            }
        }, failure = { e: IOException? ->
            simpleAlert(
                this,
                "Failed to load",
                "Unexpected error trying to load file",
                "Close"
            ) {
                finish()
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.preview_toolbar_menu, menu)
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSIONS_WRITE_STORAGE_FOR_DOWNLOAD -> {
                download()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.toolbar_download -> {
                requirePermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE, "Write permissions required", "In order to download files, this app must be granted permission to write external storage", PERMISSIONS_WRITE_STORAGE_FOR_DOWNLOAD) {
                    download()
                }
                true
            }
            R.id.toolbar_edit -> {
                // TODO
                simpleAlert(
                    this,
                    message = "This feature is not yet implemented"
                )
                true
            }
            R.id.toolbar_share -> {
                // TODO
                simpleAlert(
                    this,
                    message = "This feature is not yet implemented"
                )
                true
            }
            R.id.toolbar_forget -> {
                // TODO
                simpleAlert(
                    this,
                    message = "This feature is not yet implemented"
                )
                true
            }
            R.id.toolbar_delete -> {
                // TODO
                simpleAlert(
                    this,
                    message = "This feature is not yet implemented"
                )
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    private fun download() {
        println("download")
        // TODO

        simpleAlert(
            this,
            message = "This feature is not yet implemented"
        )
    }

    fun tagsButtonClicked(view: View) {
        TagsBottomDialogFragment().show(supportFragmentManager, "test tag")

        // TODO
//        simpleAlert(
//            this,
//            message = "This feature is not yet implemented"
//        )
    }

    fun infoButtonClicked(view: View) {
        // TODO
        simpleAlert(
            this,
            message = "This feature is not yet implemented"
        )
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

}

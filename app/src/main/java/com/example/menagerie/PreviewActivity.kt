package com.example.menagerie

import android.Manifest
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.MediaController
import android.widget.TextView
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import java.io.IOException


const val PREVIEW_ITEM_EXTRA_ID = ""
const val PREVIEW_TYPE_IMAGE = "image"
const val PREVIEW_TYPE_VIDEO = "video"

const val PERMISSIONS_WRITE_STORAGE_FOR_DOWNLOAD = 5

class PreviewActivity : AppCompatActivity() {

    private lateinit var imagePreview: ImageView
    private lateinit var videoPreview: VideoView

    private var item: Item? = null

    private var videoPosition: Int = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.preview_activity)

        item = intent.getParcelableExtra(PREVIEW_ITEM_EXTRA_ID)
        if (item == null) {
            simpleAlert(this, message = "Failed to get Item") {
                finish()
            }
        }

        imagePreview = findViewById(R.id.previewImageView)
        videoPreview = findViewById(R.id.previewVideoView)

        setSupportActionBar(findViewById(R.id.previewToolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        initializePreview()
    }

    private fun initializePreview() {
        if (item == null) return

        findViewById<TextView>(R.id.previewIDTextView).text = item?.id.toString()

        when (item!!.type) {
            PREVIEW_TYPE_IMAGE -> {
                displyImageType(item!!.fileURL!!)
            }
            PREVIEW_TYPE_VIDEO -> {
                displayVideoType(Uri.parse(item!!.fileURL!!))
            }
            else -> {
                simpleAlert(
                    this,
                    message = "Cannot display unknown type: ${item!!.type}"
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
            videoPreview.seekTo(videoPosition)

            if (videoPosition == 0) {
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
        videoPosition = savedInstanceState.getInt("Position")
        videoPreview.seekTo(videoPosition)
    }

    private fun displyImageType(url: String) {
        imagePreview.visibility = View.VISIBLE
        videoPreview.visibility = View.GONE
        APIClient.requestImage(url, success = { _, image ->
            runOnUiThread {
                imagePreview.setImageBitmap(image)
            }
        }, failure = { e: IOException? ->
            e?.printStackTrace()
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
                requirePermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    "Write permissions required",
                    "In order to download files, this app must be granted permission to write external storage",
                    PERMISSIONS_WRITE_STORAGE_FOR_DOWNLOAD
                ) {
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
        TagsBottomDialogFragment(item!!).show(supportFragmentManager, null)

        // TODO
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

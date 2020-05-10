package com.example.menagerie

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.MediaController
import android.widget.TextView
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import okio.Okio
import okio.Source
import okio.source
import java.io.File
import java.io.IOException


class PreviewActivity : AppCompatActivity() {

    private lateinit var imagePreview: ImageView
    private lateinit var videoPreview: VideoView
    private lateinit var indexTextView: TextView

    private var item: Item? = null
    private var search: ItemSearch? = null
    private var page: Int = -1
    private var indexInPage: Int = -1

    private var videoPosition: Int = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preview)

        item = intent.getParcelableExtra(PREVIEW_ITEM_EXTRA_ID)
        search = intent.getParcelableExtra(PREVIEW_SEARCH_EXTRA_ID)
        page = intent.getIntExtra(PREVIEW_PAGE_EXTRA_ID, -1)
        indexInPage = intent.getIntExtra(PREVIEW_INDEX_IN_PAGE_EXTRA_ID, -1)
        if (item == null || search == null || page < 0 || indexInPage < 0) {
            simpleAlert(this, message = "Invalid data") {
                finish()
            }
        }

        imagePreview = findViewById(R.id.previewImageView)
        videoPreview = findViewById(R.id.previewVideoView)
        indexTextView = findViewById(R.id.previewIndexTextView)

        indexTextView.text = "0/0"

        setSupportActionBar(findViewById(R.id.previewToolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        initializePreview()
    }

    private fun initializePreview() {
        title = item?.id.toString()

        val index = page * search!!.pageSize + indexInPage
        val total = search!!.total
        @SuppressLint("SetTextI18n")
        indexTextView.text = "$index/$total"

        when (item!!.type) {
            Item.IMAGE_TYPE -> {
                displyImageType(APIClient.address + item!!.fileURL!!)
            }
            Item.VIDEO_TYPE -> {
                displayVideoType(Uri.parse(item!!.fileURL!!))
            }
            Item.GROUP_TYPE -> {
                // TODO display some group thumbnails and title
                // TODO allow user to open group
            }
            else -> {
                // TODO display notice and filename of unknown type
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
            Codes.preview_request_storage_perms_for_download.ordinal -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) download()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.toolbar_download -> {
                requirePermissions(
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE),
                    "Storage permissions required",
                    "In order to download files, this app must be granted permission to read/write external storage",
                    Codes.preview_request_storage_perms_for_download.ordinal
                ) {
                    download()
                }
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
        val filename = item!!.filePath!!.substringAfterLast("/").substringAfterLast("\\")
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

        APIClient.requestFile(item!!.fileURL!!, File(dir, filename), success = { _, file ->
            MediaScannerConnection.scanFile(this, arrayOf(file.absolutePath), null, null)

            runOnUiThread {
                simpleAlert(this, message = "Downloaded file: $filename")
            }
        })
    }

    fun tagsButtonClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        TagsDialogFragment(item!!, onClick = { tag ->
            setResult(RESULT_OK, Intent().apply { putExtra(TAG_NAME_EXTRA_ID, tag.name) })
            finish()
        }).show(supportFragmentManager, null)
    }

    fun infoButtonClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        ItemInfoDialogFragment(item!!).show(supportFragmentManager, null)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

}

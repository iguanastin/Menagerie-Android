package com.example.menagerie

import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream
import java.net.URLEncoder
import java.util.regex.Pattern


class SearchActivity : AppCompatActivity() {

    private val permissionsReadStorageForUpload: Int = 1
    private val pickFileResultCode = 1
    private val preferredThumbnailWidthDP = 125

    private lateinit var grid: RecyclerView
    private lateinit var gridProgress: ProgressBar
    private lateinit var searchText: EditText
    private lateinit var searchButton: Button

    private var client: OkHttpClient? = null
    private var cache: Cache? = null

    private lateinit var preferences: SharedPreferences
    private lateinit var prefsListener: (SharedPreferences, String) -> Unit

    private var address: String? = null
    private var thumbnailAdapter: ThumbnailAdapter? = null
    private val data: MutableList<String> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        preferences = PreferenceManager.getDefaultSharedPreferences(this)
        prefsListener =
            { prefs, key ->
                when (key) {
                    "preferred-address" -> {
                        address = initializeAddress(prefs.getString("preferred-address", null))
                        if (address != null) search()
                    }
                    "fallback-address" -> {
                        // TODO
                    }
                    "cache-size" -> {
                        initializeHTTPClient()
                    }
                }
            }
        preferences.registerOnSharedPreferenceChangeListener(prefsListener)

        address = initializeAddress(preferences.getString("preferred-address", null))

        initializeHTTPClient()

        initializeViews()
        initializeListeners()

        if (address != null) search()
    }

    private fun initializeHTTPClient() {
        cache?.close()
        client = null

        cache = Cache(
            applicationContext.cacheDir,
            1024 * 1024 * preferences
                .getInt("cache-size", 128)
                .toLong()
        )
        client = OkHttpClient.Builder().cache(cache).build()
    }

    private fun initializeAddress(address: String?): String? {
        if (!address.isNullOrEmpty()) {
            if (Pattern.matches("(http://)?[a-zA-Z0-9.\\-]+:[0-9]+", address)) {
                return if (!address.startsWith("http://")) {
                    "http://$address"
                } else {
                    address
                }
            } else {
                simpleAlert(
                    this,
                    "Invalid address format",
                    "Expected format:\n     [IP/Hostname]:[Port]\nE.g.\n     123.45.6.78:12345",
                    "Ok"
                ) {
                    startActivity(Intent(this, SettingsActivity::class.java))
                }

                return null
            }
        } else {
            simpleAlert(
                this,
                "Menagerie address not set",
                "Please set your preferred and fallback addresses",
                "Ok"
            ) {
                startActivity(Intent(this, SettingsActivity::class.java))
            }

            return null
        }
    }

    override fun finish() {
        super.finish()

        thumbnailAdapter?.release()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.search_toolbar_menu, menu)
        return true
    }

    private fun initializeViews() {
        grid = findViewById(R.id.grid)
        gridProgress = findViewById(R.id.gridProgress)
        searchText = findViewById(R.id.searchText)
        searchButton = findViewById(R.id.searchButton)

        setSupportActionBar(findViewById(R.id.my_toolbar))
    }

    private fun initializeListeners() {
        // Hide or show toTopButton based on scroll position
        grid.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                findViewById<View>(R.id.toTopButton).visibility =
                    if ((recyclerView.layoutManager as GridLayoutManager).findFirstVisibleItemPosition() > 0) View.VISIBLE else View.GONE
            }
        })

        searchText.onSubmit {
            searchButton.performClick()
        }

        grid.onGlobalLayout {
            val span = grid.width / TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                preferredThumbnailWidthDP.toFloat(),
                resources.displayMetrics
            ).toInt()
            thumbnailAdapter = ThumbnailAdapter(this@SearchActivity, client!!, data, span)

            grid.apply {
                layoutManager = GridLayoutManager(context, span)
                adapter = thumbnailAdapter
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            permissionsReadStorageForUpload -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    userPickFileForUpload()
                }
                return
            }
        }
    }

    private fun userPickFileForUpload() {
        var chooseFile = Intent(Intent.ACTION_GET_CONTENT)
        println(preferences.getString("preferred-address", null))
        chooseFile.type = "*/*"
        chooseFile = Intent.createChooser(chooseFile, "Choose a file")
        startActivityForResult(chooseFile, pickFileResultCode)
    }

    /**
     * Utility listener used for user file choosing
     *
     * @param requestCode Identifying request code
     * @param resultCode  Result code from the child activity
     * @param data        Data returned from the child activity
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == pickFileResultCode) {
            if (resultCode == -1) { // ??? Magic number
                if (data != null) {
                    upload(data.data!!)
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.toolbar_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            R.id.toolbar_upload -> {
                attemptUploadAction()
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    private fun attemptUploadAction() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            ) {
                AlertDialog.Builder(this)
                    .setTitle("Storage permissions required for this operation")
                    .setMessage("In order to upload files, this app must be granted permission to read external storage")
                    .setNeutralButton("Ok") { _, _ ->
                        ActivityCompat.requestPermissions(
                            this,
                            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                            permissionsReadStorageForUpload
                        )
                    }.create().show()
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    permissionsReadStorageForUpload
                )
            }
        } else {
            userPickFileForUpload()
        }
    }

    /**
     * Uploads content to the API
     *
     * @param uri URI of content
     */
    private fun upload(uri: Uri) {
        val stream: InputStream = contentResolver.openInputStream(uri)!!
        val mime: String = contentResolver.getType(uri)!!

        val filename: String = URLEncoder.encode(System.currentTimeMillis().toString() + "." + MimeTypeMap.getSingleton().getExtensionFromMimeType(mime), "UTF-8")

        val bytes: ByteArray = stream.readBytes() // TODO okhttp3 solution is shit because I can't stream the content

        client!!.newCall(
            Request.Builder()
                .post(bytes.toRequestBody(mime.toMediaType())).url(
                    "$address/upload?filename=$filename"
                ).build()
        ).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (response.code != 201) {
                        runOnUiThread {
                            AlertDialog.Builder(this@SearchActivity).setTitle("Unexpected response")
                                .setMessage("While uploading: $filename")
                                .setNeutralButton("Ok") { _, _ -> }
                                .create().show()
                        }
                    }
                }
            }
        })
    }

    /**
     * Requests a search from the API and updates the view with results
     *
     * @param terms      terms to search with
     * @param page       0 indexed page to retrieve
     * @param descending Order descending
     * @param ungroup    ungroup and include group elements
     */
    private fun search(
        terms: String = "",
        page: Int = 0,
        descending: Boolean = true,
        ungroup: Boolean = false
    ) {
        data.clear()
        runOnUiThread {
            thumbnailAdapter?.notifyDataSetChanged()
            gridProgress.visibility = View.VISIBLE
        }

        var url = "$address/search?page=$page&terms=" + URLEncoder.encode(terms, "UTF-8")
        if (descending) url += "&desc"
        if (ungroup) url += "&ungroup"

        client!!.newCall(Request.Builder().url(url).build())
            .enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    val root = JSONObject(response.body!!.string())
                    val count = root.getInt("count")

                    if (count > 0) {
                        val items = root.getJSONArray("items")
                        for (i in 0 until count) {
                            data.add(address + (items[i] as JSONObject).getString("thumbnail"))
                        }
                    }

                    runOnUiThread {
                        thumbnailAdapter?.notifyDataSetChanged()
                        gridProgress.visibility = View.GONE
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    e.printStackTrace()
                    runOnUiThread {
                        AlertDialog.Builder(this@SearchActivity).setTitle("Error")
                            .setMessage("Failed to connect to: $address")
                            .setNeutralButton("Ok") { _: DialogInterface?, _: Int -> this@SearchActivity.finish() }
                            .create().show()
                    }
                }
            })
    }

    fun onSearchClick(view: View) {
        hideKeyboard(view)
        search(searchText.text.toString())
    }

    fun toTopOfGrid(view: View) {
        if ((grid.layoutManager as GridLayoutManager).findLastVisibleItemPosition() < 100)
            grid.smoothScrollToPosition(0)
        else
            grid.scrollToPosition(0)
    }

}
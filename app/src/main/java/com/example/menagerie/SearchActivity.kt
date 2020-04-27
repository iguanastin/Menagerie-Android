package com.example.menagerie

import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toFile
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File


class SearchActivity : AppCompatActivity() {

    private val permissionsReadStorageForUpload: Int = 1
    private val pickFileResultCode = 1

    lateinit var grid: RecyclerView
    lateinit var gridProgress: ProgressBar
    lateinit var searchText: EditText
    lateinit var searchButton: Button
    lateinit var moreSearchLayout: LinearLayout

    private lateinit var client: OkHttpClient
    private lateinit var cache: Cache

    lateinit var address: String
    lateinit var thumbnailAdapter: ThumbnailAdapter
    val data: MutableList<String> = mutableListOf()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        address = "http://" + intent.getCharSequenceExtra(ADDRESS)
        cache = Cache(applicationContext.cacheDir, 1024 * 1024 * 100)
        client = OkHttpClient.Builder().cache(cache).build()
        thumbnailAdapter = ThumbnailAdapter(this@SearchActivity, client, data)

        initViews()
        initListeners()

        search()
    }

    override fun finish() {
        super.finish()

        thumbnailAdapter.release()
    }

    private fun initViews() {
        grid = findViewById(R.id.grid)
        gridProgress = findViewById(R.id.gridProgress)
        searchText = findViewById(R.id.searchText)
        searchButton = findViewById(R.id.searchButton)
        moreSearchLayout = findViewById(R.id.moreSearchLayout)

        supportActionBar?.hide()

        grid.apply {
            layoutManager =
                GridLayoutManager(context, context.resources.getInteger(R.integer.grid_span))
            adapter = thumbnailAdapter
        }
    }

    private fun initListeners() {
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

        searchButton.setOnLongClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                ) {
                    AlertDialog.Builder(grid.context)
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

            true
        }

        grid.onGlobalLayout {
            // TODO set span of grid and element size nicely
        }
    }

    private fun userPickFileForUpload() {
        var chooseFile = Intent(Intent.ACTION_GET_CONTENT)
        chooseFile.type = "*/*"
        chooseFile = Intent.createChooser(chooseFile, "Choose a file")
        startActivityForResult(chooseFile, pickFileResultCode)
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
                    uploadFile(data.data!!.toFile()) // TODO toFile doesn't work because android documents bullshit
                    DocumentsContract.getDocumentId(data.data)
                }
            }
        }
    }

    /**
     * Uploads a file to the current Menagerie API server
     *
     * @param file The file to upload
     */
    private fun uploadFile(file: File) {
        val filename: String = URLEncoder.encode(file.name, "UTF-8")

        client.newCall(
            Request.Builder()
                .post(file.asRequestBody("application/octet-stream".toMediaType())).url(
                    "$address/upload?filename=$filename"
                ).build()
        ).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.code != 201) {
                    runOnUiThread {
                        AlertDialog.Builder(grid.context).setTitle("Unexpected response")
                            .setMessage("While uploading: $filename")
                            .setNeutralButton("Ok") { _, _ -> }
                            .create().show()
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
            thumbnailAdapter.notifyDataSetChanged()
            gridProgress.visibility = View.VISIBLE
        }

        var url = "$address/search?page=$page&terms=" + URLEncoder.encode(terms, "UTF-8")
        if (descending) url += "&desc"
        if (ungroup) url += "&ungroup"

        client.newCall(Request.Builder().url(url).build())
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
                        thumbnailAdapter.notifyDataSetChanged()
                        gridProgress.visibility = View.GONE
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    e.printStackTrace()
                    runOnUiThread {
                        AlertDialog.Builder(grid.context).setTitle("Error")
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
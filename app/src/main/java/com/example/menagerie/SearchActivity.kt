package com.example.menagerie

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject
import java.io.IOException


const val DEFAULT_CACHE_SIZE: Int = 128
const val PREFERRED_THUMBNAIL_SIZE_DP: Int = 125

const val PERMISSIONS_READ_STORAGE_FOR_UPLOAD: Int = 1
const val PICK_UPLOAD_FILE_RESULT_CODE: Int = 2
const val SETTINGS_ACTIVIYY_RESULT_CODE: Int = 6

class SearchActivity : AppCompatActivity() {

    private lateinit var grid: RecyclerView
    private lateinit var gridProgress: ProgressBar
    private lateinit var gridErrorText: TextView
    private lateinit var gridErrorIcon: ImageView
    private lateinit var searchText: EditText
    private lateinit var searchButton: Button

    private lateinit var model: SearchViewModel

    private lateinit var preferences: SharedPreferences
    private lateinit var prefsListener: (SharedPreferences, String) -> Unit

    private var thumbnailAdapter: ThumbnailAdapter? = null


    /**
     * Called when the activity is created
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.search_activity)

        // Initialize viewmodel
        model = ViewModelProvider.AndroidViewModelFactory.getInstance(application)
            .create(SearchViewModel::class.java)

        // Initialize preferences
        initializePreferences()

        // Initialize HTTP client manager
        ClientManager.cacheDir.value = cacheDir
        ClientManager.cacheSize.value =
            preferences.getInt("cache-size", DEFAULT_CACHE_SIZE).toLong()

        // Initialize APIClient
        APIClient.address = preferences.getString("preferred-address", null)

        // Initialize views
        initializeViews()

        // Attempt basic search and populate grid
        if (APIClient.isAddressValid(APIClient.address)) {
            showGridStatus(progress = true)

            APIClient.requestSearch(failure = { e: IOException? ->
                e?.printStackTrace()
                runOnUiThread {
                    showGridStatus(
                        error = true,
                        errorMessage = "Failed to connect to:\n${APIClient.address}"
                    )
                }
            }, success = { i: Int, list: List<JSONObject> ->
                runOnUiThread {
                    populateGrid(list)
                }
            })
        } else {
            showGridStatus(error = true, errorMessage = "Invalid address:\n${APIClient.address}")
        }
    }

    private fun initializePreferences() {
        preferences = PreferenceManager.getDefaultSharedPreferences(this)
        prefsListener =
            { prefs, key ->
                when (key) {
                    "preferred-address" -> {
                        APIClient.address = prefs.getString("preferred-address", null)
                    }
                    "fallback-address" -> {
                        // TODO
                    }
                    "cache-size" -> {
                        ClientManager.cacheSize.postValue(
                            prefs.getInt(
                                "cache-size",
                                DEFAULT_CACHE_SIZE
                            ).toLong()
                        )
                    }
                }
            }
        preferences.registerOnSharedPreferenceChangeListener(prefsListener)
    }

    override fun onDestroy() {
        ClientManager.release()

        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.search_toolbar_menu, menu)
        return true
    }

    /**
     * Initializes the views for this activity
     */
    private fun initializeViews() {
        grid = findViewById(R.id.grid)
        gridProgress = findViewById(R.id.gridProgress)
        gridErrorText = findViewById(R.id.gridErrorText)
        searchText = findViewById(R.id.searchText)
        searchButton = findViewById(R.id.searchButton)
        gridErrorIcon = findViewById(R.id.gridErrorIcon)

        setSupportActionBar(findViewById(R.id.searchToolbar))

        gridErrorText.gravity = Gravity.CENTER
        gridErrorText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)

        initializeListeners()
    }

    private fun showGridStatus(
        progress: Boolean = false,
        error: Boolean = false,
        errorMessage: String? = null
    ) {
        gridProgress.visibility = if (progress) View.VISIBLE else View.GONE

        gridErrorText.visibility = if (error) View.VISIBLE else View.GONE
        gridErrorIcon.visibility = if (error) View.VISIBLE else View.GONE
        if (error && errorMessage != null) gridErrorText.text = errorMessage
    }

    /**
     * Initializes the listeners for this activity
     */
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
                PREFERRED_THUMBNAIL_SIZE_DP.toFloat(),
                resources.displayMetrics
            ).toInt()
            thumbnailAdapter = ThumbnailAdapter(this@SearchActivity, model, span)

            grid.apply {
                layoutManager = GridLayoutManager(context, span)
                adapter = thumbnailAdapter
            }
        }
    }

    /**
     * Called when permissions have been granted/denied
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSIONS_READ_STORAGE_FOR_UPLOAD -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    userPickFileForUpload()
                }
            }
        }
    }

    /**
     * Prompts the user for a file to upload
     */
    private fun userPickFileForUpload() {
        var chooseFile = Intent(Intent.ACTION_GET_CONTENT)
        chooseFile.type = "*/*"
        chooseFile = Intent.createChooser(chooseFile, "Choose a file")
        startActivityForResult(chooseFile, PICK_UPLOAD_FILE_RESULT_CODE)
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

        if (requestCode == PICK_UPLOAD_FILE_RESULT_CODE) {
            if (resultCode == -1) { // ??? Magic number
                if (data != null) {
                    uploadContent(data.data!!)
                }
            }
        } else if (requestCode == SETTINGS_ACTIVIYY_RESULT_CODE) {
            ClientManager.cacheSize.value =
                preferences.getInt("cache-size", DEFAULT_CACHE_SIZE).toLong()

            val address = preferences.getString("preferred-address", null)
            if (APIClient.isAddressValid(address)) {
                APIClient.address = address
                model.pageData.value = ArrayList()

                showGridStatus(progress = true)

                APIClient.requestSearch(failure = {
                    it?.printStackTrace()
                    runOnUiThread {
                        showGridStatus(
                            error = true,
                            errorMessage = "Failed to connect to:\n${APIClient.address}"
                        )
                    }
                }, success = { _, pageData ->
                    populateGrid(pageData)
                })
            } else {
                model.pageData.value = ArrayList()
                showGridStatus(error = true, errorMessage = "Invalid address:\n$address")
            }
        }
    }

    private fun uploadContent(uri: Uri) {
        APIClient.uploadContent(
            contentResolver.openInputStream(uri)!!,
            contentResolver.getType(uri)!!,
            failure = {
                simpleAlert(this, "Failed to upload", "Unable to connect", "Ok") {}
            })
    }

    /**
     * Called when a menu option is selected
     */
    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.search_toolbar_settings -> {
                startActivityForResult(
                    Intent(this, SettingsActivity::class.java),
                    SETTINGS_ACTIVIYY_RESULT_CODE
                )
                true
            }
            R.id.search_toolbar_upload -> {
                attemptUploadAction()
                true
            }
            R.id.search_toolbar_tags -> {
                startActivity(Intent(this, TagsActivity::class.java))
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    /**
     * Performs permissions checks and attempts to request a file from the user
     */
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
                            PERMISSIONS_READ_STORAGE_FOR_UPLOAD
                        )
                    }.create().show()
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    PERMISSIONS_READ_STORAGE_FOR_UPLOAD
                )
            }
        } else {
            userPickFileForUpload()
        }
    }

    /**
     * Called when the user submits a search
     */
    fun onSearchSubmit(view: View) {
        hideKeyboard(view)

        showGridStatus(progress = true)

        APIClient.requestSearch(
            searchText.text.toString(),
            success = { code, data ->
                runOnUiThread {
                    showGridStatus()

                    populateGrid(data)
                }
            }, failure = {
                it?.printStackTrace()

                showGridStatus(
                    error = true,
                    errorMessage = "Failed to connect to:\n${APIClient.address}"
                )
            })
    }

    private fun populateGrid(newData: List<JSONObject>) {
        model.pageData.postValue(newData)

        runOnUiThread { showGridStatus(progress = false, error = false) }
    }

    /**
     * Scrolls the grid to the top
     */
    fun toTopOfGrid(view: View) {
        if ((grid.layoutManager as GridLayoutManager).findLastVisibleItemPosition() < 100)
            grid.smoothScrollToPosition(0)
        else
            grid.scrollToPosition(0)
    }

}
package com.example.menagerie

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
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

class SearchActivity : AppCompatActivity() {

    private lateinit var grid: RecyclerView
    private lateinit var gridProgress: ProgressBar
    private lateinit var gridErrorText: TextView
    private lateinit var gridErrorIcon: ImageView
    private lateinit var searchText: EditText
    private lateinit var searchButton: Button

    private lateinit var model: MenagerieViewModel

    private lateinit var preferences: SharedPreferences
    private lateinit var prefsListener: (SharedPreferences, String) -> Unit

    private var thumbnailAdapter: ThumbnailAdapter? = null
    private val data: MutableList<JSONObject> = mutableListOf()


    /**
     * Called when the activity is created
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        model = ViewModelProvider.AndroidViewModelFactory.getInstance(application)
            .create(MenagerieViewModel::class.java)

        preferences = PreferenceManager.getDefaultSharedPreferences(this)
        prefsListener =
            { prefs, key ->
                when (key) {
                    "preferred-address" -> {
                        val address = prefs.getString("preferred-address", null)
                        if (!address.isNullOrEmpty()) model.setAddress(address)
                    }
                    "fallback-address" -> {
                        // TODO
                    }
                    "cache-size" -> {
                        model.setCacheSize(prefs.getInt("cache-size", DEFAULT_CACHE_SIZE).toLong())
                    }
                }
            }
        preferences.registerOnSharedPreferenceChangeListener(prefsListener)

        model.setCacheSize(preferences.getInt("cache-size", DEFAULT_CACHE_SIZE).toLong())
        model.setCacheDir(cacheDir)
        val address = preferences.getString("preferred-address", null)
        if (!address.isNullOrEmpty()) model.setAddress(address)

        initializeViews()
        initializeListeners()

        if (model.readyToConnect()) {
            gridProgress.visibility = View.VISIBLE
            gridErrorText.visibility = View.GONE
            gridErrorIcon.visibility = View.GONE

            model.search(failure = { e: IOException? ->
                e?.printStackTrace()
                runOnUiThread {
                    gridProgress.visibility = View.GONE
                    gridErrorText.visibility = View.VISIBLE
                    gridErrorIcon.visibility = View.VISIBLE
                }
            }, success = { i: Int, list: List<JSONObject> ->
                runOnUiThread {
                    gridProgress.visibility = View.GONE
                    gridErrorText.visibility = View.GONE
                    gridErrorIcon.visibility = View.GONE
                }
            })
        }
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

        setSupportActionBar(findViewById(R.id.my_toolbar))

        gridErrorText.gravity = Gravity.CENTER
        gridErrorText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
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
                return
            }
        }
    }

    /**
     * Prompts the user for a file to upload
     */
    private fun userPickFileForUpload() {
        var chooseFile = Intent(Intent.ACTION_GET_CONTENT)
        println(preferences.getString("preferred-address", null))
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
                    val uri = data.data!!
                    model.upload(
                        contentResolver.openInputStream(uri)!!,
                        contentResolver.getType(uri)!!,
                        failure = {
                            simpleAlert(this, "Failed to upload", "Unable to connect", "Ok") {}
                        })
                }
            }
        }
    }

    /**
     * Called when a menu option is selected
     */
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
        model.search(searchText.text.toString(), success = { code, data ->
            runOnUiThread {
                populateGrid(data)
            }
        })
    }

    private fun populateGrid(newData: List<JSONObject>) {
        data.clear()
        data.addAll(newData)
        thumbnailAdapter?.notifyDataSetChanged()

        gridErrorText.visibility = View.GONE
        gridErrorIcon.visibility = View.GONE
        gridProgress.visibility = View.GONE
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
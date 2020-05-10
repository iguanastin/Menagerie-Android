package com.example.menagerie

import android.Manifest
import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
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
import android.widget.MultiAutoCompleteTextView.Tokenizer
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList


const val DEFAULT_CACHE_SIZE: Int = 128
const val PREFERRED_THUMBNAIL_SIZE_DP: Int = 125


class SearchActivity : AppCompatActivity() {

    private lateinit var grid: RecyclerView
    private lateinit var gridProgress: ProgressBar
    private lateinit var gridErrorText: TextView
    private lateinit var gridErrorIcon: ImageView
    private lateinit var searchText: MultiAutoCompleteTextView
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
        setContentView(R.layout.activity_search)

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

        if (model.pageData.value == null) {
            handleLockAndStart()
        } else {
            populateGrid(model.pageData.value!!)
        }
    }

    private fun handleLockAndStart() {
        if (preferences.getBoolean("lock-app", false)) {
            if (BiometricManager.from(this)
                    .canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS
            ) {
                promptBiometricAuth()
            } else {
                val km: KeyguardManager =
                    getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

                if (km.isKeyguardSecure) {
                    promptKeyguardAuth(km)
                } else {
                    simpleAlert(this, message = "No pass code or pattern is set") {
                        performSearch()
                    }
                }
            }
        } else {
            performSearch()
        }
    }

    private fun promptKeyguardAuth(km: KeyguardManager) {
        val authIntent: Intent = km.createConfirmDeviceCredentialIntent(
            "Authenticate",
            "Authenticate in order to access Menagerie"
        )
        startActivityForResult(
            authIntent,
            Codes.search_activity_result_authenticate.ordinal
        )
    }

    private fun promptBiometricAuth() {
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                simpleAlert(
                    this@SearchActivity,
                    title = "Fatal error while authenticating",
                    message = errString.toString()
                ) {
                    finish()
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                simpleAlert(
                    this@SearchActivity,
                    title = "Error",
                    message = "Unable to authenticate"
                ) {
                    finish()
                }
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                performSearch()
            }
        }

        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor, callback)

        biometricPrompt.authenticate(
            BiometricPrompt.PromptInfo.Builder().setTitle("Authenticate")
                .setDeviceCredentialAllowed(true).build()
        )
    }

    private fun performSearch() {
        model.pageData.value = emptyList()
        hideKeyboard(searchText)

        if (APIClient.isAddressValid(APIClient.address)) {
            showGridStatus(progress = true)

            APIClient.requestTags(success = { _, tags ->
                APIClient.tagCache.clear()
                for (tag in tags) {
                    APIClient.tagCache[tag.id] = tag
                }
                model.tagData.postValue(tags)

                APIClient.requestSearch(
                    terms = searchText.text.toString(),
                    failure = { e: IOException? ->
                        e?.printStackTrace()
                        runOnUiThread {
                            showGridStatus(
                                error = true,
                                errorMessage = "Failed to connect to:\n${APIClient.address}"
                            )
                        }
                    },
                    success = { _: Int, list: List<JSONObject> ->
                        runOnUiThread {
                            populateGrid(list)
                        }
                    })
            }, failure = { e ->
                e?.printStackTrace()
                runOnUiThread {
                    showGridStatus(
                        error = true,
                        errorMessage = "Failed to connect to:\n${APIClient.address}"
                    )
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

        showGridStatus()

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
        searchText.setTokenizer(object : Tokenizer {
            override fun findTokenEnd(text: CharSequence, cursor: Int): Int {
                var index = text.indexOf(' ', cursor + 1)
                if (index < 0) index = text.length
                return index
            }

            override fun findTokenStart(text: CharSequence, cursor: Int): Int {
                for (i in cursor - 1 downTo 0) {
                    if (text[i] == ' ') return i // TODO make " -" a token edge as well as "^-"
                }

                return 0
            }

            override fun terminateToken(text: CharSequence): CharSequence {
//                return if (text.endsWith(' ')) text else ("$text ")
                return text
            }

        })
        model.tagData.observe(this, androidx.lifecycle.Observer { tags ->
            if (tags != null) {
                val tagNames = mutableListOf<String>()
                tags.sortedByDescending { tag -> tag.frequency }.forEach(action = { tag ->
                    tagNames.add(tag.name)
                })

                runOnUiThread {
                    searchText.setAdapter(
                        ArrayAdapter(
                            this,
                            android.R.layout.simple_dropdown_item_1line,
                            tagNames.toTypedArray()
                        )
                    )
                }
            }
        })

        grid.onGlobalLayout {
            val span = grid.width / pixelsToDP(PREFERRED_THUMBNAIL_SIZE_DP)
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
            Codes.search_request_storage_permissions_for_upload.ordinal -> {
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
        startActivityForResult(
            chooseFile,
            Codes.search_activity_result_pick_file_for_upload.ordinal
        )
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

        when (requestCode) {
            Codes.search_activity_result_pick_file_for_upload.ordinal -> {
                if (resultCode == Activity.RESULT_OK) {
                    if (data != null) {
                        uploadContent(data.data!!)
                    }
                }
            }
            Codes.search_activity_result_settings_closed.ordinal -> {
                ClientManager.cacheSize.value =
                    preferences.getInt("cache-size", DEFAULT_CACHE_SIZE).toLong()

                val address = preferences.getString("preferred-address", null)
                if (APIClient.isAddressValid(address)) {
                    APIClient.address = address
                    model.pageData.value = ArrayList()

                    showGridStatus(progress = true)

                    performSearch()
                } else {
                    model.pageData.value = ArrayList()
                    showGridStatus(error = true, errorMessage = "Invalid address:\n$address")
                }
            }
            Codes.search_activity_result_authenticate.ordinal -> {
                if (resultCode == RESULT_OK) {
                    performSearch()
                } else {
                    simpleAlert(this, message = "Unable to authenticate") {
                        finish()
                    }
                }
            }
            Codes.search_activity_result_tags_list_search_tag.ordinal -> {
                if (resultCode == Activity.RESULT_OK) {
                    searchText.setText(data?.getStringExtra(TAGS_LIST_TAG_EXTRA_ID))
                    searchText.setSelection(searchText.text.length)
                    searchText.requestFocus()
                    searchButton.performClick()
                }
            }
            else -> {
                println("Unexpected requestCode: $requestCode")
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
                    Codes.search_activity_result_settings_closed.ordinal
                )
                true
            }
            R.id.search_toolbar_upload -> {
                attemptUploadAction()
                true
            }
            R.id.search_toolbar_tags -> {
                startActivityForResult(
                    Intent(this, TagsActivity::class.java),
                    Codes.search_activity_result_tags_list_search_tag.ordinal
                )
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
        requirePermissions(
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            "Storage permissions required for this operation",
            "In order to upload files, this app must be granted permission to read external storage",
            Codes.search_request_storage_permissions_for_upload.ordinal
        ) {
            userPickFileForUpload()
        }
    }

    /**
     * Called when the user submits a search
     */
    @Suppress("UNUSED_PARAMETER")
    fun onSearchSubmit(view: View) {
        performSearch()
    }

    private fun populateGrid(newData: List<JSONObject>) {
        model.pageData.postValue(newData)

        runOnUiThread { showGridStatus(progress = false, error = false) }
    }

    /**
     * Scrolls the grid to the top
     */
    @Suppress("UNUSED_PARAMETER")
    fun toTopOfGrid(view: View) {
        if ((grid.layoutManager as GridLayoutManager).findLastVisibleItemPosition() < 100)
            grid.smoothScrollToPosition(0)
        else
            grid.scrollToPosition(0)
    }

}
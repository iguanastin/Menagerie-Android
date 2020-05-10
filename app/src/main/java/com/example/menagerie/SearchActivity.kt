package com.example.menagerie

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.KeyguardManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import android.widget.MultiAutoCompleteTextView.Tokenizer
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import okio.IOException


const val DEFAULT_CACHE_SIZE: Int = 128
const val PREFERRED_THUMBNAIL_SIZE_DP: Int = 125


class SearchActivity : AppCompatActivity() {

    private lateinit var grid: RecyclerView
    private lateinit var gridErrorText: TextView
    private lateinit var gridErrorIcon: ImageView
    private lateinit var searchText: MultiAutoCompleteTextView
    private lateinit var searchButton: Button
    private lateinit var swipeRefresher: SwipeRefreshLayout
    private lateinit var pageIndexText: TextView

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
            displaySearchResults(
                model.pageData.value!!,
                model.page.value!!,
                model.search.value!!.pages
            )
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
                        search(ItemSearch())
                    }
                }
            }
        } else {
            search(ItemSearch())
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
                search(ItemSearch())
            }
        }

        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor, callback)

        biometricPrompt.authenticate(
            BiometricPrompt.PromptInfo.Builder().setTitle("Authenticate")
                .setDeviceCredentialAllowed(true).build()
        )
    }

    private fun search(
        search: ItemSearch,
        page: Int = 0,
        success: (() -> Unit)? = null,
        failure: ((e: IOException?) -> Unit)? = null
    ) {
        model.pageData.value = emptyList()
        model.page.value = page
        model.search.value = search
        hideKeyboard(searchText)
        grid.scrollToPosition(0)

        if (APIClient.isAddressValid(APIClient.address)) {
            showGridStatus(progress = true)

            APIClient.requestTags(success = { _, tags ->
                APIClient.tagCache.clear()
                for (tag in tags) {
                    APIClient.tagCache[tag.id] = tag
                }
                model.tagData.postValue(tags)

                search.request(page, success = { search, items ->
                    displaySearchResults(items, page, search.pages)

                    success?.invoke()
                }, failure = { _, e ->
                    e?.printStackTrace()
                    runOnUiThread {
                        showGridStatus(
                            error = true,
                            errorMessage = "Failed to connect to:\n${APIClient.address}"
                        )
                    }

                    failure?.invoke(e)
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
        gridErrorText = findViewById(R.id.gridErrorText)
        searchText = findViewById(R.id.searchText)
        searchButton = findViewById(R.id.searchButton)
        gridErrorIcon = findViewById(R.id.gridErrorIcon)
        swipeRefresher = findViewById(R.id.searchSwipeRefresh)
        pageIndexText = findViewById(R.id.pageIndexTextView)

        setSupportActionBar(findViewById(R.id.searchToolbar))

        swipeRefresher.setOnRefreshListener { searchButton.performClick() }

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
        swipeRefresher.isRefreshing = progress

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
                    if (i == 0 || text[i - 1] == ' ') return i
                    if (i > 0 && text[i - 1] == '-' && i - 1 == 0) return i
                    if (i > 1 && text[i - 1] == '-' && text[i - 2] == ' ') return i
                }

                return 0
            }

            override fun terminateToken(text: CharSequence): CharSequence {
                return if (text.endsWith(' ')) text else ("$text ")
//                return text
            }

        })
        model.tagData.observe(this, Observer { tags ->
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

                    search(ItemSearch())
                } else {
                    model.pageData.value = ArrayList()
                    showGridStatus(error = true, errorMessage = "Invalid address:\n$address")
                }
            }
            Codes.search_activity_result_authenticate.ordinal -> {
                if (resultCode == RESULT_OK) {
                    search(ItemSearch())
                } else {
                    simpleAlert(this, message = "Unable to authenticate") {
                        finish()
                    }
                }
            }
            Codes.search_activity_result_tags_list_search_tag.ordinal, Codes.preview_activity_result_search_tag.ordinal -> {
                if (resultCode == Activity.RESULT_OK) {
                    searchText.setText(data?.getStringExtra(TAG_NAME_EXTRA_ID))
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
        // TODO make upload progress

        APIClient.importContent(
            uri, contentResolver,
            success = {
                // TODO Track import status?
                simpleAlert(this, message = "Successfully uploaded file")
            },
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
    fun submitSearch(view: View) {
        // Push previous search and state onto stack
        model.searchStack.push(
            SearchState(
                model.search.value!!,
                model.page.value!!,
                (grid.layoutManager as GridLayoutManager).findLastCompletelyVisibleItemPosition()
            )
        )

        // TODO descending, ungroup
        search(ItemSearch(searchText.text.toString()))
    }

    @SuppressLint("SetTextI18n")
    private fun displaySearchResults(newData: List<Item>, page: Int, totalPages: Int) {
        runOnUiThread {
            model.pageData.value = newData

            pageIndexText.text = "${page + 1}/$totalPages"
            showGridStatus(progress = false, error = false)
        }
    }

    /**
     * Scrolls the grid to the top
     */
    fun toTopOfGrid(@Suppress("UNUSED_PARAMETER") view: View) {
        if ((grid.layoutManager as GridLayoutManager).findLastVisibleItemPosition() < 100)
            grid.smoothScrollToPosition(0)
        else
            grid.scrollToPosition(0)
    }

    fun choosePageIndex(@Suppress("UNUSED_PARAMETER") view: View) {
        val builder = AlertDialog.Builder(this)
        val text = EditText(builder.context).apply {
            setText((model.page.value!! + 1).toString())
            inputType = InputType.TYPE_NUMBER_FLAG_SIGNED
        }
        val listener = DialogInterface.OnClickListener { _, which ->
            if (which == AlertDialog.BUTTON_POSITIVE) {
                val page = (text.text.toString().toInt() - 1).coerceIn(0, model.search.value!!.pages - 1)
                search(model.search.value!!, page)
            }
        }
        builder.setView(text).setNegativeButton("Cancel", listener)
            .setPositiveButton("Go", listener).create().show()
    }

    override fun onBackPressed() {
        if (model.searchStack.empty()) {
            super.onBackPressed()
        } else {
            val state: SearchState = model.searchStack.pop()
            searchText.setText(state.search.terms)
            searchText.setSelection(searchText.text.length)

            search(state.search, state.page, success = {
                runOnUiThread {
                    grid.scrollToPosition(state.lastVisiblePosition)
                }
            })
        }
    }

}
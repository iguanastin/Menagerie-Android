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
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.*
import android.widget.MultiAutoCompleteTextView.Tokenizer
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import okio.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit


class SearchActivity : AppCompatActivity() {

    private lateinit var searchText: MultiAutoCompleteTextView
    private lateinit var searchButton: Button
    private lateinit var pageIndexText: TextView
    private lateinit var pager: ViewPager2

    private lateinit var model: SearchViewModel

    private lateinit var preferences: SharedPreferences
    private lateinit var prefsListener: (SharedPreferences, String) -> Unit


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
            runOnUiThread {
                applySearchResults(
                    model.pageData.value!!,
                    model.page.value!!,
                    model.search.value!!.pages
                )
            }
        }
    }

    override fun onPause() {
        super.onPause()

        if (preferences.getBoolean("disable-recents-thumbnail", false)) window.addFlags(
            WindowManager.LayoutParams.FLAG_SECURE
        )
    }

    override fun onResume() {
        super.onResume()

        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
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
                        initialSearch()
                    }
                }
            }
        } else {
            initialSearch()
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
                initialSearch()
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
        hideKeyboard(searchText)

        if (APIClient.isAddressValid(APIClient.address)) {
            APIClient.requestTags(success = { _, tags ->
                APIClient.tagCache.clear()
                for (tag in tags) {
                    APIClient.tagCache[tag.id] = tag
                }
                model.tagData.postValue(tags)

                search.request(page, success = { search, items ->
                    runOnUiThread {
                        model.search.value = search

                        applySearchResults(items, page, search.pages)
                    }

                    success?.invoke()
                }, failure = { _, e ->
                    e?.printStackTrace()
                    runOnUiThread {
                        model.pageData.value = emptyList()

//                        showGridStatus(
//                            error = true,
//                            errorMessage = "Failed to connect to:\n${APIClient.address}"
//                        )
                    }

                    failure?.invoke(e)
                })
            }, failure = { e ->
                e?.printStackTrace()
                runOnUiThread {
                    model.pageData.value = emptyList()

//                    showGridStatus(
//                        error = true,
//                        errorMessage = "Failed to connect to:\n${APIClient.address}"
//                    )
                }
            })
        } else {
//            showGridStatus(error = true, errorMessage = "Invalid address:\n${APIClient.address}")
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
        setSupportActionBar(findViewById(R.id.searchToolbar))

        searchText = findViewById(R.id.searchText)
        searchButton = findViewById(R.id.searchButton)
        pageIndexText = findViewById(R.id.pageIndexTextView)
        pager = findViewById(R.id.searchViewPager)

        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            @SuppressLint("SetTextI18n")
            override fun onPageSelected(position: Int) {
                pageIndexText.text = (position + 1).toString() + "/" + model.search.value!!.pages
                model.page.value = position
            }
        })
        pager.adapter = object : FragmentStateAdapter(supportFragmentManager, lifecycle) {
            override fun getItemCount(): Int = model.search.value?.pages ?: 0

            override fun createFragment(position: Int): Fragment =
                SearchPageFragment(model.search.value!!, position) { item, index ->
                    startActivityForResult(
                        Intent(this@SearchActivity, PreviewActivity::class.java).apply {
                            putExtra(PREVIEW_ITEM_EXTRA_ID, item)
                            putExtra(PREVIEW_SEARCH_EXTRA_ID, model.search.value)
                            putExtra(PREVIEW_PAGE_EXTRA_ID, model.page.value)
                            putExtra(PREVIEW_INDEX_IN_PAGE_EXTRA_ID, index)
                        }, Codes.preview_activity_result_search_tag.ordinal
                    )
                }
        }
        model.search.observe(this, Observer {
            pager.adapter = pager.adapter
        })

        initializeListeners()
    }

    /**
     * Initializes the listeners for this activity
     */
    private fun initializeListeners() {
        searchText.setOnEditorActionListener { _, actionId, key ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || key.keyCode == KeyEvent.KEYCODE_ENTER) {
                searchButton.performClick()
            }

            true
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
        val chooseFile = Intent(Intent.ACTION_GET_CONTENT)
        chooseFile.type = "*/*"
        chooseFile.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        startActivityForResult(
            Intent.createChooser(chooseFile, "Choose a file"),
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
                    if (data?.clipData != null) {
                        val clip = data.clipData!!
                        val count = clip.itemCount

                        var numFailed = 0
                        var numSucceeded = 0

                        for (i in 0 until count) {
                            val latch = CountDownLatch(1)
                            uploadContent(clip.getItemAt(i).uri,
                                success = {
                                    numSucceeded++
                                    latch.countDown()
                                },
                                failure = {
                                    numFailed++
                                    latch.countDown()
                                })
                            latch.await()
                        }

                        runOnUiThread { simpleAlert(this, message = "Uploaded $numSucceeded, failed $numFailed") }
                    } else if (data != null) {
                        uploadContent(
                            data.data!!,
                            success = {
                                runOnUiThread {
                                    simpleAlert(
                                        this,
                                        message = "Successfully uploaded file"
                                    )
                                }
                            },
                            failure = {
                                runOnUiThread {
                                    simpleAlert(
                                        this,
                                        "Failed to upload",
                                        "Unable to connect"
                                    )
                                }
                            })
                    }
                }
            }
            Codes.search_activity_result_settings_closed.ordinal -> {
                ClientManager.cacheSize.value =
                    preferences.getInt("cache-size", DEFAULT_CACHE_SIZE).toLong()

                val address = preferences.getString("preferred-address", null)
                if (APIClient.isAddressValid(address)) {
                    APIClient.address = address

                    initialSearch()
                } else {
                    model.pageData.value = emptyList()
                }
            }
            Codes.search_activity_result_authenticate.ordinal -> {
                if (resultCode == RESULT_OK) {
                    initialSearch()
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

    private fun initialSearch() {
        search(ItemSearch(), failure = {
            runOnUiThread { simpleAlert(this, message = it?.localizedMessage) }
        })
    }

    private fun uploadContent(
        uri: Uri,
        success: ((Int) -> Unit)? = null,
        failure: ((e: IOException?) -> Unit)? = null
    ) {
        // TODO Track import status?
        // TODO make upload progress

        APIClient.importContent(
            uri, contentResolver,
            success = success,
            failure = failure
        )
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
        if (model.search.value != null && model.page.value != null) model.searchStack.push(
            SearchState(model.search.value!!, model.page.value!!)
        )

        // TODO descending, ungroup
        search(ItemSearch(searchText.text.toString()))
    }

    @SuppressLint("SetTextI18n")
    private fun applySearchResults(newData: List<Item>, page: Int, totalPages: Int) {
        model.pageData.value = newData
        model.page.value = page

        pageIndexText.text = "${page + 1}/$totalPages"
    }

    fun choosePageIndex(@Suppress("UNUSED_PARAMETER") view: View) {
        val builder = AlertDialog.Builder(this)
        val spinner = NumberPicker(builder.context).apply {
            minValue = 1
            maxValue = model.search.value!!.pages
            value = model.page.value!! + 1
            wrapSelectorWheel = false
        }

        val listener = DialogInterface.OnClickListener { _, which ->
            if (which == AlertDialog.BUTTON_POSITIVE) {
                val page = (spinner.value - 1).coerceIn(0, model.search.value!!.pages - 1)
                pager.setCurrentItem(page, true)
            }
        }

        builder.setView(spinner).setPositiveButton("Go", listener).create().show()
    }

    override fun onBackPressed() {
        if (model.searchStack.empty()) {
            if (model.page.value != 0) {
                pager.setCurrentItem(0, true)
            } else {
                super.onBackPressed()
            }
        } else {
            val state: SearchState = model.searchStack.pop()
            searchText.setText(state.search.terms)
            searchText.setSelection(searchText.text.length)

            search(state.search, state.page)
        }
    }

}
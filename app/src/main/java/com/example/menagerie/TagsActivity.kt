package com.example.menagerie

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class TagsActivity : AppCompatActivity() {

    enum class Sort {
        Alphabetical,
        Frequency,
        Color,
        Id
    }

    private lateinit var recycler: RecyclerView
    private lateinit var searchText: EditText

    private var descending = false
    private var sortBy = Sort.Alphabetical
    private var filter = ""

    private var tags: List<Tag>? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tags)

        setSupportActionBar(findViewById(R.id.tags_toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        recycler = findViewById(R.id.tags_recycler_view)
        searchText = findViewById(R.id.tags_search_text)

        searchText.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || event?.keyCode == KeyEvent.KEYCODE_ENTER) {
                hideKeyboard(v)
                findViewById<ImageButton>(R.id.tags_search_button).performClick()
            }

            true
        }

        if (APIClient.isAddressValid(APIClient.address)) {
            APIClient.requestTags(failure = {
                it?.printStackTrace()
                simpleAlert(
                    this,
                    "Failed connection",
                    "Failed to connect to: ${APIClient.address}",
                    "Ok"
                ) {
                    runOnUiThread { finish() }
                }
            }, success = { _: Int, tags: List<Tag> ->
                this.tags = tags

                runOnUiThread {
                    recycler.layoutManager =
                        LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
                    recycler.adapter =
                        TagRecyclerAdapter(tags, allowSelecting = true, onClick = { tag ->
                            setResult(
                                RESULT_OK,
                                Intent().apply { putExtra(TAG_NAME_EXTRA_ID, tag.name) })
                            finish()
                        })

                    applyFiltering()
                }
            })
        } else {
            simpleAlert(this, "Invalid address", "Invalid address: ${APIClient.address}", "Ok") {
                runOnUiThread { finish() }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onBackPressed() {
        val adapter = recycler.adapter

        if (adapter != null && (adapter as TagRecyclerAdapter).selecting) {
            // TODO use a supportActionMode instead of this hack
            adapter.selecting = false
        } else if (sortBy != Sort.Alphabetical || descending || filter.isNotEmpty()) {
            sortBy = Sort.Alphabetical
            descending = false
            filter = ""
            searchText.setText(filter)

            applyFiltering()
        } else {
            super.onBackPressed()
        }
    }

    private fun applyFiltering() {
        val adapter = recycler.adapter as TagRecyclerAdapter

        adapter.tags.clear()
        if (tags == null) return
        tags?.forEach { tag ->
            if (tag.name.contains(filter, ignoreCase = true)) adapter.tags.add(tag)
        }

        if (descending) {
            when (sortBy) {
                Sort.Alphabetical -> {
                    adapter.tags.sortByDescending { tag -> tag.name }
                }
                Sort.Frequency -> {
                    adapter.tags.sortByDescending { tag -> tag.frequency }
                }
                Sort.Color -> {
                    adapter.tags.sortByDescending { tag -> tag.color }
                }
                Sort.Id -> {
                    adapter.tags.sortByDescending { tag -> tag.id }
                }
            }
        } else {
            when (sortBy) {
                Sort.Alphabetical -> {
                    adapter.tags.sortBy { tag -> tag.name }
                }
                Sort.Frequency -> {
                    adapter.tags.sortBy { tag -> tag.frequency }
                }
                Sort.Color -> {
                    adapter.tags.sortBy { tag -> tag.color }
                }
                Sort.Id -> {
                    adapter.tags.sortBy { tag -> tag.id }
                }
            }
        }
        adapter.notifyDataSetChanged()
    }

    fun orderClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        descending = !descending

        applyFiltering()
    }

    fun sortClicked(view: View) {
        val popup = PopupMenu(this, view)
        popup.inflate(R.menu.tags_sort_menu)
        popup.show()

        popup.setOnMenuItemClickListener { item ->
            sortBy = when (item?.itemId) {
                R.id.tags_sort_by_alphabet -> {
                    Sort.Alphabetical
                }
                R.id.tags_sort_by_frequency -> {
                    Sort.Frequency
                }
                R.id.tags_sort_by_color -> {
                    Sort.Color
                }
                R.id.tags_sort_by_id -> {
                    Sort.Id
                }
                else -> {
                    Sort.Alphabetical
                }
            }

            applyFiltering()

            true
        }

        applyFiltering()
    }

    fun searchClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        filter = searchText.text.toString()

        applyFiltering()
    }

}

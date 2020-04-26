package com.example.menagerie

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder


class MainActivity : AppCompatActivity() {

    lateinit var grid: RecyclerView
    lateinit var gridProgress: ProgressBar
    lateinit var searchText: EditText
    lateinit var searchButton: Button

    lateinit var address: String
    val data: MutableList<String> = mutableListOf()
    val thumbnailAdapter = ThumbnailAdapter(this@MainActivity, data)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportActionBar?.hide()

        initViews()
        initListeners()
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

        grid.apply {
            layoutManager =
                GridLayoutManager(context, context.resources.getInteger(R.integer.grid_span))
            adapter = thumbnailAdapter
        }

        address =
            "http://" + intent.getCharSequenceExtra(IP_ADDRESS) + ":" + intent.getCharSequenceExtra(
                PORT
            )

        search()
    }

    private fun initListeners() {
        // Hide or show toTopButton based on scroll position
        grid.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                findViewById<View>(R.id.toTopButton).visibility =
                    if ((recyclerView.layoutManager as GridLayoutManager).findFirstVisibleItemPosition() > 0) View.VISIBLE else View.GONE
            }
        })
        searchText.onSubmit { searchButton.performClick() }
        searchText.setOnFocusChangeListener { view: View, b: Boolean ->
            
        }
    }

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

        OkHttpClient().newCall(Request.Builder().url(url).build())
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
                            .setNeutralButton("Ok") { _: DialogInterface?, _: Int -> this@MainActivity.finish() }
                            .create().show()
                    }
                }
            })
    }

    fun onSearchClick(view: View) {
        search(searchText.text.toString())
    }

    fun toTopOfGrid(view: View) {
        if ((grid.layoutManager as GridLayoutManager).findLastVisibleItemPosition() < 100)
            grid.smoothScrollToPosition(0)
        else
            grid.scrollToPosition(0)
    }

    fun EditText.onSubmit(func: () -> Unit) {
        setOnEditorActionListener { v, actionId, _ ->

            if (actionId == EditorInfo.IME_ACTION_DONE) {
                (v.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(
                    v.windowToken,
                    0
                ) // Hide keyboard
                func()
            }

            true

        }
    }

}
package com.example.menagerie

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder


class SearchActivity : AppCompatActivity() {

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
        cache = Cache(applicationContext.cacheDir, 1024*1024*100)
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

        grid.onGlobalLayout {
            // TODO set span of grid and element size nicely
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
package com.example.menagerie

import android.content.Intent
import android.os.Bundle
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class TagsActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tags)

        setSupportActionBar(findViewById(R.id.tags_toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        recycler = findViewById(R.id.tags_recycler_view)

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
                runOnUiThread {
                    recycler.layoutManager =
                        LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
                    recycler.adapter = TagRecyclerAdapter(tags, allowSelecting = true, onClick = { tag ->
                        setResult(
                            RESULT_OK,
                            Intent().apply { putExtra(TAG_NAME_EXTRA_ID, tag.name) })
                        finish()
                    })
                    recycler.adapter?.notifyDataSetChanged()
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
        } else {
            super.onBackPressed()
        }
    }

}

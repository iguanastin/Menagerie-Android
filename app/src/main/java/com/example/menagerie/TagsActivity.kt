package com.example.menagerie

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class TagsActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.tags_activity)

        setSupportActionBar(findViewById(R.id.tags_toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (APIClient.isAddressValid(APIClient.address)) {
            APIClient.requestTags(failure = {
                it?.printStackTrace()
                simpleAlert(this, "Failed connection", "Failed to connect to: ${APIClient.address}", "Ok") {
                    runOnUiThread { finish() }
                }
            }, success = { _: Int, tags: List<Tag> ->
                runOnUiThread {
                    val recycler = findViewById<RecyclerView>(R.id.tags_recycler_view)
                    recycler.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
                    recycler.adapter = TagRecyclerAdapter(tags)
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
        finish()
        return true
    }

}

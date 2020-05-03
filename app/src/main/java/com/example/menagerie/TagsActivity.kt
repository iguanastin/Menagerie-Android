package com.example.menagerie

import android.app.Activity
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.view.marginBottom
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import java.lang.IllegalArgumentException

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
            }, success = { code: Int, tags: List<Tag> ->
                runOnUiThread {
                    val recycler = findViewById<RecyclerView>(R.id.tags_recycler_view)
                    recycler.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
                    recycler.adapter = TestAdapter(this, tags)
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

class TestAdapter(private val activity: Activity, private val tags: List<Tag>): RecyclerView.Adapter<TestAdapter.TestHolder>() {

    class TestHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TestHolder {
        val textView = TextView(parent.context)
        val padding: Int = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8f, activity.resources.displayMetrics).toInt()
        textView.setPadding(padding, padding, padding, padding)

        return TestHolder(textView)
    }

    override fun onBindViewHolder(holder: TestHolder, position: Int) {
        holder.textView.text = tags[position].name
        val color = tags[position].color
        try {
            holder.textView.setTextColor(Color.WHITE)
            if (!color.isNullOrEmpty()) holder.textView.setTextColor(Color.parseColor(color))
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }
    }

    override fun getItemCount(): Int {
        return tags.size
    }

}

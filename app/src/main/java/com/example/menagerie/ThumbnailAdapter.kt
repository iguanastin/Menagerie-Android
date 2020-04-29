package com.example.menagerie

import android.app.Activity
import android.graphics.ImageDecoder
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.core.view.marginLeft
import androidx.core.view.marginRight
import androidx.recyclerview.widget.RecyclerView
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min

class ThumbnailAdapter(private val activity: Activity, private val client: OkHttpClient, private val data: List<JSONObject>, private val span: Int) :
    RecyclerView.Adapter<ThumbnailAdapter.ViewHolder>() {

    class ViewHolder(val imageView: ImageView) : RecyclerView.ViewHolder(imageView) {
        var call: Call? = null
    }

    private val cache: MutableMap<Int, Drawable> = ConcurrentHashMap()


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.thumbnail_layout, parent, false) as ImageView
        val margin: Int = view.marginLeft + view.marginRight

        val size : Int = parent.width / span - margin
        view.layoutParams.width = size
        view.layoutParams.height = size

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.call?.cancel() // Cancel old request if possible

        holder.imageView.setImageDrawable(cache[data[position].getInt("id")]) // Retrieve any known image from cache
        holder.imageView.setOnClickListener {
            Toast.makeText(it.context, "" + data[position].getInt("id"), Toast.LENGTH_SHORT).show()
            // TODO
        }
        holder.imageView.setOnLongClickListener {
            Toast.makeText(it.context, data[position].getString("thumbnail"), Toast.LENGTH_SHORT).show()
            // TODO
            true
        }

        // New call if no cached thumbnail
        if (holder.imageView.drawable == null) {
            holder.call = client.newCall(Request.Builder().url(data[position].getString("thumbnail")).build())

            (holder.call as Call).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    e.printStackTrace()
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.code == 200) {
                        val bytes = response.body?.byteStream()
                        if (bytes != null) {
                            try {
                                val d = BitmapDrawable.createFromStream(bytes, null)
                                if (d != null) cache[data[position].getInt("id")] = d
                            } catch (e: ImageDecoder.DecodeException) {
                                e.printStackTrace()
                            }
                        }
                        activity.runOnUiThread {
                            if (response.code == 200) {
                                try {
                                    holder.imageView.setImageDrawable(cache[data[position].getInt("id")])
                                } catch (e: ImageDecoder.DecodeException) {
                                    cache.remove(data[position].getInt("id"))
                                    holder.imageView.setImageDrawable(null)
                                    e.printStackTrace()
                                }
                            }
                        }
                    }
                }

            })
        }
    }

    override fun getItemCount(): Int = data.size

    fun release() {
        cache.clear()
    }


}
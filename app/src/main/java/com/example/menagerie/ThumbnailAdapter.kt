package com.example.menagerie

import android.app.Activity
import android.graphics.ImageDecoder
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import okhttp3.*
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.Future

class ThumbnailAdapter(private val activity: Activity, private val data: List<String>) :
    RecyclerView.Adapter<ThumbnailAdapter.ViewHolder>() {

    class ViewHolder(val imageView: ImageView) : RecyclerView.ViewHolder(imageView) {
        var call: Call? = null
    }

    private val client: OkHttpClient = OkHttpClient()

    private val cache: MutableMap<String, Drawable> = ConcurrentHashMap()


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.thumbnail_layout, parent, false) as ImageView
        view.layoutParams.width =
            parent.width / parent.context.resources.getInteger(R.integer.grid_span) - 10
        view.layoutParams.height = view.layoutParams.width

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.call?.cancel() // Cancel old request if possible

        holder.imageView.setImageDrawable(cache[data[position]]) // Retrieve any known image from cache

        // New call if no cached thumbnail
        if (holder.imageView.drawable == null) {
            holder.call = client.newCall(Request.Builder().url(data[position]).build())

            (holder.call as Call).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    e.printStackTrace()
                }

                override fun onResponse(call: Call, response: Response) {
                    val bytes = response.body?.byteStream()
                    if (bytes != null) {
                        try {
                            val d = BitmapDrawable.createFromStream(bytes, null)
                            if (d != null) cache[data[position]] = d
                        } catch (e: ImageDecoder.DecodeException) {
                            e.printStackTrace()
                        }
                    }
                    activity.runOnUiThread {
                        if (response.code == 200) {
                            try {
                                holder.imageView.setImageDrawable(cache[data[position]])
                            } catch (e: ImageDecoder.DecodeException) {
                                cache.remove(data[position])
                                holder.imageView.setImageDrawable(null)
                                e.printStackTrace()
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
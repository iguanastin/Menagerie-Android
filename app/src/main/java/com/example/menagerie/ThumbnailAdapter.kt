package com.example.menagerie

import android.graphics.ImageDecoder
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.marginLeft
import androidx.core.view.marginRight
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import okhttp3.Call
import org.json.JSONObject

class ThumbnailAdapter(
    private val activity: AppCompatActivity,
    private val model: MenagerieViewModel,
    private val span: Int
) :
    RecyclerView.Adapter<ThumbnailAdapter.ViewHolder>() {

    private var pageData: List<JSONObject>? = null

    init {
        model.getPageData().observe(activity, Observer { data ->
            pageData = ArrayList(data)
            notifyDataSetChanged()
        })
    }

    class ViewHolder(val imageView: ImageView) : RecyclerView.ViewHolder(imageView) {
        var call: Call? = null
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.thumbnail_layout, parent, false) as ImageView
        val margin: Int = view.marginLeft + view.marginRight

        val size: Int = parent.width / span - margin
        view.layoutParams.width = size
        view.layoutParams.height = size

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.call?.cancel() // Cancel old request if possible

        if (pageData == null) {
            // TODO display something useful instead of nothing
            holder.imageView.setImageDrawable(null)
            holder.imageView.setOnClickListener(null)
            holder.imageView.setOnLongClickListener(null)
            return
        }

        holder.imageView.setImageDrawable(model.getThumbnailCache()[pageData!![position].getInt("id")]) // Retrieve any known image from cache
        holder.imageView.setOnClickListener {
            Toast.makeText(it.context, "" + pageData!![position].getInt("id"), Toast.LENGTH_SHORT)
                .show()
            // TODO
        }
        holder.imageView.setOnLongClickListener {
            Toast.makeText(
                it.context,
                pageData!![position].getString("thumbnail"),
                Toast.LENGTH_SHORT
            ).show()
            // TODO
            true
        }

        if (holder.imageView.drawable == null) {
            holder.call = model.requestImage(
                pageData!![position].getString("thumbnail"),
                pageData!![position].getInt("id"),
                success = { code, image ->
                    activity.runOnUiThread {
                        try {
                            holder.imageView.setImageDrawable(image)
                        } catch (e: ImageDecoder.DecodeException) {
                            model.badThumbnail(pageData!![position].getInt("id"))
                            holder.imageView.setImageDrawable(null)
                            e.printStackTrace()
                        }
                    }
                })
        }
    }

    override fun getItemCount(): Int = if (pageData == null) 0 else pageData!!.size


}
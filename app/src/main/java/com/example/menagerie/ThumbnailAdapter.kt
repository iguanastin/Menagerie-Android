package com.example.menagerie

import android.content.Intent
import android.graphics.ImageDecoder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.marginLeft
import androidx.core.view.marginRight
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import okhttp3.Call
import org.json.JSONObject
import kotlin.collections.ArrayList

class ThumbnailAdapter (
    private val activity: AppCompatActivity,
    private val model: SearchViewModel,
    private val span: Int
) :
    RecyclerView.Adapter<ThumbnailAdapter.ViewHolder>() {

    private var pageData: List<JSONObject>? = null

    init {
        model.pageData.observe(activity, Observer { data ->
            pageData = ArrayList(data)
            notifyDataSetChanged()
        })
    }

    class ViewHolder(view: ConstraintLayout) : RecyclerView.ViewHolder(view) {
        var call: Call? = null

        val imageView: ImageView = view.findViewById(R.id.thumbnailImageView)
        val groupIcon: ImageView = view.findViewById(R.id.groupIconView)
        val videoIcon: ImageView = view.findViewById(R.id.videoIconView)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_thumbnail, parent, false) as ConstraintLayout
        val margin: Int = view.marginLeft + view.marginRight

        val size: Int = parent.width / span - margin
        view.layoutParams.width = size
        view.layoutParams.height = size

        val holder = ViewHolder(view)
        holder.videoIcon.visibility = View.GONE
        holder.groupIcon.visibility = View.GONE

        return holder
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

        holder.videoIcon.visibility = View.GONE
        holder.groupIcon.visibility = View.GONE
        if (pageData!![position].getString("type") == "video") holder.videoIcon.visibility =
            View.VISIBLE
        else if (pageData!![position].getString("type") == "group") holder.groupIcon.visibility =
            View.VISIBLE

        holder.imageView.setImageDrawable(model.thumbnailCache[pageData!![position].getInt("id")]) // Retrieve any known image from cache
        holder.imageView.setOnClickListener {
            if (pageData!![position].getString("type") in arrayOf("image", "video")) {
                activity.startActivity(Intent(activity, PreviewActivity::class.java).apply {
                    putExtra(PREVIEW_ITEM_EXTRA_ID, Item.fromJson(pageData!![position]))
                })
            }
            // TODO
        }

        if (holder.imageView.drawable == null) {
            holder.call = APIClient.requestImage(
                    pageData!![position].getString("thumbnail"),
            success = { _, image ->
                activity.runOnUiThread {
                    try {
                        holder.imageView.setImageBitmap(image)
                        // TODO cache thumbnail
//                        pageData!![position].getInt("id")
                    } catch (e: ImageDecoder.DecodeException) {
                        holder.imageView.setImageDrawable(null)
                        e.printStackTrace()
                    }
                }
            })
        }
    }

    override fun getItemCount(): Int = if (pageData == null) 0 else pageData!!.size


}
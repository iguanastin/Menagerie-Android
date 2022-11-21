package com.example.menagerie

import android.graphics.ImageDecoder
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.marginLeft
import androidx.core.view.marginRight
import androidx.recyclerview.widget.RecyclerView
import okhttp3.Call


class ThumbnailAdapter(
    private val span: Int,
    private val onItemClick: ((item: Item, position: Int) -> Unit)? = null
) :
    RecyclerView.Adapter<ThumbnailAdapter.ViewHolder>() {

    private val handler: Handler = Handler(Looper.getMainLooper())
    var pageData: List<Item>? = null

    class ViewHolder(view: ConstraintLayout) : RecyclerView.ViewHolder(view) {
        var call: Call? = null

        val imageView: ImageView = view.findViewById(R.id.thumbnailImageView)
        val groupIcon: ImageView = view.findViewById(R.id.groupIconView)
        val videoIcon: ImageView = view.findViewById(R.id.videoIconView)
        val titleView: TextView = view.findViewById(R.id.titleTextView)
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
        holder.titleView.apply {
            isSelected = true
            visibility = View.GONE
        }

        if (onItemClick != null) {
            view.setOnClickListener {
                onItemClick.invoke(pageData!![holder.layoutPosition], holder.layoutPosition)
            }
        }

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

        val item = pageData!![position]
        holder.videoIcon.visibility = View.GONE
        holder.groupIcon.visibility = View.GONE
        holder.titleView.visibility = View.GONE
        if (item.type == "video") holder.videoIcon.visibility =
            View.VISIBLE
        else if (item.type == "group") {
            holder.groupIcon.visibility = View.VISIBLE
            holder.titleView.apply {
                visibility = View.VISIBLE
                text = item.title
            }
        }

        holder.imageView.setImageDrawable(null)
        holder.call = APIClient.requestImage(
            APIClient.address + item.thumbURL!!,
            success = { _, image ->
                handler.post {
                    try {
                        holder.imageView.setImageBitmap(image)
                    } catch (e: ImageDecoder.DecodeException) {
                        holder.imageView.setImageDrawable(null)
                        e.printStackTrace()
                    }
                }
            })
    }

    override fun getItemCount(): Int = if (pageData == null) 0 else pageData!!.size


}
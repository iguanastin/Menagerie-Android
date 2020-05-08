package com.example.menagerie

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView


class TagRecyclerAdapter(
    tags: List<Tag>,
    sort: Sort = Sort.Name,
    private val onRemoveTag: ((Tag) -> Unit)? = null
) :
    RecyclerView.Adapter<TagRecyclerAdapter.TagRecyclerHolder>() {

    enum class Sort {
        @Suppress("UNUSED")
        None,
        ID,
        Name,
        Color,
        Frequency,
    }

    val tags: MutableList<Tag> = ArrayList(tags).apply {
        when (sort) {
            Sort.ID -> sortBy { tag -> tag.id }
            Sort.Color -> sortBy { tag -> tag.color }
            Sort.Frequency -> sortBy { tag -> tag.frequency }
            Sort.Name -> sortBy { tag -> tag.name }
            else -> Unit
        }
    }

    class TagRecyclerHolder(view: View, private val adapter: TagRecyclerAdapter) :
        RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.tagNameTextView)
        var tag: Tag? = null

        init {
            val removeButton: ImageButton = view.findViewById(R.id.removeTagImageButton)

            if (adapter.onRemoveTag != null) {
                removeButton.setOnClickListener {
                    (adapter.onRemoveTag)(tag!!)
                }
            } else {
                removeButton.visibility = View.GONE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagRecyclerHolder {
        return TagRecyclerHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.layout_tag_view, parent, false),
            this
        )
    }

    override fun onBindViewHolder(holder: TagRecyclerHolder, position: Int) {
        holder.tag = tags[position]
        holder.textView.text = holder.tag!!.name
        val color = holder.tag!!.color

        holder.textView.setTextColor(Color.WHITE)
        if (!color.isNullOrEmpty()) {
            try {
                holder.textView.setTextColor(
                    Color.parseColor(
                        cssColorMap.getOrDefault(
                            color,
                            color
                        )
                    )
                )
            } catch (e: IllegalArgumentException) {
                println("Invalid color: " + holder.tag!!.color)
                e.printStackTrace()
            }
        }
    }

    override fun getItemCount(): Int {
        return tags.size
    }

}
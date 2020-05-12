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
    private val allowSelecting: Boolean = false,
    private val onRemoveTag: ((Tag) -> Unit)? = null,
    private val onClick: ((Tag) -> Unit)? = null
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

    private val selection: MutableList<Int> = mutableListOf()
    var selecting: Boolean = false
        set(value) {
            if (!value) {
                selection.clear()
                notifyDataSetChanged()
            }

            field = value
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
        val nameView: TextView = view.findViewById(R.id.tagNameTextView)
        val freqView: TextView = view.findViewById(R.id.tagFrequencyTextView)
        var tag: Tag? = null

        init {
            val removeButton: ImageButton = view.findViewById(R.id.removeTagImageButton)

            if (adapter.onRemoveTag != null) {
                removeButton.setOnClickListener {
                    adapter.onRemoveTag.invoke(tag!!)
                }
            } else {
                removeButton.visibility = View.GONE
            }

            if (adapter.onClick != null) view.setOnClickListener {
                if (adapter.selecting) {
                    if (adapter.selection.contains(layoutPosition)) {
                        adapter.selection.remove(layoutPosition)
                        view.styleSelected(false)

                        if (adapter.selection.isEmpty()) {
                            adapter.selecting = false
                        }
                    } else {
                        adapter.selection.add(layoutPosition)
                        view.styleSelected(true)
                    }
                } else {
                    adapter.onClick.invoke(tag!!)
                }
            }

            view.setOnLongClickListener {
                if (adapter.allowSelecting) {
                    adapter.selecting = !adapter.selecting
                    if (adapter.selecting) {
                        adapter.selection.add(layoutPosition)
                        view.styleSelected(true)
                    }
                }

                true
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagRecyclerHolder {
        return TagRecyclerHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.layout_tag, parent, false),
            this
        )
    }

    override fun onBindViewHolder(holder: TagRecyclerHolder, position: Int) {
        holder.tag = tags[position]
        holder.nameView.text = holder.tag!!.name
        val color = holder.tag!!.color

        holder.freqView.text = holder.tag!!.frequency.toString()

        holder.itemView.styleSelected(selection.contains(position))

        holder.nameView.setTextColor(Color.WHITE)
        if (!color.isNullOrEmpty()) {
            try {
                holder.nameView.setTextColor(
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
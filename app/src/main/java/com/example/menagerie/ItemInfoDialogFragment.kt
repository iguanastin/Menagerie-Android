package com.example.menagerie

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ItemInfoDialogFragment(val item: Item) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val layout = inflater.inflate(R.layout.fragment_item_info_dialog, container, false)

        val recycler: RecyclerView = layout.findViewById(R.id.itemInfoRecyclerView)
        recycler.adapter = ItemInfoAdapter(item)
        recycler.layoutManager = LinearLayoutManager(context)

        return layout
    }

}

private class ItemInfoAdapter(item: Item) :
    RecyclerView.Adapter<ItemInfoAdapter.ItemInfoRecyclerHolder>() {

    private val data: MutableList<String> = mutableListOf()

    init {
        data.add("ID: ${item.id}")
        data.add("Type: ${item.type}")
        data.add(
            "Added: ${DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(
                LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(item.added),
                    ZoneId.systemDefault()
                )
            )}"
        )
        data.add("Tags: ${item.tags.size}")

        if (item.filePath != null) data.add("Path: ${item.filePath}")
        if (item.md5 != null) data.add("MD5: ${item.md5}")
        if (item.elements != null) data.add("Elements: ${item.elements.size}")
        // TODO filesize
        // TODO image resolution
    }

    class ItemInfoRecyclerHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemInfoRecyclerHolder {
        return ItemInfoRecyclerHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.layout_item_info, parent, false) as TextView
        )
    }

    override fun onBindViewHolder(holder: ItemInfoRecyclerHolder, position: Int) {
        holder.textView.text = data[position]
    }

    override fun getItemCount(): Int {
        return data.size
    }

}
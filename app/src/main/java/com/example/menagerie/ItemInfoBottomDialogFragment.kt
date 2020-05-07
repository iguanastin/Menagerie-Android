package com.example.menagerie

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class ItemInfoBottomDialogFragment(val item: Item) : BottomSheetDialogFragment() {


    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.layout_item_info_bottom_fragment, container, false)

        view.findViewById<TextView>(R.id.itemInfoTextView1).text = "ID: ${item.id}"
        view.findViewById<TextView>(R.id.itemInfoTextView2).text = "Path: ${item.filePath}"
        view.findViewById<TextView>(R.id.itemInfoTextView3).text = "MD5: ${item.md5}"
        view.findViewById<TextView>(R.id.itemInfoTextView4).text = "Added: ${DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.ofInstant(
            Instant.ofEpochMilli(item.added), ZoneId.systemDefault()))}"

        return view
    }

}
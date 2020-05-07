package com.example.menagerie

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.InputFilter
import android.text.Spanned
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.AutoCompleteTextView
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.view.marginBottom
import androidx.core.view.setPadding
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class TagsBottomDialogFragment(val item: Item) : BottomSheetDialogFragment() {


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.layout_tags_bottom_fragment, container, false)

        val submit = view.findViewById<ImageButton>(R.id.tagsFragmentSendButton)
        val textEdit = view.findViewById<AutoCompleteTextView>(R.id.tagsFragmentTextEdit)
        val recycler = view.findViewById<RecyclerView>(R.id.tagsFragmentRecyclerView)

        submit.setOnClickListener {
            textEdit.text = null
        }

        textEdit.filters =
            arrayOf(InputFilter { source: CharSequence, _: Int, _: Int, _: Spanned, _: Int, _: Int ->
                source.replace(Regex("\\s+"), "_")
            })
        textEdit.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submit.performClick()
                true
            } else {
                false
            }
        }

        recycler.adapter = TagRecyclerAdapter(requireContext(), item.tags)
        recycler.layoutManager = LinearLayoutManager(context)

        return view
    }

}

class TagRecyclerAdapter(private val context: Context, private val tags: List<Tag>): RecyclerView.Adapter<TagRecyclerAdapter.TagRecyclerHolder>() {

    class TagRecyclerHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagRecyclerHolder {
        val textView = TextView(parent.context)
        val padding: Int = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8f, context.resources.displayMetrics).toInt()
        textView.setPadding(padding)

        return TagRecyclerHolder(textView)
    }

    override fun onBindViewHolder(holder: TagRecyclerHolder, position: Int) {
        holder.textView.text = tags[position].name
        val color = tags[position].color

        holder.textView.setTextColor(Color.WHITE)
        if (!color.isNullOrEmpty()) {
            try {
                holder.textView.setTextColor(Color.parseColor(cssColorMap.getOrDefault(color, color)))
            } catch (e: IllegalArgumentException) {
                println("Invalid color: " + tags[position].color)
                e.printStackTrace()
            }
        }
    }

    override fun getItemCount(): Int {
        return tags.size
    }

}
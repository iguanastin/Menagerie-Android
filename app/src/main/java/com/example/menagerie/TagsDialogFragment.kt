package com.example.menagerie

import android.os.Bundle
import android.text.InputFilter
import android.text.Spanned
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.AutoCompleteTextView
import android.widget.ImageButton
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class TagsDialogFragment(val item: Item, val onClick: ((tag: Tag) -> Unit)? = null) : BottomSheetDialogFragment() {


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.layout_tags_fragment, container, false)

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

        recycler.adapter = TagRecyclerAdapter(item.tags, onRemoveTag = {
            simpleAlert(requireContext(), message = "Not yet implemented")
        }, onClick = { tag ->
            onClick?.invoke(tag)
        })
        recycler.layoutManager = LinearLayoutManager(context)

        return view
    }

}

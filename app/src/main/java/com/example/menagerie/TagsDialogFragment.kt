package com.example.menagerie

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputFilter
import android.text.Spanned
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageButton
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class TagsDialogFragment(
    val item: Item,
    private val onClick: ((tag: Tag) -> Unit)? = null
) : BottomSheetDialogFragment() {

    private val handler: Handler = Handler(Looper.getMainLooper())


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_tags_dialog, container, false)

        val submit = view.findViewById<ImageButton>(R.id.tagsFragmentSendButton)
        val textEdit = view.findViewById<AutoCompleteTextView>(R.id.tagsFragmentTextEdit)
        val recycler = view.findViewById<RecyclerView>(R.id.tagsFragmentRecyclerView)

        submit.setOnClickListener {
            APIClient.requestEditTags(item, textEdit.text.toString(), success = { _, _ ->
                (recycler.adapter as TagRecyclerAdapter).tags.clear()
                (recycler.adapter as TagRecyclerAdapter).tags.addAll(item.tags)
                handler.post { recycler.adapter?.notifyDataSetChanged() }
            }, failure = { e ->
                e?.printStackTrace()
                handler.post {
                    Toast.makeText(context, "Failed to edit tags", Toast.LENGTH_SHORT).show()
                }
            })

            textEdit.text = null
        }

        textEdit.filters =
            arrayOf(InputFilter { source: CharSequence, _: Int, _: Int, _: Spanned, _: Int, _: Int ->
                source.replace(Regex("\\s+"), "_")
            })
        textEdit.setOnEditorActionListener { _, actionId, key ->
            if (actionId == EditorInfo.IME_ACTION_DONE || key.keyCode == KeyEvent.KEYCODE_ENTER) {
                submit.performClick()
                true
            } else {
                false
            }
        }
        val tagNames = ArrayList<String>()
        APIClient.tagCache.values.forEach { tag -> tagNames.add(tag.name) }
        textEdit.setAdapter(
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                tagNames.toTypedArray()
            )
        )

        recycler.adapter = TagRecyclerAdapter(item.tags, onRemoveTag = { tag ->
            APIClient.requestEditTags(item, "-" + tag.name, success = { _, _ ->
                (recycler.adapter as TagRecyclerAdapter).tags.clear()
                (recycler.adapter as TagRecyclerAdapter).tags.addAll(item.tags)
                handler.post { recycler.adapter?.notifyDataSetChanged() }
            }, failure = { e ->
                e?.printStackTrace()
                handler.post {
                    Toast.makeText(context, "Failed to remove tag: ${tag.name}", Toast.LENGTH_SHORT)
                        .show()
                }
            })
        }, onClick = { tag ->
            onClick?.invoke(tag)
        })
        recycler.layoutManager = LinearLayoutManager(context)

        return view
    }

}

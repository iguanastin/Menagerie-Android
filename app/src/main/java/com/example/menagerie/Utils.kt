package com.example.menagerie

import android.content.Context
import android.view.View
import android.view.ViewTreeObserver
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import java.io.File
import java.text.DecimalFormat
import kotlin.math.log10


fun hideKeyboard(v: View) {
    (v.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(
        v.windowToken,
        0
    )
}

fun EditText.onSubmit(func: () -> Unit) {
    setOnEditorActionListener { v, actionId, _ ->
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            hideKeyboard(v)
            func()
        }

        true
    }
}

fun View.onGlobalLayout(func: () -> Unit) {
    viewTreeObserver.addOnGlobalLayoutListener(object :
        ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            viewTreeObserver.removeOnGlobalLayoutListener(this)

            func()
        }
    })
}

fun getDirectorySize(file: File): Long {
    var size: Long = 0

    if (file.isDirectory) {
        for (f in file.listFiles()) {
            size += getDirectorySize(f)
        }
    } else {
        size += file.length()
    }

    return size
}

fun byteSizeToString(size: Long): String? {
    if (size <= 0) return "0 Bytes"
    val units = arrayOf("Bytes", "kB", "MB", "GB", "TB")
    val digitGroups =
        (log10(size.toDouble()) / log10(1024.0)).toInt()
    return DecimalFormat("#,##0.#").format(
        size / Math.pow(
            1024.0,
            digitGroups.toDouble()
        )
    ).toString() + " " + units[digitGroups]
}
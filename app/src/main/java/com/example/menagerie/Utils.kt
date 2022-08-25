package com.example.menagerie

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Color
import android.util.TypedValue
import android.view.View
import android.view.ViewTreeObserver
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.text.DecimalFormat
import java.util.*
import kotlin.math.log10


private val cssColors = "indianred:#cd5c5c\n" +
        "lightcoral:#f08080\n" +
        "salmon:#fa8072\n" +
        "darksalmon:#e9967a\n" +
        "lightsalmon:#ffa07a\n" +
        "crimson:#dc143c\n" +
        "red:#ff0000\n" +
        "firebrick:#b22222\n" +
        "darkred:#8b0000\n" +
        "pink:#ffc0cb\n" +
        "lightpink:#ffb6c1\n" +
        "hotpink:#ff69b4\n" +
        "deeppink:#ff1493\n" +
        "mediumvioletred:#c71585\n" +
        "palevioletred:#db7093\n" +
        "lightsalmon:#ffa07a\n" +
        "coral:#ff7f50\n" +
        "tomato:#ff6347\n" +
        "orangered:#ff4500\n" +
        "darkorange:#ff8c00\n" +
        "orange:#ffa500\n" +
        "gold:#ffd700\n" +
        "yellow:#ffff00\n" +
        "lightyellow:#ffffe0\n" +
        "lemonchiffon:#fffacd\n" +
        "lightgoldenrodyellow:#fafad2\n" +
        "papayawhip:#ffefd5\n" +
        "moccasin:#ffe4b5\n" +
        "peachpuff:#ffdab9\n" +
        "palegoldenrod:#eee8aa\n" +
        "khaki:#f0e68c\n" +
        "darkkhaki:#bdb76b\n" +
        "lavender:#e6e6fa\n" +
        "thistle:#d8bfd8\n" +
        "plum:#dda0dd\n" +
        "violet:#ee82ee\n" +
        "orchid:#da70d6\n" +
        "fuchsia:#ff00ff\n" +
        "magenta:#ff00ff\n" +
        "mediumorchid:#ba55d3\n" +
        "mediumpurple:#9370db\n" +
        "rebeccapurple:#663399\n" +
        "blueviolet:#8a2be2\n" +
        "darkviolet:#9400d3\n" +
        "darkorchid:#9932cc\n" +
        "darkmagenta:#8b008b\n" +
        "purple:#800080\n" +
        "indigo:#4b0082\n" +
        "slateblue:#6a5acd\n" +
        "darkslateblue:#483d8b\n" +
        "mediumslateblue:#7b68ee\n" +
        "greenyellow:#adff2f\n" +
        "chartreuse:#7fff00\n" +
        "lawngreen:#7cfc00\n" +
        "lime:#00ff00\n" +
        "limegreen:#32cd32\n" +
        "palegreen:#98fb98\n" +
        "lightgreen:#90ee90\n" +
        "mediumspringgreen:#00fa9a\n" +
        "springgreen:#00ff7f\n" +
        "mediumseagreen:#3cb371\n" +
        "seagreen:#2e8b57\n" +
        "forestgreen:#228b22\n" +
        "green:#008000\n" +
        "darkgreen:#006400\n" +
        "yellowgreen:#9acd32\n" +
        "olivedrab:#6b8e23\n" +
        "olive:#808000\n" +
        "darkolivegreen:#556b2f\n" +
        "mediumaquamarine:#66cdaa\n" +
        "darkseagreen:#8fbc8b\n" +
        "lightseagreen:#20b2aa\n" +
        "darkcyan:#008b8b\n" +
        "teal:#008080\n" +
        "aqua:#00ffff\n" +
        "cyan:#00ffff\n" +
        "lightcyan:#e0ffff\n" +
        "paleturquoise:#afeeee\n" +
        "aquamarine:#7fffd4\n" +
        "turquoise:#40e0d0\n" +
        "mediumturquoise:#48d1cc\n" +
        "darkturquoise:#00ced1\n" +
        "cadetblue:#5f9ea0\n" +
        "steelblue:#4682b4\n" +
        "lightsteelblue:#b0c4de\n" +
        "powderblue:#b0e0e6\n" +
        "lightblue:#add8e6\n" +
        "skyblue:#87ceeb\n" +
        "lightskyblue:#87cefa\n" +
        "deepskyblue:#00bfff\n" +
        "dodgerblue:#1e90ff\n" +
        "cornflowerblue:#6495ed\n" +
        "mediumslateblue:#7b68ee\n" +
        "royalblue:#4169e1\n" +
        "blue:#0000ff\n" +
        "mediumblue:#0000cd\n" +
        "darkblue:#00008b\n" +
        "navy:#000080\n" +
        "midnightblue:#191970\n" +
        "cornsilk:#fff8dc\n" +
        "blanchedalmond:#ffebcd\n" +
        "bisque:#ffe4c4\n" +
        "navajowhite:#ffdead\n" +
        "wheat:#f5deb3\n" +
        "burlywood:#deb887\n" +
        "tan:#d2b48c\n" +
        "rosybrown:#bc8f8f\n" +
        "sandybrown:#f4a460\n" +
        "goldenrod:#daa520\n" +
        "darkgoldenrod:#b8860b\n" +
        "peru:#cd853f\n" +
        "chocolate:#d2691e\n" +
        "saddlebrown:#8b4513\n" +
        "sienna:#a0522d\n" +
        "brown:#a52a2a\n" +
        "maroon:#800000\n" +
        "white:#ffffff\n" +
        "snow:#fffafa\n" +
        "honeydew:#f0fff0\n" +
        "mintcream:#f5fffa\n" +
        "azure:#f0ffff\n" +
        "aliceblue:#f0f8ff\n" +
        "ghostwhite:#f8f8ff\n" +
        "whitesmoke:#f5f5f5\n" +
        "seashell:#fff5ee\n" +
        "beige:#f5f5dc\n" +
        "oldlace:#fdf5e6\n" +
        "floralwhite:#fffaf0\n" +
        "ivory:#fffff0\n" +
        "antiquewhite:#faebd7\n" +
        "linen:#faf0e6\n" +
        "lavenderblush:#fff0f5\n" +
        "mistyrose:#ffe4e1\n" +
        "gainsboro:#dcdcdc\n" +
        "lightgray:#d3d3d3\n" +
        "silver:#c0c0c0\n" +
        "darkgray:#a9a9a9\n" +
        "gray:#808080\n" +
        "dimgray:#696969\n" +
        "lightslategray:#778899\n" +
        "slategray:#708090\n" +
        "darkslategray:#2f4f4f\n" +
        "black:#00000"

val cssColorMap: Map<String, String> by lazy {
    val map: MutableMap<String, String> = hashMapOf()

    Scanner(cssColors).forEach {
        map[it.substringBefore(":")] = it.substringAfter(":")
    }

    map
}


fun hideKeyboard(v: View) {
    (v.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(
        v.applicationWindowToken,
        0
    )
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

fun simpleAlert(
    context: Context,
    title: String? = "Alert",
    message: String?,
    button: String = "Ok",
    onDismiss: (() -> Unit)? = null
) {
    AlertDialog.Builder(context).setTitle(title).setMessage(message)
        .setNeutralButton(button) { _, _ -> onDismiss?.invoke() }.create().show()
}

fun Activity.requirePermissions(
    permissions: Array<String>,
    justificationTitle: String,
    justificationMessage: String,
    permissionGrantId: Int,
    success: () -> Unit
) {
    val needed: MutableList<String> = mutableListOf()
    var showRationale = false
    for (perm in permissions) {
        if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
            needed.add(perm)

            if (ActivityCompat.shouldShowRequestPermissionRationale(this, perm)) {
                showRationale = true
            }
        }
    }
    if (needed.isNotEmpty()) {
        if (showRationale) {
            simpleAlert(this, title = justificationTitle, message = justificationMessage) {
                ActivityCompat.requestPermissions(this, permissions, permissionGrantId)
            }
        } else {
            ActivityCompat.requestPermissions(this, needed.toTypedArray(), permissionGrantId)
        }
    } else {
        success()
    }
}

fun View.dpToPixels(pixels: Int): Int {
    return dpToPixels(resources, pixels)
}

fun Activity.dpToPixels(pixels: Int): Int {
    return dpToPixels(resources, pixels)
}

fun dpToPixels(resources: Resources, pixels: Int): Int {
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, pixels.toFloat(), resources.displayMetrics).toInt()
}

fun View.styleSelected(selected: Boolean) {
    scaleX = if (selected) 0.95f else 1f
    scaleY = if (selected) 0.95f else 1f
    backgroundTintList = if (selected) ColorStateList.valueOf(Color.parseColor("#8888AA")) else null
}

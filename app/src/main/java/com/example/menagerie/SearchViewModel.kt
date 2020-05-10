package com.example.menagerie

import android.graphics.drawable.Drawable
import android.webkit.MimeTypeMap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

class SearchViewModel : ViewModel() {

    val thumbnailCache: MutableMap<Int, Drawable> = ConcurrentHashMap<Int, Drawable>()

    val search: MutableLiveData<ItemSearch> = MutableLiveData()
    val page: MutableLiveData<Int> = MutableLiveData(0)
    val pageData: MutableLiveData<List<Item>> = MutableLiveData()
    val tagData: MutableLiveData<List<Tag>> = MutableLiveData()

    override fun onCleared() {
        thumbnailCache.clear()
        pageData.value = null
        tagData.value = null
        search.value = null

        super.onCleared()
    }

}
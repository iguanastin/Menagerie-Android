package com.example.menagerie

import android.graphics.ImageDecoder
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.View
import android.webkit.MimeTypeMap
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

class MenagerieViewModel : ViewModel() {

    private val thumbnailCache: MutableMap<Int, Drawable> = ConcurrentHashMap<Int, Drawable>()

    private val address: MutableLiveData<String> = MutableLiveData()
    private val tags: MutableLiveData<List<Tag>> = MutableLiveData()
    private val pageData: MutableLiveData<List<JSONObject>> = MutableLiveData()

    private var cacheSize: MutableLiveData<Long> = MutableLiveData(128)
    private var cacheDir: MutableLiveData<File> = MutableLiveData()
    private var cache: Cache? = null
    private var client: OkHttpClient? = null
        get() {
            if (field == null) {
                cache?.close()
                cache = null

                if (cacheDir.value != null) {
                    cache = Cache(
                        cacheDir.value!!,
                        1024 * 1024 * cacheSize.value!!
                    )
                    field = OkHttpClient.Builder().cache(cache).build()
                } else {
                    field = OkHttpClient()
                }
            }

            return field
        }

    fun getPageData(): LiveData<List<JSONObject>> {
        return pageData
    }

    fun getThumbnailCache(): Map<Int, Drawable> {
        return thumbnailCache
    }

    fun badThumbnail(id: Int) {
        thumbnailCache.remove(id)
    }

    fun getCacheSize(): LiveData<Long> {
        return cacheSize
    }

    fun setCacheSize(size: Long) {
        cacheSize.value = size
        closeClient()
    }

    fun getCacheDir(): LiveData<File> {
        return cacheDir
    }

    fun setCacheDir(dir: File) {
        cacheDir.value = dir
        closeClient()
    }

    fun getTags(): LiveData<List<Tag>> {
        return tags
    }

    fun getAddress(): LiveData<String> {
        return address
    }

    fun setAddress(addr: String): Boolean {
        if (addr == address.value) return true

        if (addr.isEmpty() || !Pattern.matches(
                "(http://)?[a-zA-Z0-9.\\-]+:[0-9]+",
                addr
            )
        ) return false

        val newAddr = if (!addr.startsWith("http://")) {
            "http://$addr"
        } else {
            addr
        }

        address.value = newAddr
        return true
    }

    fun readyToConnect(): Boolean {
        return !address.value.isNullOrEmpty()
    }

    private fun loadTags(
        success: ((code: Int) -> Unit)? = null,
        failure: ((e: IOException?) -> Unit)? = null
    ) {
        val temp: MutableList<Tag> = arrayListOf()
        tags.value = temp

        if (address.value == null) throw IOException("No address")

        client!!.newCall(Request.Builder().url("${address.value}/tags").build())
            .enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        if (it.isSuccessful) {
                            val root = JSONObject(response.body!!.string())

                            val items = root.getJSONArray("tags")
                            val count = items.length()
                            for (i in 0 until count) {
                                val json: JSONObject = items[i] as JSONObject

                                temp.add(tagFromJson(json)!!)
                            }

                            success?.invoke(it.code)
                        } else {
                            failure?.invoke(null)
                        }
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    failure?.invoke(e)
                }
            })
    }

    fun upload(
        stream: InputStream,
        mime: String,
        success: ((code: Int) -> Unit)? = null,
        failure: ((e: IOException?) -> Unit)? = null
    ) {
        val filename: String = URLEncoder.encode(
            System.currentTimeMillis().toString() + "." + MimeTypeMap.getSingleton()
                .getExtensionFromMimeType(mime), "UTF-8"
        ) // TODO get better filename

        val bytes: ByteArray = stream.readBytes() // TODO stream request body instead of copying

        client!!.newCall(
            Request.Builder()
                .post(bytes.toRequestBody(mime.toMediaType())).url(
                    "${address.value}/upload?filename=$filename"
                ).build()
        ).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                failure?.invoke(e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (it.isSuccessful) {
                        success?.invoke(it.code)
                    } else {
                        failure?.invoke(null)
                    }
                }
            }
        })
    }

    fun search(
        terms: String = "",
        page: Int = 0,
        descending: Boolean = true,
        ungroup: Boolean = false,
        success: ((code: Int, data: List<JSONObject>) -> Unit)? = null,
        failure: ((e: IOException?) -> Unit)? = null
    ) {
        var url = "${address.value}/search?page=$page&terms=" + URLEncoder.encode(terms, "UTF-8")
        if (descending) url += "&desc"
        if (ungroup) url += "&ungroup"

        client!!.newCall(Request.Builder().url(url).build())
            .enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        if (it.isSuccessful) {
                            val root = JSONObject(response.body!!.string())
                            val count = root.getInt("count")
                            val data = ArrayList<JSONObject>()

                            if (count > 0) {
                                val items = root.getJSONArray("items")
                                for (i in 0 until count) {
                                    val item: JSONObject = items[i] as JSONObject

                                    item.put(
                                        "thumbnail",
                                        address.value + item.getString("thumbnail")
                                    )
                                    if (item.has("file")) item.put(
                                        "file",
                                        address.value + item.getString("file")
                                    )

                                    data.add(item)
                                }
                            }

                            pageData.postValue(data)
                            success?.invoke(it.code, data)
                        } else {
                            failure?.invoke(null)
                        }
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    failure?.invoke(e)
                }
            })
    }

    fun requestImage(
        url: String,
        id: Int? = null,
        success: ((code: Int, image: Drawable) -> Unit)? = null,
        failure: ((e: IOException?) -> Unit)? = null
    ): Call {
        // TODO error checking
        val call = client!!.newCall(Request.Builder().url(url).build())

        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                failure?.invoke(e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (it.isSuccessful) {
                        val bytes: InputStream? = response.body?.byteStream()
                        if (bytes == null) {
                            failure?.invoke(null)
                        } else {
                            try {
                                val d = BitmapDrawable.createFromStream(bytes, null)

                                if (d != null && id != null) thumbnailCache[id] = d

                                success?.invoke(it.code, d)
                            } catch (e: ImageDecoder.DecodeException) {
                                failure?.invoke(e)
                            }
                        }
                    } else {
                        failure?.invoke(null)
                    }
                }
            }
        })

        return call
    }

    fun closeClient() {
        cache?.close()
        cache = null
        client = null
    }

    override fun onCleared() {
        closeClient()
        thumbnailCache.clear()

        super.onCleared()
    }

}

private fun tagFromJson(json: JSONObject): Tag? {
    if (!json.has("id") || !json.has("name")) return null

    val color: String? = if (json.has("color")) json.getString("color") else null
    val notes: Array<String>? = if (json.has("notes")) {
        val n = json.getJSONArray("notes")
        Array(n.length()) { n.getString(it) }
    } else null
    val frequency: Int? = if (json.has("frequency")) json.getInt("frequency") else null

    return Tag(json.getInt("id"), json.getString("name"), color, notes, frequency)
}

class Tag(
    val id: Int,
    val name: String,
    val color: String?,
    val notes: Array<String>?,
    val frequency: Int?
) {

    override fun equals(other: Any?): Boolean {
        return other is Tag && other.id == id
    }

    override fun hashCode(): Int {
        return Integer.hashCode(id)
    }

}

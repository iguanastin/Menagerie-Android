package com.example.menagerie

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.provider.OpenableColumns
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.URLEncoder
import java.util.regex.Pattern


object APIClient {

    var address: String? = null
        set(value) {
            if (value != field) {
                itemCache.clear()
                tagCache.clear()
            }

            if (!value.isNullOrEmpty()) {
                var addr = if (!value.startsWith("http://")) {
                    "http://$value"
                } else {
                    value
                }

                if (addr.endsWith("/")) addr = addr.substringBeforeLast('/')

                field = addr
            } else {
                field = value
            }
        }

    val tagCache = HashMap<Int, Tag>()
    val itemCache = HashMap<Int, Item>()


    fun isAddressValid(address: String?): Boolean {
        return !address.isNullOrEmpty() && Pattern.matches(
            "(http://)?[a-zA-Z0-9.\\-]+:[0-9]+/?",
            address
        )
    }

    fun requestSearch(
        terms: String = "",
        page: Int = 0,
        descending: Boolean = true,
        ungroup: Boolean = false,
        success: ((data: List<Item>, total: Int) -> Unit)? = null,
        failure: ((e: IOException?) -> Unit)? = null
    ) {
        var url = "$address/search?page=$page&terms=" + URLEncoder.encode(terms, "UTF-8")
        if (descending) url += "&desc"
        if (ungroup) url += "&ungroup"

        ClientManager.client!!.newCall(Request.Builder().url(url).build())
            .enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        if (it.isSuccessful) {
                            val root = JSONObject(response.body!!.string())
                            val count = root.getInt("count")
                            val data = ArrayList<Item>()

                            if (count > 0) {
                                val items = root.getJSONArray("items")
                                // Iterate all result items
                                for (i in 0 until count) {
                                    val json: JSONObject = items[i] as JSONObject

                                    // Get item from cache, or build it from json
                                    data.add(
                                        itemCache.getOrPut(
                                            json.getInt("id"),
                                            defaultValue = { Item.fromJson(json) })
                                            .updateFromJSON(json)
                                    )
                                }
                            }

                            success?.invoke(data, root.getInt("total"))
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

    fun requestTags(
        success: ((code: Int, tags: List<Tag>) -> Unit)? = null,
        failure: ((e: IOException?) -> Unit)? = null
    ): Call {
        val call =
            ClientManager.client!!.newCall(Request.Builder().url("$address/tags").build())
        call.enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (it.isSuccessful) {
                        val root = JSONObject(response.body!!.string())

                        val items = root.getJSONArray("tags")
                        val count = items.length()

                        val results: MutableList<Tag> = arrayListOf()
                        for (i in 0 until count) {
                            val json: JSONObject = items[i] as JSONObject

                            results.add(Tag.fromJson(json))
                        }

                        success?.invoke(it.code, results)
                    } else {
                        failure?.invoke(null)
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                failure?.invoke(e)
            }
        })

        return call
    }

    fun importContent(
        uri: Uri,
        contentResolver: ContentResolver,
        success: ((code: Int) -> Unit)? = null,
        failure: ((e: IOException?) -> Unit)? = null
    ): Call {
        val stream = contentResolver.openInputStream(uri)!!
        val mime = contentResolver.getType(uri)!!

        var filename: String? = null
        if (uri.scheme == "content") {
            contentResolver.query(uri, null, null, null, null)!!.use { c ->
                if (c.moveToFirst()) {
                    filename = c.getString(c.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                }
            }
        }
        if (filename == null) {
            filename = uri.path
            val cut = filename!!.lastIndexOf('/')
            if (cut != -1) {
                filename = filename!!.substring(cut + 1)
            }
        }
        filename = URLEncoder.encode(filename, "UTF-8")
        val url = "$address/upload?filename=$filename"

        val bytes: ByteArray = stream.readBytes() // TODO stream request body instead of copying

        val call = ClientManager.client!!.newCall(
            Request.Builder()
                .post(bytes.toRequestBody(mime.toMediaType())).url(url).build()
        )
        call.enqueue(object : Callback {
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

        return call
    }

    fun requestImage(
        url: String,
        success: ((code: Int, image: Bitmap) -> Unit)? = null,
        failure: ((e: IOException?) -> Unit)? = null
    ): Call? {
        val call = ClientManager.client!!.newCall(Request.Builder().url(url).build())

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
                                success?.invoke(it.code, BitmapFactory.decodeStream(bytes))
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

    fun requestFile(
        url: String,
        saveTo: File,
        success: ((code: Int, file: File) -> Unit)? = null,
        failure: ((e: IOException?) -> Unit)? = null
    ): Call? {
        val call = ClientManager.client!!.newCall(Request.Builder().url(url).build())

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
                            bytes.use { stream: InputStream ->
                                saveTo.outputStream()
                                    .use { fos: FileOutputStream -> stream.copyTo(fos) }
                            }

                            success?.invoke(it.code, saveTo)
                        }
                    } else {
                        failure?.invoke(null)
                    }
                }
            }
        })

        return call
    }

}
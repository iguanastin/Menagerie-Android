package com.example.menagerie

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.webkit.MimeTypeMap
import androidx.lifecycle.MutableLiveData
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
import java.util.regex.Pattern

object APIClient {

    var address: String? = null
        set(value) {
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
        success: ((code: Int, data: List<JSONObject>) -> Unit)? = null,
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
                            val data = ArrayList<JSONObject>()

                            if (count > 0) {
                                val items = root.getJSONArray("items")
                                for (i in 0 until count) {
                                    val item: JSONObject = items[i] as JSONObject

                                    item.put(
                                        "thumbnail",
                                        address + item.getString("thumbnail")
                                    )
                                    if (item.has("file")) item.put(
                                        "file",
                                        address + item.getString("file")
                                    )

                                    data.add(item)
                                }
                            }

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

    fun uploadContent(
        stream: InputStream,
        mime: String,
        success: ((code: Int) -> Unit)? = null,
        failure: ((e: IOException?) -> Unit)? = null
    ): Call {
        val filename: String = URLEncoder.encode(
            System.currentTimeMillis().toString() + "." + MimeTypeMap.getSingleton()
                .getExtensionFromMimeType(mime), "UTF-8"
        ) // TODO get better filename
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

}
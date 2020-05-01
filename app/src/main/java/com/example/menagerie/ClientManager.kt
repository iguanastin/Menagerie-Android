package com.example.menagerie

import androidx.lifecycle.MutableLiveData
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File

object ClientManager {

    val cacheDir: MutableLiveData<File> = MutableLiveData()
    val cacheSize: MutableLiveData<Long> = MutableLiveData(128)

    private var cache: Cache? = null
    var client: OkHttpClient? = null
        get() {
            if (field == null) {
                cache?.close()
                cache = null

                if (cacheDir.value != null && cacheSize.value?.takeIf { it > 0 } != null) {
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
        private set


    init {
        cacheSize.observeForever {
            closeClient()
        }
        cacheDir.observeForever {
            closeClient()
        }
    }


    fun closeClient() {
        cache?.close()
        client = null
    }

    fun release() {
        closeClient()
    }

}
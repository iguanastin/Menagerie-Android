package com.example.menagerie

import android.graphics.drawable.Drawable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class SearchViewModel : ViewModel() {

    val thumbnailCache: MutableMap<Int, Drawable> = ConcurrentHashMap<Int, Drawable>()

    val searchStack: Stack<SearchState> = Stack()
    val search: MutableLiveData<ItemSearch> = MutableLiveData()
    val page: MutableLiveData<Int> = MutableLiveData(0)
    val pageData: MutableLiveData<List<Item>> = MutableLiveData()
    val tagData: MutableLiveData<List<Tag>> = MutableLiveData()

    override fun onCleared() {
        thumbnailCache.clear()
        searchStack.clear()
        pageData.value = null
        tagData.value = null
        search.value = null

        super.onCleared()
    }

}
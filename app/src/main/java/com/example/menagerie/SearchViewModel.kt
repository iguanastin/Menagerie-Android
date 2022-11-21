package com.example.menagerie

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.util.*

class SearchViewModel : ViewModel() {

    val searchStack: Stack<SearchState> = Stack()
    val search: MutableLiveData<ItemSearch?> = MutableLiveData()
    val page: MutableLiveData<Int> = MutableLiveData(0)
    val pageData: MutableLiveData<List<Item>?> = MutableLiveData()
    val tagData: MutableLiveData<List<Tag>?> = MutableLiveData()

    override fun onCleared() {
        searchStack.clear()
        pageData.value = null
        tagData.value = null
        search.value = null

        super.onCleared()
    }

}
package com.example.menagerie

import java.io.IOException

class ItemSearch(val terms: String = "", val descending: Boolean = true, val ungroup: Boolean = false) {

    var total: Int = UNKNOWN_SEARCH_TOTAL
        private set


    fun request(page: Int = 0, success: ((search: ItemSearch, items: List<Item>) -> Unit)? = null, failure: ((search: ItemSearch, e: IOException?) -> Unit)? = null) {
        APIClient.requestSearch(terms, page, descending, ungroup, success = { data, total ->
            this.total = total
            // TODO cache pages
            success?.invoke(this, data)
        }, failure = { e ->
            failure?.invoke(this, e)
        })
    }

}
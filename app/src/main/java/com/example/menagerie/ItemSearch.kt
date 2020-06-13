package com.example.menagerie

import android.os.Parcel
import android.os.Parcelable
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

class ItemSearch(val terms: String = "", val descending: Boolean = true, val ungroup: Boolean = false): Parcelable {

    var total: Int = UNKNOWN_SEARCH_TOTAL
        private set
    var pageSize: Int = UNKNOWN_PAGE_SIZE
        private set
    var pages: Int = 1
        private set

    val pageCache: ConcurrentHashMap<Int, List<Item>> = ConcurrentHashMap()


    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readByte() != 0.toByte(),
        parcel.readByte() != 0.toByte()
    ) {
        total = parcel.readInt()
        pageSize = parcel.readInt()
        pages = parcel.readInt()
    }

    fun request(page: Int = 0, success: ((search: ItemSearch, items: List<Item>) -> Unit)? = null, failure: ((search: ItemSearch, e: IOException?) -> Unit)? = null) {
        APIClient.requestSearch(terms, page, descending, ungroup, success = { data, total, pageSize, pages ->
            this.total = total
            this.pageSize = pageSize
            this.pages = pages

            pageCache[page] = ArrayList(data)
            success?.invoke(this, data)
        }, failure = { e ->
            failure?.invoke(this, e)
        })
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(terms)
        parcel.writeByte(if (descending) 1 else 0)
        parcel.writeByte(if (ungroup) 1 else 0)
        parcel.writeInt(total)
        parcel.writeInt(pageSize)
        parcel.writeInt(pages)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ItemSearch> {
        override fun createFromParcel(parcel: Parcel): ItemSearch {
            return ItemSearch(parcel)
        }

        override fun newArray(size: Int): Array<ItemSearch?> {
            return arrayOfNulls(size)
        }
    }

}
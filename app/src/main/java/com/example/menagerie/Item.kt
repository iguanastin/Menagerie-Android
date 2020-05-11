package com.example.menagerie

import android.os.Parcel
import android.os.Parcelable
import org.json.JSONObject
import java.util.ArrayList

class Item(
    val id: Int,
    val type: String,
    val added: Long,
    val tags: ArrayList<Tag>,
    val md5: String? = null,
    var filePath: String? = null,
    val fileURL: String? = null,
    var title: String? = null,
    val elements: List<Int>? = null,
    val thumbURL: String? = null,
    var elementOf: Int? = null,
    var elementIndex: Int? = null
) : Parcelable {


    companion object CREATOR : Parcelable.Creator<Item> {

        const val VIDEO_TYPE = "video"
        const val IMAGE_TYPE = "image"
        const val GROUP_TYPE = "group"

        override fun createFromParcel(parcel: Parcel): Item {
            return Item(parcel)
        }

        override fun newArray(size: Int): Array<Item?> {
            return arrayOfNulls(size)
        }

        fun fromJson(json: JSONObject): Item {
            val id = json.getInt("id")
            val type = json.getString("type")
            val added = json.getLong("added")

            val tagarr = json.getJSONArray("tags")
            val tags = ArrayList<Tag>()
            for (i in 0 until tagarr.length()) {
                val d = tagarr.getInt(i)
                tags.add(APIClient.tagCache.getOrElse(d) { Tag(d, "unknown") })
            }
            tags.sortBy { tag -> tag.name }

            val md5 = if (json.has("md5")) json.getString("md5") else null
            val filePath = if (json.has("path")) json.getString("path") else null
            val fileURL = if (json.has("file")) json.getString("file") else null
            val title = if (json.has("title")) json.getString("title") else null

            var elements: MutableList<Int>? = null
            if (json.has("elements")) {
                elements = ArrayList()

                val arr = json.getJSONArray("elements")
                for (i in 0 until arr.length()) {
                    elements.add(arr.getInt(i))
                }
            }

            val thumbURL = if (json.has("thumbnail")) json.getString("thumbnail") else null

            val elementOf = if (json.has("element_of")) json.getInt("element_of") else null
            val elementIndex = if (json.has("element_index")) json.getInt("element_index") else null

            return Item(
                id,
                type,
                added,
                tags,
                md5 = md5,
                filePath = filePath,
                fileURL = fileURL,
                title = title,
                elements = elements,
                thumbURL = thumbURL,
                elementOf = elementOf,
                elementIndex = elementIndex
            )
        }
    }

    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readString()!!,
        parcel.readLong(),
        parcel.createTypedArrayList(Tag.CREATOR)!!,
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.createIntArray()?.toList(),
        parcel.readString(),
        parcel.readInt(),
        parcel.readInt()
    ) {
        if (elementOf != null && elementOf!! < 0) elementOf = null
        if (elementIndex != null && elementIndex!! < 0) elementIndex = null
    }

    fun updateFromJSON(json: JSONObject): Item {
        if (json.has("path")) filePath = json.getString("path")
        if (json.has("title")) title = json.getString("title")

        return this
    }

    override fun equals(other: Any?): Boolean {
        return other is Item && other.id == id
    }

    override fun hashCode(): Int {
        return Integer.hashCode(id)
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeString(type)
        parcel.writeLong(added)
        parcel.writeTypedList(tags)
        parcel.writeString(md5)
        parcel.writeString(filePath)
        parcel.writeString(fileURL)
        parcel.writeString(title)
        parcel.writeIntArray(elements?.toIntArray())
        parcel.writeString(thumbURL)
        parcel.writeInt(elementOf ?: -1)
        parcel.writeInt(elementIndex ?: -1)
    }

    override fun describeContents(): Int {
        return 0
    }

}
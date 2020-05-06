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
    val filePath: String? = null,
    val fileURL: String? = null,
    val title: String? = null,
    val elements: List<Item>? = null,
    val thumbURL: String? = null
) : Parcelable {


    companion object CREATOR : Parcelable.Creator<Item> {

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
            val elements = null // TODO elements
            val thumbURL = if (json.has("thumbnail")) json.getString("thumbnail") else null

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
                thumbURL = thumbURL
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
        parcel.createTypedArrayList(CREATOR),
        parcel.readString()
    )

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
        parcel.writeTypedList(elements)
        parcel.writeString(thumbURL)
    }

    override fun describeContents(): Int {
        return 0
    }

}
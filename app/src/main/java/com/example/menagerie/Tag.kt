package com.example.menagerie

import android.os.Parcel
import android.os.Parcelable
import org.json.JSONException
import org.json.JSONObject

class Tag(
    val id: Int,
    val name: String,
    val color: String? = null,
    val notes: Array<String>? = null,
    val frequency: Int? = null
): Parcelable {

    companion object CREATOR : Parcelable.Creator<Tag> {
        override fun createFromParcel(parcel: Parcel): Tag {
            return Tag(parcel)
        }

        override fun newArray(size: Int): Array<Tag?> {
            return arrayOfNulls(size)
        }

        fun fromJson(json: JSONObject): Tag {
            if (!json.has("id") || !json.has("name")) throw JSONException("Invalid json format. Expected 'id' and 'name' attributes")

            val color: String? = if (json.has("color")) json.getString("color") else null
            val notes: Array<String>? = if (json.has("notes")) {
                val n = json.getJSONArray("notes")
                Array(n.length()) { n.getString(it) }
            } else null
            val frequency: Int? = if (json.has("frequency")) json.getInt("frequency") else null

            return Tag(json.getInt("id"), json.getString("name"), color, notes, frequency)
        }
    }

    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readString()!!,
        parcel.readString(),
        parcel.createStringArray(),
        parcel.readValue(Int::class.java.classLoader) as? Int
    )

    override fun equals(other: Any?): Boolean {
        return other is Tag && other.id == id
    }

    override fun hashCode(): Int {
        return Integer.hashCode(id)
    }

    override fun toString(): String {
        return "Tag[$id: $name]"
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeString(name)
        parcel.writeString(color)
        parcel.writeStringArray(notes)
        parcel.writeValue(frequency)
    }

    override fun describeContents(): Int {
        return 0
    }

}

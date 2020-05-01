package com.example.menagerie

import org.json.JSONException
import org.json.JSONObject

class Tag(
    val id: Int,
    val name: String,
    val color: String?,
    val notes: Array<String>?,
    val frequency: Int?
) {

    companion object {
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

    override fun equals(other: Any?): Boolean {
        return other is Tag && other.id == id
    }

    override fun hashCode(): Int {
        return Integer.hashCode(id)
    }

}

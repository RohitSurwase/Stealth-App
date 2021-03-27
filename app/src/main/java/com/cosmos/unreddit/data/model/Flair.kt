package com.cosmos.unreddit.data.model

import android.os.Parcelable
import com.cosmos.unreddit.data.remote.api.reddit.model.RichText
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
class Flair : Parcelable {
    @IgnoredOnParcel
    private val _data = mutableListOf<Pair<String, FlairType>>()
    val data: List<Pair<String, FlairType>> get() = _data

    fun addData(resource: String, type: FlairType) {
        _data.add(Pair(resource, type))
    }

    fun isEmpty(): Boolean = _data.isEmpty()

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is Flair) {
            return false
        }
        return this._data == other._data
    }

    override fun hashCode(): Int {
        return _data.hashCode()
    }

    companion object {
        fun fromData(flairRichText: List<RichText>?, flair: String?): Flair {
            val redditFlair = Flair()

            if (!flairRichText.isNullOrEmpty()) {
                for (richText in flairRichText) {
                    if (!richText.t.isNullOrBlank()) {
                        redditFlair.addData(richText.t, FlairType.TEXT)
                    } else if (!richText.u.isNullOrEmpty()) {
                        redditFlair.addData(richText.u, FlairType.IMAGE)
                    }
                }
            } else if (!flair.isNullOrBlank()) {
                redditFlair.addData(flair, FlairType.TEXT)
            }

            return redditFlair
        }
    }

    enum class FlairType {
        TEXT, IMAGE
    }
}

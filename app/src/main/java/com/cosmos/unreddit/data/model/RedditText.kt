package com.cosmos.unreddit.data.model

import android.os.Parcelable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
class RedditText : Parcelable {
    @IgnoredOnParcel
    private val _blocks = mutableListOf<HtmlBlock>()
    val blocks: List<HtmlBlock> get() = _blocks

    fun addBlock(block: Block, type: HtmlBlock.BlockType) {
        _blocks.add(HtmlBlock(block, type))
    }

    fun isFirstBlockText(): Boolean {
        return isNotEmpty() && _blocks[0].type == HtmlBlock.BlockType.TEXT
    }

    fun isNotEmpty(): Boolean {
        return !isEmpty()
    }

    fun isEmpty(): Boolean {
        return _blocks.isEmpty()
    }
}

package com.cosmos.unreddit.util

import com.cosmos.unreddit.R
import com.cosmos.unreddit.util.extension.fitTo

object CommentUtil {

    fun getCommentIndicator(depth: Int?): Int? {
        if (depth == null || depth <= 0) return null

        val commentDepth = depth - 1

        return if (commentDepth in colorArray.indices) {
            colorArray[commentDepth]
        } else {
            colorArray[commentDepth fitTo colorArray.indices]
        }
    }

    private val colorArray = arrayOf(
        R.color.comment_indicator_1,
        R.color.comment_indicator_2,
        R.color.comment_indicator_3,
        R.color.comment_indicator_4,
        R.color.comment_indicator_5,
        R.color.comment_indicator_6,
        R.color.comment_indicator_7,
        R.color.comment_indicator_8,
    )
}

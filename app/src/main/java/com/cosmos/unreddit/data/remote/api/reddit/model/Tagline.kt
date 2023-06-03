package com.cosmos.unreddit.data.remote.api.reddit.model

data class Tagline(
    val created: Long,

    val edited: Long,

    val author: String,

    val distinguished: String,

    val score: Int,

    val scoreHidden: Boolean,

    val isSubmitter: Boolean,

    val stickied: Boolean,

    val awardings: List<Awarding>,

    val totalAwards: Int,

    val flairRichText: List<RichText>,

    val flair: String?
)

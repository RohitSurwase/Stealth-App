package com.cosmos.unreddit.data.local.mapper

import com.cosmos.unreddit.data.model.db.SubredditEntity
import com.cosmos.unreddit.data.remote.api.reddit.model.AboutChild
import com.cosmos.unreddit.data.remote.api.reddit.model.AboutData
import com.cosmos.unreddit.data.remote.api.reddit.model.Child
import com.cosmos.unreddit.data.remote.api.reddit.model.ChildType
import com.cosmos.unreddit.util.HtmlParser

object SubredditMapper {

    suspend fun dataToEntity(
        data: AboutData,
        htmlParser: HtmlParser = HtmlParser()
    ): SubredditEntity {
        with(data) {
            return SubredditEntity(
                wikiEnabled ?: false,
                displayName,
                getHeader(),
                title,
                getPrimaryColor(),
                activeUserCount,
                getIcon(),
                subscribers,
                quarantine ?: false,
                htmlParser.separateHtmlBlocks(publicDescriptionHtml),
                getKeyColor(),
                getBackgroundColor(),
                over18 ?: false,
                htmlParser.separateHtmlBlocks(descriptionHtml),
                url,
                getTimeInMillis()
            )
        }
    }

    suspend fun dataToEntities(data: List<Child>?): List<SubredditEntity> {
        val subredditList = mutableListOf<SubredditEntity>()

        val htmlParser = HtmlParser()

        data?.forEach {
            if (it.kind == ChildType.t5) {
                subredditList.add(dataToEntity((it as AboutChild).data, htmlParser))
            }
        }

        return subredditList
    }
}

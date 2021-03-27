package com.cosmos.unreddit.data.local.mapper

import com.cosmos.unreddit.data.model.User
import com.cosmos.unreddit.data.remote.api.reddit.model.AboutUserChild
import com.cosmos.unreddit.data.remote.api.reddit.model.AboutUserData
import com.cosmos.unreddit.data.remote.api.reddit.model.Child
import com.cosmos.unreddit.data.remote.api.reddit.model.ChildType

object UserMapper {

    fun dataToEntity(data: AboutUserData): User {
        with(data) {
            return User(
                isSuspended,
                name,
                subreddit?.title,
                subreddit?.over18 ?: false,
                iconImg,
                subreddit?.url,
                subreddit?.publicDescription,
                linkKarma,
                commentKarma,
                getTimeInMillis()
            )
        }
    }

    fun dataToEntities(data: List<Child>?): List<User> {
        val userList = mutableListOf<User>()

        data?.forEach {
            if (it.kind == ChildType.t2) {
                with((it as AboutUserChild).data) {
                    if (!isSuspended) {
                        userList.add(dataToEntity(this))
                    }
                }
            }
        }

        return userList
    }
}

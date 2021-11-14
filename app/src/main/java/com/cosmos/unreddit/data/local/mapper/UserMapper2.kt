package com.cosmos.unreddit.data.local.mapper

import com.cosmos.unreddit.data.model.User
import com.cosmos.unreddit.data.remote.api.reddit.model.AboutUserData
import com.cosmos.unreddit.di.DispatchersModule
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserMapper2 @Inject constructor(
    @DispatchersModule.DefaultDispatcher defaultDispatcher: CoroutineDispatcher
) : Mapper<AboutUserData, User>(defaultDispatcher) {

    override suspend fun toEntity(from: AboutUserData): User {
        with(from) {
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
}

package com.cosmos.unreddit.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.cosmos.unreddit.subreddit.Subscription
import com.cosmos.unreddit.user.History

@Database(
    entities = [Subscription::class, History::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class RedditDatabase : RoomDatabase() {

    abstract fun subscriptionDao(): SubscriptionDao

    abstract fun historyDao(): HistoryDao
}
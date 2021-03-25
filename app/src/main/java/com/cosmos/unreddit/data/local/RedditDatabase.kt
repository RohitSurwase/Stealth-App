package com.cosmos.unreddit.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.cosmos.unreddit.data.local.dao.HistoryDao
import com.cosmos.unreddit.data.local.dao.SubscriptionDao
import com.cosmos.unreddit.data.model.db.History
import com.cosmos.unreddit.data.model.db.Subscription

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
package com.cosmos.unreddit.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.cosmos.unreddit.data.local.dao.HistoryDao
import com.cosmos.unreddit.data.local.dao.ProfileDao
import com.cosmos.unreddit.data.local.dao.SubscriptionDao
import com.cosmos.unreddit.data.model.db.History
import com.cosmos.unreddit.data.model.db.Profile
import com.cosmos.unreddit.data.model.db.Subscription

@Database(
    entities = [Subscription::class, History::class, Profile::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class RedditDatabase : RoomDatabase() {

    abstract fun subscriptionDao(): SubscriptionDao

    abstract fun historyDao(): HistoryDao

    abstract fun profileDao(): ProfileDao

    class Callback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            insertDefaultProfile(db)
        }
    }

    companion object {
        private const val DEFAULT_PROFILE_ID = 1
        private const val DEFAULT_PROFILE_NAME = "Stealth"

        private fun insertDefaultProfile(database: SupportSQLiteDatabase) {
            database.execSQL("INSERT INTO profile (id, name) VALUES($DEFAULT_PROFILE_ID, '$DEFAULT_PROFILE_NAME')")
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `profile` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL)")
                insertDefaultProfile(database)

                database.execSQL("CREATE TABLE IF NOT EXISTS `new_subscription` (`name` TEXT NOT NULL COLLATE NOCASE, `time` INTEGER NOT NULL, `icon` TEXT, `profile_id` INTEGER DEFAULT $DEFAULT_PROFILE_ID NOT NULL, PRIMARY KEY(`name`, `profile_id`), FOREIGN KEY(`profile_id`) REFERENCES `profile`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                database.execSQL("INSERT INTO new_subscription (name, time, icon) SELECT name, time, icon FROM subscription")
                database.execSQL("DROP TABLE subscription")
                database.execSQL("ALTER TABLE new_subscription RENAME TO subscription")

                database.execSQL("CREATE TABLE IF NOT EXISTS `new_history` (`post_id` TEXT NOT NULL, `time` INTEGER NOT NULL, `profile_id` INTEGER DEFAULT $DEFAULT_PROFILE_ID NOT NULL, PRIMARY KEY(`post_id`, `profile_id`), FOREIGN KEY(`profile_id`) REFERENCES `profile`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                database.execSQL("INSERT INTO new_history (post_id, time) SELECT post_id, time FROM history")
                database.execSQL("DROP TABLE history")
                database.execSQL("ALTER TABLE new_history RENAME TO history")
            }
        }
    }
}

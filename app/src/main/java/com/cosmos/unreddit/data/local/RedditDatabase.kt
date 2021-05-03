package com.cosmos.unreddit.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.cosmos.unreddit.data.local.dao.CommentDao
import com.cosmos.unreddit.data.local.dao.HistoryDao
import com.cosmos.unreddit.data.local.dao.PostDao
import com.cosmos.unreddit.data.local.dao.ProfileDao
import com.cosmos.unreddit.data.local.dao.SubscriptionDao
import com.cosmos.unreddit.data.model.Comment
import com.cosmos.unreddit.data.model.db.History
import com.cosmos.unreddit.data.model.db.PostEntity
import com.cosmos.unreddit.data.model.db.Profile
import com.cosmos.unreddit.data.model.db.Subscription

@Database(
    entities = [
        Subscription::class,
        History::class,
        Profile::class,
        PostEntity::class,
        Comment.CommentEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class RedditDatabase : RoomDatabase() {

    abstract fun subscriptionDao(): SubscriptionDao

    abstract fun historyDao(): HistoryDao

    abstract fun profileDao(): ProfileDao

    abstract fun postDao(): PostDao

    abstract fun commentDao(): CommentDao

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

                database.execSQL("CREATE TABLE IF NOT EXISTS `post` (`id` TEXT NOT NULL, `subreddit` TEXT NOT NULL, `title` TEXT NOT NULL, `ratio` INTEGER NOT NULL, `total_awards` INTEGER NOT NULL, `oc` INTEGER NOT NULL, `score` TEXT NOT NULL, `type` INTEGER NOT NULL, `domain` TEXT NOT NULL, `self` INTEGER NOT NULL, `self_text_html` TEXT, `suggested_sorting` TEXT NOT NULL, `nsfw` INTEGER NOT NULL, `preview` TEXT, `spoiler` INTEGER NOT NULL, `archived` INTEGER NOT NULL, `locked` INTEGER NOT NULL, `poster_type` INTEGER NOT NULL, `author` TEXT NOT NULL, `comments_number` TEXT NOT NULL, `permalink` TEXT NOT NULL, `stickied` INTEGER NOT NULL, `url` TEXT NOT NULL, `created` INTEGER NOT NULL, `media_type` TEXT NOT NULL, `media_url` TEXT NOT NULL, `profile_id` INTEGER NOT NULL, PRIMARY KEY(`id`, `profile_id`), FOREIGN KEY(`profile_id`) REFERENCES `profile`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                database.execSQL("CREATE TABLE IF NOT EXISTS `comment` (`total_awards` INTEGER NOT NULL, `link_id` TEXT NOT NULL, `author` TEXT NOT NULL, `score` TEXT NOT NULL, `body_html` TEXT NOT NULL, `edited` INTEGER NOT NULL, `submitter` INTEGER NOT NULL, `stickied` INTEGER NOT NULL, `score_hidden` INTEGER NOT NULL, `permalink` TEXT NOT NULL, `id` TEXT NOT NULL, `created` INTEGER NOT NULL, `controversiality` INTEGER NOT NULL, `poster_type` INTEGER NOT NULL, `link_title` TEXT, `link_permalink` TEXT, `link_author` TEXT, `subreddit` TEXT NOT NULL, `name` TEXT NOT NULL, `profile_id` INTEGER NOT NULL, PRIMARY KEY(`name`, `profile_id`), FOREIGN KEY(`profile_id`) REFERENCES `profile`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            }
        }
    }
}

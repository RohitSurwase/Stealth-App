package com.cosmos.unreddit.di

import android.content.Context
import androidx.room.Room
import com.cosmos.unreddit.data.local.RedditDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object DatabaseModule {

    @Provides
    @Singleton
    fun provideRedditDatabase(@ApplicationContext context: Context): RedditDatabase {
        return Room.databaseBuilder(context, RedditDatabase::class.java, "reddit_db")
            .addCallback(RedditDatabase.Callback())
            .addMigrations(RedditDatabase.MIGRATION_1_2)
            .addMigrations(RedditDatabase.MIGRATION_2_3)
            .addMigrations(RedditDatabase.MIGRATION_3_4)
            .build()
    }
}

package com.callmenot.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.callmenot.app.data.local.CallMeNotDatabase
import com.callmenot.app.data.local.dao.BlacklistDao
import com.callmenot.app.data.local.dao.CallEventDao
import com.callmenot.app.data.local.dao.WhitelistDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS blacklist_entries (
                    id TEXT PRIMARY KEY NOT NULL,
                    displayName TEXT NOT NULL,
                    phoneNumber TEXT NOT NULL,
                    normalizedNumber TEXT NOT NULL,
                    reason TEXT,
                    createdAt INTEGER NOT NULL
                )
            """.trimIndent())
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_blacklist_entries_normalizedNumber ON blacklist_entries (normalizedNumber)")
        }
    }

    @Provides
    @Singleton
    fun provideCallMeNotDatabase(
        @ApplicationContext context: Context
    ): CallMeNotDatabase {
        return Room.databaseBuilder(
            context,
            CallMeNotDatabase::class.java,
            CallMeNotDatabase.DATABASE_NAME
        )
        .addMigrations(MIGRATION_1_2)
        .build()
    }

    @Provides
    @Singleton
    fun provideWhitelistDao(database: CallMeNotDatabase): WhitelistDao {
        return database.whitelistDao()
    }

    @Provides
    @Singleton
    fun provideCallEventDao(database: CallMeNotDatabase): CallEventDao {
        return database.callEventDao()
    }

    @Provides
    @Singleton
    fun provideBlacklistDao(database: CallMeNotDatabase): BlacklistDao {
        return database.blacklistDao()
    }
}

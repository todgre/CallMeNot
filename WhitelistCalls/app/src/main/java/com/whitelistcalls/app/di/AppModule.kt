package com.whitelistcalls.app.di

import android.content.Context
import androidx.room.Room
import com.whitelistcalls.app.data.local.WhitelistDatabase
import com.whitelistcalls.app.data.local.dao.CallEventDao
import com.whitelistcalls.app.data.local.dao.WhitelistDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideWhitelistDatabase(
        @ApplicationContext context: Context
    ): WhitelistDatabase {
        return Room.databaseBuilder(
            context,
            WhitelistDatabase::class.java,
            WhitelistDatabase.DATABASE_NAME
        ).build()
    }

    @Provides
    @Singleton
    fun provideWhitelistDao(database: WhitelistDatabase): WhitelistDao {
        return database.whitelistDao()
    }

    @Provides
    @Singleton
    fun provideCallEventDao(database: WhitelistDatabase): CallEventDao {
        return database.callEventDao()
    }
}

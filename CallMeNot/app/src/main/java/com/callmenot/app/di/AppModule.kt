package com.callmenot.app.di

import android.content.Context
import androidx.room.Room
import com.callmenot.app.data.local.CallMeNotDatabase
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

    @Provides
    @Singleton
    fun provideCallMeNotDatabase(
        @ApplicationContext context: Context
    ): CallMeNotDatabase {
        return Room.databaseBuilder(
            context,
            CallMeNotDatabase::class.java,
            CallMeNotDatabase.DATABASE_NAME
        ).build()
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
}

package com.callmenot.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.callmenot.app.data.local.dao.BlacklistDao
import com.callmenot.app.data.local.dao.CallEventDao
import com.callmenot.app.data.local.dao.WhitelistDao
import com.callmenot.app.data.local.entity.BlacklistEntry
import com.callmenot.app.data.local.entity.CallEvent
import com.callmenot.app.data.local.entity.WhitelistEntry

@Database(
    entities = [WhitelistEntry::class, CallEvent::class, BlacklistEntry::class],
    version = 2,
    exportSchema = true
)
abstract class CallMeNotDatabase : RoomDatabase() {
    abstract fun whitelistDao(): WhitelistDao
    abstract fun callEventDao(): CallEventDao
    abstract fun blacklistDao(): BlacklistDao
    
    companion object {
        const val DATABASE_NAME = "whitelist_database"
    }
}

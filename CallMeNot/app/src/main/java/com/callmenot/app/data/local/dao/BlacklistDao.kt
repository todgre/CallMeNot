package com.callmenot.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.callmenot.app.data.local.entity.BlacklistEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface BlacklistDao {
    
    @Query("SELECT * FROM blacklist_entries ORDER BY displayName ASC")
    fun getAllEntries(): Flow<List<BlacklistEntry>>
    
    @Query("SELECT * FROM blacklist_entries WHERE id = :id")
    suspend fun getEntryById(id: String): BlacklistEntry?
    
    @Query("SELECT EXISTS(SELECT 1 FROM blacklist_entries WHERE normalizedNumber = :normalizedNumber)")
    suspend fun isNumberBlacklisted(normalizedNumber: String): Boolean
    
    @Query("SELECT COUNT(*) FROM blacklist_entries")
    fun getEntryCount(): Flow<Int>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: BlacklistEntry)
    
    @Delete
    suspend fun delete(entry: BlacklistEntry)
    
    @Query("DELETE FROM blacklist_entries WHERE id = :id")
    suspend fun deleteById(id: String)
    
    @Query("DELETE FROM blacklist_entries WHERE normalizedNumber = :normalizedNumber")
    suspend fun deleteByNormalizedNumber(normalizedNumber: String)
    
    @Query("DELETE FROM blacklist_entries")
    suspend fun deleteAll()
}

package com.whitelistcalls.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.whitelistcalls.app.data.local.entity.WhitelistEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface WhitelistDao {
    
    @Query("SELECT * FROM whitelist_entries ORDER BY displayName ASC")
    fun getAllEntries(): Flow<List<WhitelistEntry>>
    
    @Query("SELECT * FROM whitelist_entries ORDER BY displayName ASC")
    suspend fun getAllEntriesList(): List<WhitelistEntry>
    
    @Query("SELECT * FROM whitelist_entries WHERE id = :id")
    suspend fun getEntryById(id: String): WhitelistEntry?
    
    @Query("SELECT * FROM whitelist_entries WHERE normalizedNumber = :normalizedNumber LIMIT 1")
    suspend fun getEntryByNormalizedNumber(normalizedNumber: String): WhitelistEntry?
    
    @Query("SELECT EXISTS(SELECT 1 FROM whitelist_entries WHERE normalizedNumber = :normalizedNumber)")
    suspend fun isNumberWhitelisted(normalizedNumber: String): Boolean
    
    @Query("SELECT normalizedNumber FROM whitelist_entries")
    suspend fun getAllNormalizedNumbers(): List<String>
    
    @Query("SELECT COUNT(*) FROM whitelist_entries")
    fun getEntryCount(): Flow<Int>
    
    @Query("SELECT * FROM whitelist_entries WHERE displayName LIKE '%' || :query || '%' OR phoneNumber LIKE '%' || :query || '%'")
    fun searchEntries(query: String): Flow<List<WhitelistEntry>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: WhitelistEntry)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<WhitelistEntry>)
    
    @Update
    suspend fun update(entry: WhitelistEntry)
    
    @Delete
    suspend fun delete(entry: WhitelistEntry)
    
    @Query("DELETE FROM whitelist_entries WHERE id = :id")
    suspend fun deleteById(id: String)
    
    @Query("DELETE FROM whitelist_entries")
    suspend fun deleteAll()
    
    @Query("SELECT * FROM whitelist_entries WHERE syncedAt IS NULL OR updatedAt > syncedAt")
    suspend fun getUnsyncedEntries(): List<WhitelistEntry>
    
    @Query("UPDATE whitelist_entries SET syncedAt = :syncedAt WHERE id = :id")
    suspend fun markAsSynced(id: String, syncedAt: Long)
}

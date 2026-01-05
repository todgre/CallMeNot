package com.callmenot.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.callmenot.app.data.local.entity.CallAction
import com.callmenot.app.data.local.entity.CallEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface CallEventDao {
    
    @Query("SELECT * FROM call_events ORDER BY timestamp DESC")
    fun getAllEvents(): Flow<List<CallEvent>>
    
    @Query("SELECT * FROM call_events ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentEvents(limit: Int): Flow<List<CallEvent>>
    
    @Query("SELECT * FROM call_events WHERE action = :action ORDER BY timestamp DESC")
    fun getEventsByAction(action: CallAction): Flow<List<CallEvent>>
    
    @Query("SELECT * FROM call_events WHERE timestamp >= :startOfDay AND timestamp < :endOfDay ORDER BY timestamp DESC")
    fun getEventsForDay(startOfDay: Long, endOfDay: Long): Flow<List<CallEvent>>
    
    @Query("SELECT COUNT(*) FROM call_events WHERE action = :action AND timestamp >= :startOfDay AND timestamp < :endOfDay")
    fun getEventCountForDay(action: CallAction, startOfDay: Long, endOfDay: Long): Flow<Int>
    
    @Query("SELECT COUNT(*) FROM call_events WHERE normalizedNumber = :normalizedNumber AND timestamp >= :sinceTimestamp")
    suspend fun getRecentCallCountFromNumber(normalizedNumber: String, sinceTimestamp: Long): Int
    
    @Query("SELECT EXISTS(SELECT 1 FROM call_events WHERE normalizedNumber = :normalizedNumber AND timestamp >= :sinceTimestamp)")
    suspend fun hasRecentCallFromNumber(normalizedNumber: String, sinceTimestamp: Long): Boolean
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: CallEvent)
    
    @Query("DELETE FROM call_events WHERE timestamp < :beforeTimestamp")
    suspend fun deleteOldEvents(beforeTimestamp: Long)
    
    @Query("DELETE FROM call_events")
    suspend fun deleteAll()
    
    @Query("SELECT * FROM call_events WHERE timestamp >= :sinceTimestamp ORDER BY timestamp DESC")
    fun getEventsSince(sinceTimestamp: Long): Flow<List<CallEvent>>
    
    @Query("SELECT * FROM call_events WHERE action = :action AND timestamp >= :sinceTimestamp ORDER BY timestamp DESC")
    fun getEventsByActionSince(action: CallAction, sinceTimestamp: Long): Flow<List<CallEvent>>
}

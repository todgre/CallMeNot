package com.callmenot.app.data.repository

import com.callmenot.app.data.local.dao.CallEventDao
import com.callmenot.app.data.local.entity.CallAction
import com.callmenot.app.data.local.entity.CallEvent
import com.callmenot.app.data.local.entity.CallReason
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CallEventRepository @Inject constructor(
    private val callEventDao: CallEventDao
) {
    fun getAllEvents(): Flow<List<CallEvent>> = callEventDao.getAllEvents()
    
    fun getRecentEvents(limit: Int = 50): Flow<List<CallEvent>> = callEventDao.getRecentEvents(limit)
    
    fun getTodayBlockedCount(): Flow<Int> {
        val (start, end) = getTodayBounds()
        return callEventDao.getEventCountForDay(CallAction.BLOCKED, start, end)
    }
    
    fun getTodayAllowedCount(): Flow<Int> {
        val (start, end) = getTodayBounds()
        return callEventDao.getEventCountForDay(CallAction.ALLOWED, start, end)
    }
    
    suspend fun hasRecentCallFromNumber(normalizedNumber: String, withinMinutes: Int): Boolean {
        val sinceTimestamp = System.currentTimeMillis() - (withinMinutes * 60 * 1000L)
        return callEventDao.hasRecentCallFromNumber(normalizedNumber, sinceTimestamp)
    }
    
    suspend fun getRecentCallCount(normalizedNumber: String, withinMinutes: Int): Int {
        val sinceTimestamp = System.currentTimeMillis() - (withinMinutes * 60 * 1000L)
        return callEventDao.getRecentCallCountFromNumber(normalizedNumber, sinceTimestamp)
    }
    
    suspend fun logCallEvent(
        phoneNumber: String?,
        normalizedNumber: String?,
        displayName: String?,
        action: CallAction,
        reason: CallReason,
        matchedWhitelistId: String? = null,
        isPrivateNumber: Boolean = false
    ) {
        val event = CallEvent(
            id = UUID.randomUUID().toString(),
            phoneNumber = phoneNumber,
            normalizedNumber = normalizedNumber,
            displayName = displayName,
            action = action,
            reason = reason,
            matchedWhitelistId = matchedWhitelistId,
            isPrivateNumber = isPrivateNumber
        )
        callEventDao.insert(event)
    }
    
    suspend fun cleanupOldEvents(daysToKeep: Int = 30) {
        val cutoff = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000L)
        callEventDao.deleteOldEvents(cutoff)
    }
    
    private fun getTodayBounds(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis
        
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val endOfDay = calendar.timeInMillis
        
        return startOfDay to endOfDay
    }
}

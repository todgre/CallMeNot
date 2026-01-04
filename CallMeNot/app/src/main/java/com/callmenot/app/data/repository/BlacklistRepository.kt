package com.callmenot.app.data.repository

import com.callmenot.app.data.local.dao.BlacklistDao
import com.callmenot.app.data.local.entity.BlacklistEntry
import com.callmenot.app.util.PhoneNumberUtil
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlacklistRepository @Inject constructor(
    private val blacklistDao: BlacklistDao,
    private val phoneNumberUtil: PhoneNumberUtil
) {
    val entries: Flow<List<BlacklistEntry>> = blacklistDao.getAllEntries()
    val entryCount: Flow<Int> = blacklistDao.getEntryCount()
    
    suspend fun addEntry(
        displayName: String,
        phoneNumber: String,
        reason: String? = null
    ): BlacklistEntry {
        val normalizedNumber = phoneNumberUtil.normalize(phoneNumber)
        val entry = BlacklistEntry(
            id = UUID.randomUUID().toString(),
            displayName = displayName,
            phoneNumber = phoneNumber,
            normalizedNumber = normalizedNumber,
            reason = reason
        )
        blacklistDao.insert(entry)
        return entry
    }
    
    suspend fun isNumberBlacklisted(normalizedNumber: String): Boolean {
        return blacklistDao.isNumberBlacklisted(normalizedNumber)
    }
    
    suspend fun deleteEntry(entry: BlacklistEntry) {
        blacklistDao.delete(entry)
    }
    
    suspend fun deleteByNormalizedNumber(normalizedNumber: String) {
        blacklistDao.deleteByNormalizedNumber(normalizedNumber)
    }
}

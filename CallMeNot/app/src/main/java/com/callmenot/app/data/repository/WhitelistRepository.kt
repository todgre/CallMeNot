package com.callmenot.app.data.repository

import com.callmenot.app.data.local.dao.WhitelistDao
import com.callmenot.app.data.local.entity.WhitelistEntry
import com.callmenot.app.data.remote.FirestoreService
import com.callmenot.app.util.PhoneNumberUtil
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WhitelistRepository @Inject constructor(
    private val whitelistDao: WhitelistDao,
    private val firestoreService: FirestoreService,
    private val phoneNumberUtil: PhoneNumberUtil
) {
    fun getAllEntries(): Flow<List<WhitelistEntry>> = whitelistDao.getAllEntries()
    
    fun getEntryCount(): Flow<Int> = whitelistDao.getEntryCount()
    
    fun searchEntries(query: String): Flow<List<WhitelistEntry>> = whitelistDao.searchEntries(query)
    
    suspend fun getEntryById(id: String): WhitelistEntry? = whitelistDao.getEntryById(id)
    
    suspend fun isNumberWhitelisted(phoneNumber: String): Boolean {
        val normalized = phoneNumberUtil.normalize(phoneNumber)
        return whitelistDao.isNumberWhitelisted(normalized)
    }
    
    suspend fun getAllNormalizedNumbers(): Set<String> = whitelistDao.getAllNormalizedNumbers().toSet()
    
    suspend fun addEntry(
        displayName: String,
        phoneNumber: String,
        contactId: String? = null,
        isEmergencyBypass: Boolean = false
    ): WhitelistEntry {
        val normalized = phoneNumberUtil.normalize(phoneNumber)
        val entry = WhitelistEntry(
            id = UUID.randomUUID().toString(),
            displayName = displayName,
            phoneNumber = phoneNumber,
            normalizedNumber = normalized,
            contactId = contactId,
            isEmergencyBypass = isEmergencyBypass
        )
        whitelistDao.insert(entry)
        return entry
    }
    
    suspend fun updateEntry(entry: WhitelistEntry) {
        val updated = entry.copy(updatedAt = System.currentTimeMillis())
        whitelistDao.update(updated)
    }
    
    suspend fun deleteEntry(entry: WhitelistEntry) {
        whitelistDao.delete(entry)
    }
    
    suspend fun deleteEntryById(id: String) {
        whitelistDao.deleteById(id)
    }
    
    suspend fun syncToCloud(userId: String) {
        val entries = whitelistDao.getAllEntriesList()
        firestoreService.syncWhitelist(userId, entries)
        entries.forEach { entry ->
            whitelistDao.markAsSynced(entry.id, System.currentTimeMillis())
        }
    }
    
    suspend fun syncFromCloud(userId: String) {
        val cloudEntries = firestoreService.getWhitelist(userId)
        val localEntries = whitelistDao.getAllEntriesList()
        
        val localMap = localEntries.associateBy { it.id }
        val cloudMap = cloudEntries.associateBy { it.id }
        
        val toInsert = mutableListOf<WhitelistEntry>()
        
        cloudMap.forEach { (id, cloudEntry) ->
            val localEntry = localMap[id]
            if (localEntry == null || cloudEntry.updatedAt > localEntry.updatedAt) {
                toInsert.add(cloudEntry.copy(syncedAt = System.currentTimeMillis()))
            }
        }
        
        if (toInsert.isNotEmpty()) {
            whitelistDao.insertAll(toInsert)
        }
    }
}

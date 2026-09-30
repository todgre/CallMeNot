package com.callmenot.app.data.remote

import com.callmenot.app.data.local.entity.WhitelistEntry
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cloud sync is not available in this app version.
 *
 * Keep the existing API so callers remain safe, but do not touch Firestore:
 * this preserves any existing cloud data and prevents a local-only list from
 * overwriting/deleting records in a configured Firebase project.
 */
@Singleton
class FirestoreService @Inject constructor() {

    val isAvailable: Boolean
        get() = false

    suspend fun syncWhitelist(userId: String, entries: List<WhitelistEntry>): Boolean = false

    suspend fun getWhitelist(userId: String): List<WhitelistEntry> = emptyList()

    suspend fun deleteWhitelistEntry(userId: String, entryId: String) = Unit

    suspend fun syncSettings(userId: String, settings: Map<String, Any>) = Unit

    suspend fun getSettings(userId: String): Map<String, Any>? = null
}
package com.callmenot.app.data.remote

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.callmenot.app.data.local.entity.WhitelistEntry
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreService @Inject constructor() {
    
    companion object {
        private const val TAG = "FirestoreService"
    }
    
    private val firestore: FirebaseFirestore? by lazy { 
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.w(TAG, "Firebase not configured, cloud sync disabled", e)
            null
        }
    }
    
    val isAvailable: Boolean
        get() = firestore != null
    
    private fun userCollection(userId: String) = firestore?.collection("users")?.document(userId)
    private fun whitelistCollection(userId: String) = userCollection(userId)?.collection("whitelist")
    private fun settingsDocument(userId: String) = userCollection(userId)?.collection("settings")?.document("preferences")
    
    suspend fun syncWhitelist(userId: String, entries: List<WhitelistEntry>) {
        val fs = firestore ?: return
        try {
            val batch = fs.batch()
            
            entries.forEach { entry ->
                val docRef = whitelistCollection(userId)?.document(entry.id) ?: return@forEach
                batch.set(docRef, entry.toMap(), SetOptions.merge())
            }
            
            batch.commit().await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync whitelist", e)
        }
    }
    
    suspend fun getWhitelist(userId: String): List<WhitelistEntry> {
        return try {
            val snapshot = whitelistCollection(userId)?.get()?.await() ?: return emptyList()
            snapshot.documents.mapNotNull { doc ->
                try {
                    WhitelistEntry(
                        id = doc.id,
                        displayName = doc.getString("displayName") ?: "",
                        phoneNumber = doc.getString("phoneNumber") ?: "",
                        normalizedNumber = doc.getString("normalizedNumber") ?: "",
                        contactId = doc.getString("contactId"),
                        isEmergencyBypass = doc.getBoolean("isEmergencyBypass") ?: false,
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                        updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                    )
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get whitelist", e)
            emptyList()
        }
    }
    
    suspend fun deleteWhitelistEntry(userId: String, entryId: String) {
        try {
            whitelistCollection(userId)?.document(entryId)?.delete()?.await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete whitelist entry", e)
        }
    }
    
    suspend fun syncSettings(userId: String, settings: Map<String, Any>) {
        try {
            settingsDocument(userId)?.set(settings, SetOptions.merge())?.await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync settings", e)
        }
    }
    
    suspend fun getSettings(userId: String): Map<String, Any>? {
        return try {
            val snapshot = settingsDocument(userId)?.get()?.await() ?: return null
            snapshot.data
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get settings", e)
            null
        }
    }
    
    private fun WhitelistEntry.toMap(): Map<String, Any?> = mapOf(
        "displayName" to displayName,
        "phoneNumber" to phoneNumber,
        "normalizedNumber" to normalizedNumber,
        "contactId" to contactId,
        "isEmergencyBypass" to isEmergencyBypass,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt
    )
}

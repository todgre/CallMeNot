package com.callmenot.app.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.callmenot.app.data.local.entity.WhitelistEntry
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreService @Inject constructor() {
    
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    
    private fun userCollection(userId: String) = firestore.collection("users").document(userId)
    private fun whitelistCollection(userId: String) = userCollection(userId).collection("whitelist")
    private fun settingsDocument(userId: String) = userCollection(userId).collection("settings").document("preferences")
    
    suspend fun syncWhitelist(userId: String, entries: List<WhitelistEntry>) {
        val batch = firestore.batch()
        
        entries.forEach { entry ->
            val docRef = whitelistCollection(userId).document(entry.id)
            batch.set(docRef, entry.toMap(), SetOptions.merge())
        }
        
        batch.commit().await()
    }
    
    suspend fun getWhitelist(userId: String): List<WhitelistEntry> {
        return try {
            val snapshot = whitelistCollection(userId).get().await()
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
            emptyList()
        }
    }
    
    suspend fun deleteWhitelistEntry(userId: String, entryId: String) {
        whitelistCollection(userId).document(entryId).delete().await()
    }
    
    suspend fun syncSettings(userId: String, settings: Map<String, Any>) {
        settingsDocument(userId).set(settings, SetOptions.merge()).await()
    }
    
    suspend fun getSettings(userId: String): Map<String, Any>? {
        return try {
            val snapshot = settingsDocument(userId).get().await()
            snapshot.data
        } catch (e: Exception) {
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

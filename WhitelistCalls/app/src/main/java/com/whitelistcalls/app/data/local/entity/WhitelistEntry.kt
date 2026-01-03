package com.whitelistcalls.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "whitelist_entries",
    indices = [Index(value = ["normalizedNumber"], unique = true)]
)
data class WhitelistEntry(
    @PrimaryKey
    val id: String,
    val displayName: String,
    val phoneNumber: String,
    val normalizedNumber: String,
    val contactId: String? = null,
    val isEmergencyBypass: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncedAt: Long? = null
)

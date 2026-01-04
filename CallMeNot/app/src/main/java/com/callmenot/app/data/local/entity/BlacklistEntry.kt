package com.callmenot.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "blacklist_entries",
    indices = [Index(value = ["normalizedNumber"], unique = true)]
)
data class BlacklistEntry(
    @PrimaryKey
    val id: String,
    val displayName: String,
    val phoneNumber: String,
    val normalizedNumber: String,
    val reason: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

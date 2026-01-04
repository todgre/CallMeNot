package com.callmenot.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "call_events")
data class CallEvent(
    @PrimaryKey
    val id: String,
    val phoneNumber: String?,
    val normalizedNumber: String?,
    val displayName: String?,
    val timestamp: Long = System.currentTimeMillis(),
    val action: CallAction,
    val reason: CallReason,
    val matchedWhitelistId: String? = null,
    val isPrivateNumber: Boolean = false
)

enum class CallAction {
    ALLOWED,
    BLOCKED
}

enum class CallReason {
    WHITELISTED,
    STARRED_CONTACT,
    KNOWN_CONTACT,
    RECENT_OUTGOING,
    EMERGENCY_BYPASS,
    NOT_WHITELISTED,
    BLACKLISTED,
    UNKNOWN_NUMBER_BLOCKED,
    SCREENING_DISABLED,
    SUBSCRIPTION_INACTIVE,
    SCHEDULE_INACTIVE
}

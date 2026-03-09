package com.callmenot.app.domain.usecase

import android.content.ContentResolver
import android.provider.CallLog
import com.callmenot.app.data.local.entity.CallReason
import com.callmenot.app.data.repository.BlacklistRepository
import com.callmenot.app.data.repository.CallEventRepository
import com.callmenot.app.data.repository.SettingsRepository
import com.callmenot.app.data.repository.WhitelistRepository
import com.callmenot.app.service.BillingManager
import com.callmenot.app.util.ContactsHelper
import com.callmenot.app.util.ScheduleHelper
import java.util.Calendar
import javax.inject.Inject

data class CallScreeningDecision(
    val shouldAllow: Boolean,
    val reason: CallReason,
    val matchedWhitelistId: String? = null
)

class EvaluateCallUseCase @Inject constructor(
    private val whitelistRepository: WhitelistRepository,
    private val blacklistRepository: BlacklistRepository,
    private val callEventRepository: CallEventRepository,
    private val settingsRepository: SettingsRepository,
    private val billingManager: BillingManager,
    private val contactsHelper: ContactsHelper,
    private val scheduleHelper: ScheduleHelper
) {
    suspend operator fun invoke(
        rawNumber: String?,
        normalizedNumber: String?,
        isPrivateNumber: Boolean
    ): CallScreeningDecision {
        
        val isSubscriptionActive = billingManager.isSubscriptionActive() || 
                                   settingsRepository.isTrialActive()
        if (!isSubscriptionActive) {
            return CallScreeningDecision(
                shouldAllow = true,
                reason = CallReason.SUBSCRIPTION_INACTIVE
            )
        }
        
        val settings = settingsRepository.getSettingsSnapshot()
        
        if (!settings.blockingEnabled) {
            return CallScreeningDecision(
                shouldAllow = true,
                reason = CallReason.SCREENING_DISABLED
            )
        }
        
        if (settings.scheduleEnabled && !scheduleHelper.isWithinSchedule(settings)) {
            return CallScreeningDecision(
                shouldAllow = true,
                reason = CallReason.SCHEDULE_INACTIVE
            )
        }
        
        if (isPrivateNumber) {
            return if (settings.blockUnknownNumbers) {
                CallScreeningDecision(
                    shouldAllow = false,
                    reason = CallReason.UNKNOWN_NUMBER_BLOCKED
                )
            } else {
                CallScreeningDecision(
                    shouldAllow = true,
                    reason = CallReason.SCREENING_DISABLED
                )
            }
        }
        
        if (normalizedNumber == null) {
            return CallScreeningDecision(
                shouldAllow = false,
                reason = CallReason.NOT_WHITELISTED
            )
        }
        
        if (blacklistRepository.isNumberBlacklisted(normalizedNumber)) {
            return CallScreeningDecision(
                shouldAllow = false,
                reason = CallReason.BLACKLISTED
            )
        }
        
        if (whitelistRepository.isNumberWhitelisted(normalizedNumber)) {
            return CallScreeningDecision(
                shouldAllow = true,
                reason = CallReason.WHITELISTED
            )
        }
        
        if (settings.allowStarredContacts && contactsHelper.isStarredContact(normalizedNumber)) {
            return CallScreeningDecision(
                shouldAllow = true,
                reason = CallReason.STARRED_CONTACT
            )
        }
        
        if (settings.allowAllContacts && contactsHelper.isKnownContact(normalizedNumber)) {
            return CallScreeningDecision(
                shouldAllow = true,
                reason = CallReason.KNOWN_CONTACT
            )
        }
        
        if (settings.emergencyBypassEnabled) {
            val recentCallCount = callEventRepository.getRecentCallCount(
                normalizedNumber = normalizedNumber,
                withinMinutes = settings.emergencyBypassMinutes
            )
            if (recentCallCount >= 1) {
                return CallScreeningDecision(
                    shouldAllow = true,
                    reason = CallReason.EMERGENCY_BYPASS
                )
            }
        }
        
        if (settings.allowRecentOutgoing) {
            if (contactsHelper.hasRecentOutgoingCall(normalizedNumber, settings.recentOutgoingDays)) {
                return CallScreeningDecision(
                    shouldAllow = true,
                    reason = CallReason.RECENT_OUTGOING
                )
            }
        }
        
        return CallScreeningDecision(
            shouldAllow = false,
            reason = CallReason.NOT_WHITELISTED
        )
    }
}

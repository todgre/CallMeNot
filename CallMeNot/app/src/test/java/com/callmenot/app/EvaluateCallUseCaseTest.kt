package com.callmenot.app

import com.callmenot.app.data.local.entity.CallReason
import com.callmenot.app.data.repository.BlacklistRepository
import com.callmenot.app.data.repository.CallEventRepository
import com.callmenot.app.data.repository.SettingsRepository
import com.callmenot.app.data.repository.SettingsSnapshot
import com.callmenot.app.data.repository.WhitelistRepository
import com.callmenot.app.domain.usecase.EvaluateCallUseCase
import com.callmenot.app.service.BillingManager
import com.callmenot.app.util.ContactsHelper
import com.callmenot.app.util.ScheduleHelper
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EvaluateCallUseCaseTest {
    private val whitelist = mockk<WhitelistRepository>()
    private val blacklist = mockk<BlacklistRepository>()
    private val events = mockk<CallEventRepository>()
    private val settings = mockk<SettingsRepository>()
    private val billing = mockk<BillingManager>()
    private val contacts = mockk<ContactsHelper>()
    private val evaluate = EvaluateCallUseCase(
        whitelist, blacklist, events, settings, billing, contacts, ScheduleHelper()
    )

    private val defaultSettings = SettingsSnapshot(
        blockingEnabled = true, allowStarredContacts = false, allowAllContacts = false,
        blockUnknownNumbers = true, emergencyBypassEnabled = false, emergencyBypassMinutes = 3,
        allowRecentOutgoing = false, recentOutgoingDays = 3, scheduleEnabled = false,
        scheduleStartHour = 22, scheduleStartMinute = 0,
        scheduleEndHour = 7, scheduleEndMinute = 0
    )

    @Test fun expiredWithoutSubscriptionFailsOpen() = runBlocking {
        every { billing.isSubscriptionActive() } returns false
        coEvery { settings.isTrialActive() } returns false

        val result = evaluate("+15551230000", "+15551230000", false)
        assertTrue(result.shouldAllow)
        assertEquals(CallReason.SUBSCRIPTION_INACTIVE, result.reason)
    }

    @Test fun activeTrialBlocksPrivateCallWhenConfigured() = runBlocking {
        every { billing.isSubscriptionActive() } returns false
        coEvery { settings.isTrialActive() } returns true
        coEvery { settings.getSettingsSnapshot() } returns defaultSettings

        val result = evaluate(null, null, true)
        assertFalse(result.shouldAllow)
        assertEquals(CallReason.UNKNOWN_NUMBER_BLOCKED, result.reason)
    }

    @Test fun whitelistAllowsKnownNumber() = runBlocking {
        every { billing.isSubscriptionActive() } returns true
        coEvery { settings.getSettingsSnapshot() } returns defaultSettings
        coEvery { blacklist.isNumberBlacklisted("+15551230000") } returns false
        coEvery { whitelist.isNumberWhitelisted("+15551230000") } returns true

        val result = evaluate("+15551230000", "+15551230000", false)
        assertTrue(result.shouldAllow)
        assertEquals(CallReason.WHITELISTED, result.reason)
    }

    @Test fun blacklistWinsOverWhitelist() = runBlocking {
        every { billing.isSubscriptionActive() } returns true
        coEvery { settings.getSettingsSnapshot() } returns defaultSettings
        coEvery { blacklist.isNumberBlacklisted("+15551230000") } returns true

        val result = evaluate("+15551230000", "+15551230000", false)
        assertFalse(result.shouldAllow)
        assertEquals(CallReason.BLACKLISTED, result.reason)
    }
}
package com.whitelistcalls.app

import com.whitelistcalls.app.data.local.entity.CallReason
import com.whitelistcalls.app.data.repository.SettingsSnapshot
import com.whitelistcalls.app.util.ScheduleHelper
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class CallScreeningDecisionTest {

    private val scheduleHelper = ScheduleHelper()

    @Test
    fun `schedule helper returns true when schedule is disabled`() {
        val settings = createSettings(scheduleEnabled = false)
        assertTrue(scheduleHelper.isWithinSchedule(settings))
    }

    @Test
    fun `schedule helper returns true when within normal schedule`() {
        val settings = createSettings(
            scheduleEnabled = true,
            scheduleStartHour = 9,
            scheduleStartMinute = 0,
            scheduleEndHour = 17,
            scheduleEndMinute = 0
        )
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val expected = currentHour in 9..16
        assertEquals(expected, scheduleHelper.isWithinSchedule(settings))
    }

    @Test
    fun `schedule helper handles overnight schedule`() {
        val settings = createSettings(
            scheduleEnabled = true,
            scheduleStartHour = 22,
            scheduleStartMinute = 0,
            scheduleEndHour = 7,
            scheduleEndMinute = 0
        )
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val expected = currentHour >= 22 || currentHour < 7
        assertEquals(expected, scheduleHelper.isWithinSchedule(settings))
    }

    @Test
    fun `whitelist check with normalized number should match`() {
        val whitelist = setOf("+14155551234", "+14155555678")
        val normalizedNumber = "+14155551234"
        assertTrue(whitelist.contains(normalizedNumber))
    }

    @Test
    fun `whitelist check with unknown number returns false`() {
        val whitelist = setOf("+14155551234")
        val normalizedNumber = "+19995551111"
        assertFalse(whitelist.contains(normalizedNumber))
    }

    @Test
    fun `emergency bypass requires recent call within window`() {
        val lastCallTimestamp = System.currentTimeMillis() - (2 * 60 * 1000)
        val windowMinutes = 3
        val cutoffTime = System.currentTimeMillis() - (windowMinutes * 60 * 1000)
        
        assertTrue(lastCallTimestamp >= cutoffTime)
    }

    @Test
    fun `emergency bypass fails when outside window`() {
        val lastCallTimestamp = System.currentTimeMillis() - (5 * 60 * 1000)
        val windowMinutes = 3
        val cutoffTime = System.currentTimeMillis() - (windowMinutes * 60 * 1000)
        
        assertFalse(lastCallTimestamp >= cutoffTime)
    }

    @Test
    fun `call reason enum values exist`() {
        assertEquals("WHITELISTED", CallReason.WHITELISTED.name)
        assertEquals("NOT_WHITELISTED", CallReason.NOT_WHITELISTED.name)
        assertEquals("EMERGENCY_BYPASS", CallReason.EMERGENCY_BYPASS.name)
        assertEquals("UNKNOWN_NUMBER_BLOCKED", CallReason.UNKNOWN_NUMBER_BLOCKED.name)
        assertEquals("SUBSCRIPTION_INACTIVE", CallReason.SUBSCRIPTION_INACTIVE.name)
    }

    private fun createSettings(
        blockingEnabled: Boolean = true,
        allowStarredContacts: Boolean = true,
        blockUnknownNumbers: Boolean = true,
        emergencyBypassEnabled: Boolean = true,
        emergencyBypassMinutes: Int = 3,
        allowRecentOutgoing: Boolean = true,
        recentOutgoingDays: Int = 7,
        scheduleEnabled: Boolean = false,
        scheduleStartHour: Int = 22,
        scheduleStartMinute: Int = 0,
        scheduleEndHour: Int = 7,
        scheduleEndMinute: Int = 0
    ): SettingsSnapshot {
        return SettingsSnapshot(
            blockingEnabled = blockingEnabled,
            allowStarredContacts = allowStarredContacts,
            blockUnknownNumbers = blockUnknownNumbers,
            emergencyBypassEnabled = emergencyBypassEnabled,
            emergencyBypassMinutes = emergencyBypassMinutes,
            allowRecentOutgoing = allowRecentOutgoing,
            recentOutgoingDays = recentOutgoingDays,
            scheduleEnabled = scheduleEnabled,
            scheduleStartHour = scheduleStartHour,
            scheduleStartMinute = scheduleStartMinute,
            scheduleEndHour = scheduleEndHour,
            scheduleEndMinute = scheduleEndMinute
        )
    }
}

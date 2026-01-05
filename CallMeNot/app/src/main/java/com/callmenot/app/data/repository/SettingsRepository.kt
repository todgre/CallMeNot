package com.callmenot.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val BLOCKING_ENABLED = booleanPreferencesKey("blocking_enabled")
        val ALLOW_STARRED_CONTACTS = booleanPreferencesKey("allow_starred_contacts")
        val ALLOW_ALL_CONTACTS = booleanPreferencesKey("allow_all_contacts")
        val BLOCK_UNKNOWN_NUMBERS = booleanPreferencesKey("block_unknown_numbers")
        val EMERGENCY_BYPASS_ENABLED = booleanPreferencesKey("emergency_bypass_enabled")
        val EMERGENCY_BYPASS_MINUTES = intPreferencesKey("emergency_bypass_minutes")
        val ALLOW_RECENT_OUTGOING = booleanPreferencesKey("allow_recent_outgoing")
        val RECENT_OUTGOING_DAYS = intPreferencesKey("recent_outgoing_days")
        val SCHEDULE_ENABLED = booleanPreferencesKey("schedule_enabled")
        val SCHEDULE_START_HOUR = intPreferencesKey("schedule_start_hour")
        val SCHEDULE_START_MINUTE = intPreferencesKey("schedule_start_minute")
        val SCHEDULE_END_HOUR = intPreferencesKey("schedule_end_hour")
        val SCHEDULE_END_MINUTE = intPreferencesKey("schedule_end_minute")
        val SHOW_BLOCKED_NOTIFICATIONS = booleanPreferencesKey("show_blocked_notifications")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val TRIAL_START_DATE = longPreferencesKey("trial_start_date")
        val USER_ID = stringPreferencesKey("user_id")
        val LAST_SEEN_VERSION = stringPreferencesKey("last_seen_version")
    }
    
    val blockingEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.BLOCKING_ENABLED] ?: true }
    val allowStarredContacts: Flow<Boolean> = context.dataStore.data.map { it[Keys.ALLOW_STARRED_CONTACTS] ?: true }
    val allowAllContacts: Flow<Boolean> = context.dataStore.data.map { it[Keys.ALLOW_ALL_CONTACTS] ?: false }
    val blockUnknownNumbers: Flow<Boolean> = context.dataStore.data.map { it[Keys.BLOCK_UNKNOWN_NUMBERS] ?: true }
    val emergencyBypassEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.EMERGENCY_BYPASS_ENABLED] ?: true }
    val emergencyBypassMinutes: Flow<Int> = context.dataStore.data.map { it[Keys.EMERGENCY_BYPASS_MINUTES] ?: 3 }
    val allowRecentOutgoing: Flow<Boolean> = context.dataStore.data.map { it[Keys.ALLOW_RECENT_OUTGOING] ?: true }
    val recentOutgoingDays: Flow<Int> = context.dataStore.data.map { it[Keys.RECENT_OUTGOING_DAYS] ?: 3 }
    val scheduleEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.SCHEDULE_ENABLED] ?: false }
    val scheduleStartHour: Flow<Int> = context.dataStore.data.map { it[Keys.SCHEDULE_START_HOUR] ?: 22 }
    val scheduleStartMinute: Flow<Int> = context.dataStore.data.map { it[Keys.SCHEDULE_START_MINUTE] ?: 0 }
    val scheduleEndHour: Flow<Int> = context.dataStore.data.map { it[Keys.SCHEDULE_END_HOUR] ?: 7 }
    val scheduleEndMinute: Flow<Int> = context.dataStore.data.map { it[Keys.SCHEDULE_END_MINUTE] ?: 0 }
    val showBlockedNotifications: Flow<Boolean> = context.dataStore.data.map { it[Keys.SHOW_BLOCKED_NOTIFICATIONS] ?: false }
    val onboardingCompleted: Flow<Boolean> = context.dataStore.data.map { it[Keys.ONBOARDING_COMPLETED] ?: false }
    val trialStartDate: Flow<Long?> = context.dataStore.data.map { it[Keys.TRIAL_START_DATE] }
    val userId: Flow<String?> = context.dataStore.data.map { it[Keys.USER_ID] }
    
    suspend fun setBlockingEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.BLOCKING_ENABLED] = enabled }
    }
    
    suspend fun setAllowStarredContacts(enabled: Boolean) {
        context.dataStore.edit { it[Keys.ALLOW_STARRED_CONTACTS] = enabled }
    }
    
    suspend fun setAllowAllContacts(enabled: Boolean) {
        context.dataStore.edit { it[Keys.ALLOW_ALL_CONTACTS] = enabled }
    }
    
    suspend fun setBlockUnknownNumbers(enabled: Boolean) {
        context.dataStore.edit { it[Keys.BLOCK_UNKNOWN_NUMBERS] = enabled }
    }
    
    suspend fun setEmergencyBypassEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.EMERGENCY_BYPASS_ENABLED] = enabled }
    }
    
    suspend fun setEmergencyBypassMinutes(minutes: Int) {
        context.dataStore.edit { it[Keys.EMERGENCY_BYPASS_MINUTES] = minutes }
    }
    
    suspend fun setAllowRecentOutgoing(enabled: Boolean) {
        context.dataStore.edit { it[Keys.ALLOW_RECENT_OUTGOING] = enabled }
    }
    
    suspend fun setScheduleEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SCHEDULE_ENABLED] = enabled }
    }
    
    suspend fun setScheduleTime(startHour: Int, startMinute: Int, endHour: Int, endMinute: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SCHEDULE_START_HOUR] = startHour
            prefs[Keys.SCHEDULE_START_MINUTE] = startMinute
            prefs[Keys.SCHEDULE_END_HOUR] = endHour
            prefs[Keys.SCHEDULE_END_MINUTE] = endMinute
        }
    }
    
    suspend fun setShowBlockedNotifications(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_BLOCKED_NOTIFICATIONS] = enabled }
    }
    
    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }
    
    suspend fun initTrialIfNeeded() {
        val current = context.dataStore.data.first()[Keys.TRIAL_START_DATE]
        if (current == null) {
            context.dataStore.edit { it[Keys.TRIAL_START_DATE] = System.currentTimeMillis() }
        }
    }
    
    suspend fun getTrialDaysRemaining(): Int {
        val startDate = context.dataStore.data.first()[Keys.TRIAL_START_DATE] ?: return 0
        val elapsed = System.currentTimeMillis() - startDate
        val daysElapsed = (elapsed / (24 * 60 * 60 * 1000)).toInt()
        return maxOf(0, 7 - daysElapsed)
    }
    
    suspend fun isTrialActive(): Boolean = getTrialDaysRemaining() > 0
    
    suspend fun setUserId(userId: String?) {
        context.dataStore.edit { 
            if (userId != null) {
                it[Keys.USER_ID] = userId
            } else {
                it.remove(Keys.USER_ID)
            }
        }
    }
    
    suspend fun getUserId(): String? = context.dataStore.data.first()[Keys.USER_ID]
    
    suspend fun getLastSeenVersion(): String? = context.dataStore.data.first()[Keys.LAST_SEEN_VERSION]
    
    suspend fun setLastSeenVersion(version: String) {
        context.dataStore.edit { it[Keys.LAST_SEEN_VERSION] = version }
    }
    
    suspend fun getSettingsSnapshot(): SettingsSnapshot {
        val prefs = context.dataStore.data.first()
        return SettingsSnapshot(
            blockingEnabled = prefs[Keys.BLOCKING_ENABLED] ?: true,
            allowStarredContacts = prefs[Keys.ALLOW_STARRED_CONTACTS] ?: true,
            allowAllContacts = prefs[Keys.ALLOW_ALL_CONTACTS] ?: false,
            blockUnknownNumbers = prefs[Keys.BLOCK_UNKNOWN_NUMBERS] ?: true,
            emergencyBypassEnabled = prefs[Keys.EMERGENCY_BYPASS_ENABLED] ?: true,
            emergencyBypassMinutes = prefs[Keys.EMERGENCY_BYPASS_MINUTES] ?: 3,
            allowRecentOutgoing = prefs[Keys.ALLOW_RECENT_OUTGOING] ?: true,
            recentOutgoingDays = prefs[Keys.RECENT_OUTGOING_DAYS] ?: 3,
            scheduleEnabled = prefs[Keys.SCHEDULE_ENABLED] ?: false,
            scheduleStartHour = prefs[Keys.SCHEDULE_START_HOUR] ?: 22,
            scheduleStartMinute = prefs[Keys.SCHEDULE_START_MINUTE] ?: 0,
            scheduleEndHour = prefs[Keys.SCHEDULE_END_HOUR] ?: 7,
            scheduleEndMinute = prefs[Keys.SCHEDULE_END_MINUTE] ?: 0
        )
    }
}

data class SettingsSnapshot(
    val blockingEnabled: Boolean,
    val allowStarredContacts: Boolean,
    val allowAllContacts: Boolean,
    val blockUnknownNumbers: Boolean,
    val emergencyBypassEnabled: Boolean,
    val emergencyBypassMinutes: Int,
    val allowRecentOutgoing: Boolean,
    val recentOutgoingDays: Int,
    val scheduleEnabled: Boolean,
    val scheduleStartHour: Int,
    val scheduleStartMinute: Int,
    val scheduleEndHour: Int,
    val scheduleEndMinute: Int
)

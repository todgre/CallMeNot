package com.whitelistcalls.app.util

import com.whitelistcalls.app.data.repository.SettingsSnapshot
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScheduleHelper @Inject constructor() {
    
    fun isWithinSchedule(settings: SettingsSnapshot): Boolean {
        if (!settings.scheduleEnabled) {
            return true
        }
        
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        val currentTimeMinutes = currentHour * 60 + currentMinute
        
        val startTimeMinutes = settings.scheduleStartHour * 60 + settings.scheduleStartMinute
        val endTimeMinutes = settings.scheduleEndHour * 60 + settings.scheduleEndMinute
        
        return if (startTimeMinutes <= endTimeMinutes) {
            currentTimeMinutes in startTimeMinutes until endTimeMinutes
        } else {
            currentTimeMinutes >= startTimeMinutes || currentTimeMinutes < endTimeMinutes
        }
    }
}

package com.whitelistcalls.app.util

import android.Manifest
import android.app.Activity
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun hasCallScreeningRole(): Boolean {
        val roleManager = context.getSystemService(Context.ROLE_SERVICE) as? RoleManager
        return roleManager?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) == true
    }
    
    fun getCallScreeningRoleIntent(): Intent? {
        val roleManager = context.getSystemService(Context.ROLE_SERVICE) as? RoleManager
        return roleManager?.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
    }
    
    fun hasContactsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    fun hasCallLogPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
    
    fun isBatteryOptimizationIgnored(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
    }
    
    fun getBatteryOptimizationSettingsIntent(): Intent {
        return Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
    }
    
    fun getAllRequiredPermissions(): List<String> {
        val permissions = mutableListOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_CALL_LOG
        )
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        
        return permissions
    }
    
    fun getMissingPermissions(): List<String> {
        return getAllRequiredPermissions().filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        }
    }
    
    data class PermissionStatus(
        val hasCallScreeningRole: Boolean,
        val hasContactsPermission: Boolean,
        val hasCallLogPermission: Boolean,
        val hasNotificationPermission: Boolean,
        val isBatteryOptimizationIgnored: Boolean
    ) {
        val isFullyConfigured: Boolean
            get() = hasCallScreeningRole && hasContactsPermission && hasCallLogPermission
    }
    
    fun getPermissionStatus(): PermissionStatus {
        return PermissionStatus(
            hasCallScreeningRole = hasCallScreeningRole(),
            hasContactsPermission = hasContactsPermission(),
            hasCallLogPermission = hasCallLogPermission(),
            hasNotificationPermission = hasNotificationPermission(),
            isBatteryOptimizationIgnored = isBatteryOptimizationIgnored()
        )
    }
}

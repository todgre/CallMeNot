package com.callmenot.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            ProtectionNotificationService.start(context)
            if (SamsungCallBlockerService.isSamsungDevice()) {
                SamsungCallBlockerService.start(context)
            }
        }
    }
}

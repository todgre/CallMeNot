package com.callmenot.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // CallScreeningService is automatically available after boot
            // No explicit start needed - Android binds to it when calls come in
        }
    }
}

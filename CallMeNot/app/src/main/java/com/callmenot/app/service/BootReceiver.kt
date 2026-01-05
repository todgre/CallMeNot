package com.callmenot.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.callmenot.app.data.repository.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    
    @Inject
    lateinit var settingsRepository: SettingsRepository
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val pendingResult = goAsync()
            scope.launch {
                try {
                    val settings = settingsRepository.getSettingsSnapshot()
                    if (settings.blockingEnabled) {
                        ProtectionNotificationService.start(context)
                    }
                    if (SamsungCallBlockerService.isSamsungDevice()) {
                        SamsungCallBlockerService.start(context)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}

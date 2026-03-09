package com.callmenot.app.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.provider.CallLog
import android.telecom.TelecomManager
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.callmenot.app.BuildConfig
import com.callmenot.app.MainActivity
import com.callmenot.app.R
import com.callmenot.app.domain.usecase.EvaluateCallUseCase
import com.callmenot.app.util.PhoneNumberUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SamsungCallBlockerService : Service() {

    companion object {
        private const val TAG = "SamsungCallBlocker"
        private const val CHANNEL_ID = "samsung_call_blocker_channel"
        private const val NOTIFICATION_ID = 1002
        private const val CALL_LOG_QUERY_DELAY_MS = 500L
        
        fun start(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val intent = Intent(context, SamsungCallBlockerService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                if (BuildConfig.DEBUG) Log.d(TAG, "Starting Samsung call blocker service")
            }
        }
        
        fun stop(context: Context) {
            val intent = Intent(context, SamsungCallBlockerService::class.java)
            context.stopService(intent)
            if (BuildConfig.DEBUG) Log.d(TAG, "Stopping Samsung call blocker service")
        }
        
        fun isSamsungDevice(): Boolean {
            return Build.MANUFACTURER.equals("samsung", ignoreCase = true)
        }
    }

    @Inject
    lateinit var evaluateCallUseCase: EvaluateCallUseCase
    
    @Inject
    lateinit var phoneNumberUtil: PhoneNumberUtil

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var telephonyManager: TelephonyManager? = null
    private var telecomManager: TelecomManager? = null
    private var telephonyCallback: TelephonyCallback? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) Log.d(TAG, "Service created")
        
        createNotificationChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                createNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, createNotification())
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            telecomManager = getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            registerTelephonyCallback()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Samsung Call Blocking",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Required for Samsung call blocking fallback"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Call Protection Active")
            .setContentText("Samsung enhanced call blocking enabled")
            .setSmallIcon(R.drawable.ic_shield)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setContentIntent(pendingIntent)
            .build()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun registerTelephonyCallback() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) 
            != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "READ_PHONE_STATE permission not granted")
            return
        }

        telephonyCallback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
            override fun onCallStateChanged(state: Int) {
                when (state) {
                    TelephonyManager.CALL_STATE_RINGING -> {
                        handleIncomingCall()
                    }
                }
            }
        }

        try {
            telephonyManager?.registerTelephonyCallback(
                mainExecutor,
                telephonyCallback!!
            )
            if (BuildConfig.DEBUG) Log.d(TAG, "TelephonyCallback registered successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register TelephonyCallback", e)
        }
    }

    private fun handleIncomingCall() {
        if (BuildConfig.DEBUG) Log.d(TAG, "Handling incoming call via Samsung fallback")
        
        serviceScope.launch {
            try {
                val incomingNumber = resolveIncomingNumberWithRetry()
                val isPrivateNumber = incomingNumber.isNullOrBlank()
                val normalizedNumber = if (!isPrivateNumber) {
                    phoneNumberUtil.normalize(incomingNumber!!)
                } else {
                    null
                }
                
                if (BuildConfig.DEBUG) Log.d(TAG, "Resolved incoming number, private=$isPrivateNumber")
                
                val decision = evaluateCallUseCase(
                    rawNumber = incomingNumber,
                    normalizedNumber = normalizedNumber,
                    isPrivateNumber = isPrivateNumber
                )
                
                if (BuildConfig.DEBUG) Log.d(TAG, "Call decision: shouldAllow=${decision.shouldAllow}, reason=${decision.reason}")
                
                if (!decision.shouldAllow) {
                    endCall()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error evaluating call", e)
            }
        }
    }

    private suspend fun resolveIncomingNumberWithRetry(): String? {
        val maxAttempts = 5
        val delayBetweenMs = 500L
        
        for (attempt in 1..maxAttempts) {
            delay(delayBetweenMs)
            val number = getLatestIncomingNumber()
            if (number != null) return number
            if (BuildConfig.DEBUG) Log.d(TAG, "Call log lookup attempt $attempt returned null, retrying...")
        }
        
        if (BuildConfig.DEBUG) Log.d(TAG, "Could not resolve incoming number after $maxAttempts attempts")
        return null
    }

    private fun getLatestIncomingNumber(): String? {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG)
            != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "READ_CALL_LOG permission not granted, cannot resolve number")
            return null
        }
        
        try {
            val cursor = contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.TYPE, CallLog.Calls.DATE),
                "${CallLog.Calls.TYPE} IN (?, ?)",
                arrayOf(
                    CallLog.Calls.INCOMING_TYPE.toString(),
                    CallLog.Calls.MISSED_TYPE.toString()
                ),
                "${CallLog.Calls.DATE} DESC"
            )
            
            cursor?.use {
                if (it.moveToFirst()) {
                    val numberIndex = it.getColumnIndex(CallLog.Calls.NUMBER)
                    val dateIndex = it.getColumnIndex(CallLog.Calls.DATE)
                    if (numberIndex >= 0) {
                        val number = it.getString(numberIndex)
                        val date = if (dateIndex >= 0) it.getLong(dateIndex) else 0L
                        val ageMs = System.currentTimeMillis() - date
                        if (ageMs < 10000 && !number.isNullOrBlank()) {
                            return number
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to query call log for incoming number", e)
        }
        
        return null
    }

    @Suppress("DEPRECATION")
    private fun endCall() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ANSWER_PHONE_CALLS) 
            != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "ANSWER_PHONE_CALLS permission not granted, cannot end call")
            return
        }

        try {
            val ended = telecomManager?.endCall() ?: false
            if (BuildConfig.DEBUG) Log.d(TAG, "Call end result: $ended")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to end call", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (BuildConfig.DEBUG) Log.d(TAG, "Service destroyed")
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyCallback?.let { callback ->
                try {
                    telephonyManager?.unregisterTelephonyCallback(callback)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to unregister callback", e)
                }
            }
        }
    }
}

package com.callmenot.app.service

import android.Manifest
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.telecom.TelecomManager
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.callmenot.app.domain.usecase.EvaluateCallUseCase
import com.callmenot.app.util.PhoneNumberUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SamsungCallBlockerService : Service() {

    companion object {
        private const val TAG = "SamsungCallBlocker"
        
        fun start(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val intent = Intent(context, SamsungCallBlockerService::class.java)
                context.startService(intent)
                Log.d(TAG, "Starting Samsung call blocker service")
            }
        }
        
        fun stop(context: Context) {
            val intent = Intent(context, SamsungCallBlockerService::class.java)
            context.stopService(intent)
            Log.d(TAG, "Stopping Samsung call blocker service")
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
        Log.d(TAG, "Service created")
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            telecomManager = getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            registerTelephonyCallback()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started")
        return START_STICKY
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
                Log.d(TAG, "Call state changed: $state")
                when (state) {
                    TelephonyManager.CALL_STATE_RINGING -> {
                        handleIncomingCall()
                    }
                    TelephonyManager.CALL_STATE_IDLE -> {
                        Log.d(TAG, "Call ended or no call")
                    }
                    TelephonyManager.CALL_STATE_OFFHOOK -> {
                        Log.d(TAG, "Call answered")
                    }
                }
            }
        }

        try {
            telephonyManager?.registerTelephonyCallback(
                mainExecutor,
                telephonyCallback!!
            )
            Log.d(TAG, "TelephonyCallback registered successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register TelephonyCallback", e)
        }
    }

    private fun handleIncomingCall() {
        Log.d(TAG, "Handling incoming call via Samsung fallback")
        
        serviceScope.launch {
            try {
                val decision = evaluateCallUseCase(
                    rawNumber = null,
                    normalizedNumber = null,
                    isPrivateNumber = true
                )
                
                Log.d(TAG, "Call decision: shouldAllow=${decision.shouldAllow}, reason=${decision.reason}")
                
                if (!decision.shouldAllow) {
                    endCall()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error evaluating call", e)
            }
        }
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
            Log.d(TAG, "Call end result: $ended")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to end call", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyCallback?.let { callback ->
                try {
                    telephonyManager?.unregisterTelephonyCallback(callback)
                    Log.d(TAG, "TelephonyCallback unregistered")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to unregister callback", e)
                }
            }
        }
    }
}

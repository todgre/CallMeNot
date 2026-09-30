package com.callmenot.app

import android.app.Application
import android.util.Log
import com.callmenot.app.service.BillingManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class CallMeNotApp : Application() {
    @Inject lateinit var billingManager: BillingManager

    companion object {
        private const val TAG = "CallMeNotApp"
    }

    override fun onCreate() {
        super.onCreate()
        // Screening can run before any Activity is opened (including after reboot).
        billingManager.initialize()
        Log.d(TAG, "CallMeNotApp initialized")
    }
}

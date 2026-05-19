package com.callmenot.app

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class CallMeNotApp : Application() {

    companion object {
        private const val TAG = "CallMeNotApp"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "CallMeNotApp initialized")
    }
}

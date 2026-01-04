package com.callmenot.app

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class CallMeNotApp : Application(), Configuration.Provider {

    companion object {
        private const val TAG = "CallMeNotApp"
    }

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = try {
            Configuration.Builder()
                .setWorkerFactory(workerFactory)
                .build()
        } catch (e: Exception) {
            Log.e(TAG, "Error creating WorkManager config", e)
            Configuration.Builder().build()
        }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "CallMeNotApp initialized")
    }
}

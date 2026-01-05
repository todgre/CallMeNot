package com.callmenot.app

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.callmenot.app.data.repository.SettingsRepository
import com.callmenot.app.ui.components.Changelog
import com.callmenot.app.ui.components.WhatsNewDialog
import com.callmenot.app.ui.navigation.CallMeNotNavHost
import com.callmenot.app.ui.theme.CallMeNotTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    companion object {
        private const val TAG = "MainActivity"
    }
    
    @Inject
    lateinit var settingsRepository: SettingsRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            enableEdgeToEdge()
        } catch (e: Exception) {
            Log.e(TAG, "Error enabling edge to edge", e)
        }
        
        setContent {
            var showWhatsNew by remember { mutableStateOf(false) }
            var lastSeenVersion by remember { mutableStateOf<String?>(null) }
            var hasCheckedVersion by remember { mutableStateOf(false) }
            
            LaunchedEffect(Unit) {
                val lastSeen = withContext(Dispatchers.IO) {
                    settingsRepository.getLastSeenVersion()
                }
                val currentVersion = Changelog.latestVersion
                
                if (lastSeen != currentVersion) {
                    lastSeenVersion = lastSeen
                    showWhatsNew = true
                }
                hasCheckedVersion = true
            }
            
            CallMeNotTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CallMeNotNavHost()
                    
                    if (showWhatsNew && hasCheckedVersion) {
                        WhatsNewDialog(
                            lastSeenVersion = lastSeenVersion,
                            onDismiss = {
                                showWhatsNew = false
                                CoroutineScope(Dispatchers.IO).launch {
                                    settingsRepository.setLastSeenVersion(Changelog.latestVersion)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

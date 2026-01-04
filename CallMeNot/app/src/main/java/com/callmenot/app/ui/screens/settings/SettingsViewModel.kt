package com.callmenot.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callmenot.app.data.repository.SettingsRepository
import com.callmenot.app.service.BillingManager
import com.callmenot.app.service.SubscriptionStatus
import com.callmenot.app.util.PermissionHelper
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val allowStarredContacts: Boolean = true,
    val blockUnknownNumbers: Boolean = true,
    val emergencyBypassEnabled: Boolean = true,
    val emergencyBypassMinutes: Int = 3,
    val allowRecentOutgoing: Boolean = true,
    val scheduleEnabled: Boolean = false,
    val scheduleStartHour: Int = 22,
    val scheduleEndHour: Int = 7,
    val subscriptionStatus: SubscriptionStatus = SubscriptionStatus.Loading,
    val trialDaysRemaining: Int = 0,
    val userEmail: String? = null,
    val permissionStatus: PermissionHelper.PermissionStatus? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val billingManager: BillingManager,
    private val permissionHelper: PermissionHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
        observeSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val trialDays = settingsRepository.getTrialDaysRemaining()
            val permStatus = permissionHelper.getPermissionStatus()
            val user = try {
                FirebaseAuth.getInstance().currentUser
            } catch (e: Exception) {
                null
            }
            
            _uiState.value = _uiState.value.copy(
                trialDaysRemaining = trialDays,
                permissionStatus = permStatus,
                userEmail = user?.phoneNumber ?: user?.email
            )
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            combine(
                settingsRepository.allowStarredContacts,
                settingsRepository.blockUnknownNumbers,
                settingsRepository.emergencyBypassEnabled,
                settingsRepository.emergencyBypassMinutes,
                settingsRepository.allowRecentOutgoing
            ) { starred, blockUnknown, emergencyBypass, emergencyMinutes, recentOutgoing ->
                _uiState.value.copy(
                    allowStarredContacts = starred,
                    blockUnknownNumbers = blockUnknown,
                    emergencyBypassEnabled = emergencyBypass,
                    emergencyBypassMinutes = emergencyMinutes,
                    allowRecentOutgoing = recentOutgoing
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
        
        viewModelScope.launch {
            billingManager.subscriptionStatus.collect { status ->
                _uiState.value = _uiState.value.copy(subscriptionStatus = status)
            }
        }
    }

    fun setAllowStarredContacts(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAllowStarredContacts(enabled)
        }
    }

    fun setBlockUnknownNumbers(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setBlockUnknownNumbers(enabled)
        }
    }

    fun setEmergencyBypassEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setEmergencyBypassEnabled(enabled)
        }
    }

    fun setAllowRecentOutgoing(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAllowRecentOutgoing(enabled)
        }
    }

    fun setScheduleEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setScheduleEnabled(enabled)
        }
    }

    fun signOut() {
        try {
            FirebaseAuth.getInstance().signOut()
        } catch (e: Exception) {
            // Firebase not configured, ignore
        }
        viewModelScope.launch {
            settingsRepository.setUserId(null)
        }
    }
}

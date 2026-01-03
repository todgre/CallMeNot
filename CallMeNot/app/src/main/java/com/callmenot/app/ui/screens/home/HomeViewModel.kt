package com.callmenot.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callmenot.app.data.repository.CallEventRepository
import com.callmenot.app.data.repository.SettingsRepository
import com.callmenot.app.data.repository.WhitelistRepository
import com.callmenot.app.service.BillingManager
import com.callmenot.app.service.SubscriptionStatus
import com.callmenot.app.util.PermissionHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isBlockingEnabled: Boolean = true,
    val subscriptionStatus: SubscriptionStatus = SubscriptionStatus.Loading,
    val trialDaysRemaining: Int = 0,
    val blockedToday: Int = 0,
    val allowedToday: Int = 0,
    val whitelistCount: Int = 0,
    val permissionStatus: PermissionHelper.PermissionStatus? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val callEventRepository: CallEventRepository,
    private val whitelistRepository: WhitelistRepository,
    private val billingManager: BillingManager,
    private val permissionHelper: PermissionHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        billingManager.initialize()
        loadData()
        observeData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val trialDays = settingsRepository.getTrialDaysRemaining()
            val permStatus = permissionHelper.getPermissionStatus()
            _uiState.value = _uiState.value.copy(
                trialDaysRemaining = trialDays,
                permissionStatus = permStatus
            )
        }
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                settingsRepository.blockingEnabled,
                billingManager.subscriptionStatus,
                callEventRepository.getTodayBlockedCount(),
                callEventRepository.getTodayAllowedCount(),
                whitelistRepository.getEntryCount()
            ) { blockingEnabled, subStatus, blocked, allowed, whitelistCount ->
                _uiState.value.copy(
                    isBlockingEnabled = blockingEnabled,
                    subscriptionStatus = subStatus,
                    blockedToday = blocked,
                    allowedToday = allowed,
                    whitelistCount = whitelistCount
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun toggleBlocking(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setBlockingEnabled(enabled)
        }
    }

    fun refreshPermissions() {
        _uiState.value = _uiState.value.copy(
            permissionStatus = permissionHelper.getPermissionStatus()
        )
    }

    fun isProtectionActive(): Boolean {
        val state = _uiState.value
        return when (state.subscriptionStatus) {
            is SubscriptionStatus.Active -> state.isBlockingEnabled
            else -> state.trialDaysRemaining > 0 && state.isBlockingEnabled
        }
    }
}

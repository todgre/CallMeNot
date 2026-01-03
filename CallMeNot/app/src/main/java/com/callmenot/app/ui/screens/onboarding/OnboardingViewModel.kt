package com.callmenot.app.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callmenot.app.data.repository.SettingsRepository
import com.callmenot.app.service.BillingManager
import com.callmenot.app.util.PermissionHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val permissionHelper: PermissionHelper,
    private val billingManager: BillingManager
) : ViewModel() {

    val isOnboardingComplete: StateFlow<Boolean> = settingsRepository.onboardingCompleted
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _currentStep = MutableStateFlow(0)
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()

    private val _permissionStatus = MutableStateFlow(permissionHelper.getPermissionStatus())
    val permissionStatus: StateFlow<PermissionHelper.PermissionStatus> = _permissionStatus.asStateFlow()

    fun refreshPermissionStatus() {
        _permissionStatus.value = permissionHelper.getPermissionStatus()
    }

    fun nextStep() {
        _currentStep.value++
    }

    fun previousStep() {
        if (_currentStep.value > 0) {
            _currentStep.value--
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            settingsRepository.initTrialIfNeeded()
            settingsRepository.setOnboardingCompleted(true)
            billingManager.initialize()
        }
    }

    fun hasCallScreeningRole(): Boolean = permissionHelper.hasCallScreeningRole()
    
    fun getCallScreeningRoleIntent() = permissionHelper.getCallScreeningRoleIntent()
}

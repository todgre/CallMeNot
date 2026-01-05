package com.callmenot.app.ui.screens.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callmenot.app.data.local.entity.CallAction
import com.callmenot.app.data.local.entity.CallEvent
import com.callmenot.app.data.repository.BlacklistRepository
import com.callmenot.app.data.repository.CallEventRepository
import com.callmenot.app.data.repository.TimePeriod
import com.callmenot.app.data.repository.WhitelistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ActivityUiState(
    val events: List<CallEvent> = emptyList(),
    val filter: CallAction? = null,
    val timePeriod: TimePeriod = TimePeriod.TODAY,
    val isLoading: Boolean = false
)

@HiltViewModel
class ActivityViewModel @Inject constructor(
    private val callEventRepository: CallEventRepository,
    private val whitelistRepository: WhitelistRepository,
    private val blacklistRepository: BlacklistRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActivityUiState())
    val uiState: StateFlow<ActivityUiState> = _uiState.asStateFlow()
    
    private var observeJob: Job? = null

    init {
        observeEvents()
    }

    private fun observeEvents() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            callEventRepository.getEventsForPeriod(
                period = _uiState.value.timePeriod,
                action = _uiState.value.filter
            ).collect { events ->
                _uiState.value = _uiState.value.copy(events = events)
            }
        }
    }

    fun setFilter(filter: CallAction?) {
        _uiState.value = _uiState.value.copy(filter = filter)
        observeEvents()
    }
    
    fun setTimePeriod(period: TimePeriod) {
        _uiState.value = _uiState.value.copy(timePeriod = period)
        observeEvents()
    }
    
    fun addToWhitelist(event: CallEvent) {
        viewModelScope.launch {
            val number = event.normalizedNumber ?: event.phoneNumber ?: return@launch
            whitelistRepository.addNumber(
                phoneNumber = number,
                displayName = event.displayName,
                notes = "Added from call activity"
            )
        }
    }
    
    fun removeFromWhitelist(event: CallEvent) {
        viewModelScope.launch {
            val number = event.normalizedNumber ?: event.phoneNumber ?: return@launch
            whitelistRepository.removeByNumber(number)
        }
    }
    
    fun allowTemporarily(event: CallEvent, hours: Int) {
        viewModelScope.launch {
            val number = event.normalizedNumber ?: event.phoneNumber ?: return@launch
            val expiresAt = System.currentTimeMillis() + (hours * 60 * 60 * 1000L)
            whitelistRepository.addNumber(
                phoneNumber = number,
                displayName = event.displayName,
                notes = "Temporary access",
                expiresAt = expiresAt
            )
        }
    }
    
    fun addToBlacklist(event: CallEvent) {
        viewModelScope.launch {
            val number = event.normalizedNumber ?: event.phoneNumber ?: return@launch
            blacklistRepository.addEntry(
                displayName = event.displayName ?: number,
                phoneNumber = number,
                reason = "Blocked from call activity"
            )
        }
    }
    
    fun removeFromBlacklist(event: CallEvent) {
        viewModelScope.launch {
            val number = event.normalizedNumber ?: event.phoneNumber ?: return@launch
            blacklistRepository.deleteByNormalizedNumber(number)
        }
    }
}

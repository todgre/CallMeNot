package com.callmenot.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callmenot.app.data.repository.BlacklistRepository
import com.callmenot.app.data.repository.SettingsRepository
import com.callmenot.app.service.BillingManager
import com.callmenot.app.service.SubscriptionStatus
import com.callmenot.app.util.ContactsHelper
import com.callmenot.app.util.PermissionHelper
import com.callmenot.app.util.PhoneNumberUtil
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ContactItem(
    val id: String,
    val name: String,
    val phoneNumbers: List<String>,
    val isSelected: Boolean = false
)

data class BlockedContactItem(
    val id: String,
    val name: String,
    val phoneNumber: String
)

private data class SettingsGroup1(
    val starred: Boolean,
    val allContacts: Boolean,
    val blockUnknown: Boolean,
    val emergencyEnabled: Boolean,
    val emergencyMinutes: Int
)

data class SettingsUiState(
    val allowStarredContacts: Boolean = true,
    val allowAllContacts: Boolean = false,
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
    val permissionStatus: PermissionHelper.PermissionStatus? = null,
    val showContactExclusionDialog: Boolean = false,
    val contacts: List<ContactItem> = emptyList(),
    val filteredContacts: List<ContactItem> = emptyList(),
    val contactSearchQuery: String = "",
    val isLoadingContacts: Boolean = false,
    val blockedContacts: List<BlockedContactItem> = emptyList()
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val billingManager: BillingManager,
    private val permissionHelper: PermissionHelper,
    private val contactsHelper: ContactsHelper,
    private val blacklistRepository: BlacklistRepository,
    private val phoneNumberUtil: PhoneNumberUtil
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
        observeSettings()
        observeBlockedContacts()
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
                settingsRepository.allowAllContacts,
                settingsRepository.blockUnknownNumbers,
                settingsRepository.emergencyBypassEnabled,
                settingsRepository.emergencyBypassMinutes
            ) { starred, allContacts, blockUnknown, emergencyEnabled, emergencyMinutes ->
                SettingsGroup1(starred, allContacts, blockUnknown, emergencyEnabled, emergencyMinutes)
            }.combine(settingsRepository.allowRecentOutgoing) { group1, recentOutgoing ->
                _uiState.value.copy(
                    allowStarredContacts = group1.starred,
                    allowAllContacts = group1.allContacts,
                    blockUnknownNumbers = group1.blockUnknown,
                    emergencyBypassEnabled = group1.emergencyEnabled,
                    emergencyBypassMinutes = group1.emergencyMinutes,
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
    
    private fun observeBlockedContacts() {
        viewModelScope.launch {
            blacklistRepository.entries.collect { entries ->
                val blockedItems = entries.map { entry ->
                    BlockedContactItem(
                        id = entry.id,
                        name = entry.displayName,
                        phoneNumber = entry.phoneNumber
                    )
                }
                _uiState.value = _uiState.value.copy(blockedContacts = blockedItems)
            }
        }
    }

    fun setAllowStarredContacts(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAllowStarredContacts(enabled)
        }
    }

    fun setAllowAllContacts(enabled: Boolean) {
        if (enabled) {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(
                    isLoadingContacts = true,
                    showContactExclusionDialog = true
                )
                
                val contacts = withContext(Dispatchers.IO) {
                    contactsHelper.getAllContacts().map { contact ->
                        ContactItem(
                            id = contact.id,
                            name = contact.name,
                            phoneNumbers = contact.phoneNumbers,
                            isSelected = false
                        )
                    }
                }
                
                _uiState.value = _uiState.value.copy(
                    contacts = contacts,
                    filteredContacts = contacts,
                    contactSearchQuery = "",
                    isLoadingContacts = false
                )
            }
        } else {
            viewModelScope.launch {
                settingsRepository.setAllowAllContacts(false)
            }
        }
    }
    
    fun toggleContactSelection(contactId: String) {
        val updatedContacts = _uiState.value.contacts.map { contact ->
            if (contact.id == contactId) {
                contact.copy(isSelected = !contact.isSelected)
            } else {
                contact
            }
        }
        val query = _uiState.value.contactSearchQuery
        val filtered = if (query.isBlank()) {
            updatedContacts
        } else {
            updatedContacts.filter { contact ->
                contact.name.contains(query, ignoreCase = true) ||
                contact.phoneNumbers.any { it.contains(query) }
            }
        }
        _uiState.value = _uiState.value.copy(
            contacts = updatedContacts,
            filteredContacts = filtered
        )
    }
    
    fun confirmContactExclusions() {
        viewModelScope.launch {
            val selectedContacts = _uiState.value.contacts.filter { it.isSelected }
            
            for (contact in selectedContacts) {
                for (phoneNumber in contact.phoneNumbers) {
                    blacklistRepository.addEntry(
                        displayName = contact.name,
                        phoneNumber = phoneNumber,
                        reason = "Excluded when enabling Allow All Contacts"
                    )
                }
            }
            
            settingsRepository.setAllowAllContacts(true)
            
            _uiState.value = _uiState.value.copy(
                showContactExclusionDialog = false,
                contacts = emptyList()
            )
        }
    }
    
    fun dismissContactExclusionDialog() {
        _uiState.value = _uiState.value.copy(
            showContactExclusionDialog = false,
            contacts = emptyList(),
            filteredContacts = emptyList(),
            contactSearchQuery = ""
        )
    }
    
    fun updateContactSearchQuery(query: String) {
        val contacts = _uiState.value.contacts
        val filtered = if (query.isBlank()) {
            contacts
        } else {
            contacts.filter { contact ->
                contact.name.contains(query, ignoreCase = true) ||
                contact.phoneNumbers.any { it.contains(query) }
            }
        }
        _uiState.value = _uiState.value.copy(
            contactSearchQuery = query,
            filteredContacts = filtered
        )
    }
    
    fun unblockContact(blockedContact: BlockedContactItem) {
        viewModelScope.launch {
            blacklistRepository.deleteByNormalizedNumber(
                phoneNumberUtil.normalize(blockedContact.phoneNumber)
            )
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

    fun setEmergencyBypassMinutes(minutes: Int) {
        val clampedMinutes = minutes.coerceIn(1, 60)
        viewModelScope.launch {
            settingsRepository.setEmergencyBypassMinutes(clampedMinutes)
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

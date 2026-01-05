package com.callmenot.app.ui.screens.whitelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callmenot.app.data.local.entity.WhitelistEntry
import com.callmenot.app.data.repository.WhitelistRepository
import com.callmenot.app.util.ContactsHelper
import com.callmenot.app.util.PhoneNumberUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WhitelistUiState(
    val entries: List<WhitelistEntry> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val showAddDialog: Boolean = false,
    val showContactPicker: Boolean = false,
    val selectedContacts: Set<String> = emptySet(),
    val lastAddedEntry: AddedEntryInfo? = null
)

data class AddedEntryInfo(
    val phoneNumber: String,
    val displayName: String,
    val matchedContact: ContactsHelper.Contact? = null
)

@HiltViewModel
class WhitelistViewModel @Inject constructor(
    private val whitelistRepository: WhitelistRepository,
    private val contactsHelper: ContactsHelper,
    private val phoneNumberUtil: PhoneNumberUtil
) : ViewModel() {

    private val _uiState = MutableStateFlow(WhitelistUiState())
    val uiState: StateFlow<WhitelistUiState> = _uiState.asStateFlow()

    private val _contacts = MutableStateFlow<List<ContactsHelper.Contact>>(emptyList())
    val contacts: StateFlow<List<ContactsHelper.Contact>> = _contacts.asStateFlow()

    init {
        observeWhitelist()
    }

    private fun observeWhitelist() {
        viewModelScope.launch {
            whitelistRepository.getAllEntries().collect { entries ->
                _uiState.value = _uiState.value.copy(entries = entries)
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        if (query.isNotEmpty()) {
            viewModelScope.launch {
                whitelistRepository.searchEntries(query).collect { entries ->
                    _uiState.value = _uiState.value.copy(entries = entries)
                }
            }
        } else {
            observeWhitelist()
        }
    }

    fun showAddDialog() {
        _uiState.value = _uiState.value.copy(showAddDialog = true)
        loadContacts()
    }

    fun hideAddDialog() {
        _uiState.value = _uiState.value.copy(showAddDialog = false)
    }

    fun showContactPicker() {
        _uiState.value = _uiState.value.copy(showContactPicker = true)
        loadContacts()
    }

    fun hideContactPicker() {
        _uiState.value = _uiState.value.copy(
            showContactPicker = false,
            selectedContacts = emptySet()
        )
    }

    private fun loadContacts() {
        viewModelScope.launch {
            _contacts.value = contactsHelper.getAllContacts()
        }
    }

    fun toggleContactSelection(contactId: String) {
        val currentSelected = _uiState.value.selectedContacts
        _uiState.value = _uiState.value.copy(
            selectedContacts = if (contactId in currentSelected) {
                currentSelected - contactId
            } else {
                currentSelected + contactId
            }
        )
    }

    fun selectAllContacts() {
        _uiState.value = _uiState.value.copy(
            selectedContacts = _contacts.value.map { it.id }.toSet()
        )
    }

    fun clearContactSelection() {
        _uiState.value = _uiState.value.copy(selectedContacts = emptySet())
    }

    fun addSelectedContacts() {
        viewModelScope.launch {
            val selectedIds = _uiState.value.selectedContacts
            val contactsToAdd = _contacts.value.filter { it.id in selectedIds }
            
            contactsToAdd.forEach { contact ->
                contact.phoneNumbers.forEach { number ->
                    whitelistRepository.addEntry(
                        displayName = contact.name,
                        phoneNumber = number,
                        contactId = contact.id
                    )
                }
            }
            hideContactPicker()
        }
    }

    fun addManualNumber(name: String, phoneNumber: String) {
        viewModelScope.launch {
            if (phoneNumberUtil.isValidNumber(phoneNumber)) {
                val normalizedNumber = phoneNumberUtil.normalizeNumber(phoneNumber)
                val matchedContact = findContactByNumber(normalizedNumber)
                
                val displayName = when {
                    name.isNotBlank() -> name
                    matchedContact != null -> matchedContact.name
                    else -> phoneNumber
                }
                
                whitelistRepository.addEntry(
                    displayName = displayName,
                    phoneNumber = phoneNumber,
                    contactId = matchedContact?.id
                )
                
                _uiState.value = _uiState.value.copy(
                    lastAddedEntry = AddedEntryInfo(
                        phoneNumber = phoneNumber,
                        displayName = displayName,
                        matchedContact = matchedContact
                    )
                )
                
                hideAddDialog()
            }
        }
    }

    private fun findContactByNumber(phoneNumber: String): ContactsHelper.Contact? {
        val normalizedInput = phoneNumberUtil.normalizeNumber(phoneNumber)
        return _contacts.value.find { contact ->
            contact.phoneNumbers.any { number ->
                phoneNumberUtil.numbersMatch(normalizedInput, number)
            }
        }
    }

    fun clearLastAddedEntry() {
        _uiState.value = _uiState.value.copy(lastAddedEntry = null)
    }

    fun addFromContact(contact: ContactsHelper.Contact) {
        viewModelScope.launch {
            contact.phoneNumbers.forEach { number ->
                whitelistRepository.addEntry(
                    displayName = contact.name,
                    phoneNumber = number,
                    contactId = contact.id
                )
            }
            hideContactPicker()
        }
    }

    fun deleteEntry(entry: WhitelistEntry) {
        viewModelScope.launch {
            whitelistRepository.deleteEntry(entry)
        }
    }

    fun toggleEmergencyBypass(entry: WhitelistEntry) {
        viewModelScope.launch {
            whitelistRepository.updateEntry(
                entry.copy(isEmergencyBypass = !entry.isEmergencyBypass)
            )
        }
    }
}

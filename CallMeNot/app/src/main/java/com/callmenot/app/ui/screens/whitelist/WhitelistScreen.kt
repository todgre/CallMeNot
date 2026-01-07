package com.callmenot.app.ui.screens.whitelist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.callmenot.app.data.local.entity.WhitelistEntry
import com.callmenot.app.util.ContactsHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhitelistScreen(
    viewModel: WhitelistViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val contacts by viewModel.contacts.collectAsState()
    var showInfoDialog by remember { mutableStateOf(false) }
    var entryToDelete by remember { mutableStateOf<WhitelistEntry?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(uiState.lastAddedEntry) {
        uiState.lastAddedEntry?.let { entry ->
            if (entry.matchedContact != null) {
                snackbarHostState.showSnackbar(
                    message = "Added ${entry.displayName} - matched to existing contact!"
                )
            }
            viewModel.clearLastAddedEntry()
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showAddDialog() }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Whitelist",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { showInfoDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "What is a whitelist?",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Text(
                text = "Only these people can call you",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search whitelist") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (uiState.entries.isEmpty()) {
                EmptyWhitelistMessage()
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.entries, key = { it.id }) { entry ->
                        WhitelistEntryCard(
                            entry = entry,
                            onDelete = { entryToDelete = entry },
                            onToggleEmergencyBypass = { viewModel.toggleEmergencyBypass(entry) }
                        )
                    }
                }
            }
        }
    }
    
    if (uiState.showAddDialog) {
        AddWhitelistDialog(
            onDismiss = { 
                viewModel.clearAddError()
                viewModel.hideAddDialog() 
            },
            onAddManual = { name, number -> viewModel.addManualNumber(name, number) },
            onPickFromContacts = { viewModel.showContactPicker() },
            errorMessage = uiState.addError
        )
    }
    
    if (uiState.showContactPicker) {
        ContactPickerSheet(
            contacts = contacts,
            selectedContactIds = uiState.selectedContacts,
            onDismiss = { viewModel.hideContactPicker() },
            onToggleContact = { viewModel.toggleContactSelection(it) },
            onAddSelected = { viewModel.addSelectedContacts() },
            onSelectAll = { viewModel.selectAllContacts() },
            onClearSelection = { viewModel.clearContactSelection() }
        )
    }
    
    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text("What is a Whitelist?") },
            text = {
                Column {
                    Text(
                        "A whitelist is your list of approved callers. When call protection is active:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "\u2022 Calls FROM people on your whitelist will ring through normally",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "\u2022 Calls from everyone else will be automatically blocked",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Add your family, friends, doctors, and other important contacts to ensure you never miss their calls.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text("Got it")
                }
            }
        )
    }
    
    entryToDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { entryToDelete = null },
            title = { Text("Remove from Whitelist?") },
            text = {
                Text("Are you sure you want to remove ${entry.displayName} from your whitelist? Calls from this number will be blocked.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteEntry(entry)
                        entryToDelete = null
                    }
                ) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { entryToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun EmptyWhitelistMessage() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Your whitelist is empty",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Add contacts who can always reach you",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun WhitelistEntryCard(
    entry: WhitelistEntry,
    onDelete: () -> Unit,
    onToggleEmergencyBypass: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = entry.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = entry.phoneNumber,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Row {
                IconButton(onClick = onToggleEmergencyBypass) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Emergency Bypass",
                        tint = if (entry.isEmergencyBypass) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.outline
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun AddWhitelistDialog(
    onDismiss: () -> Unit,
    onAddManual: (name: String, number: String) -> Unit,
    onPickFromContacts: () -> Unit,
    errorMessage: String? = null
) {
    var name by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to Whitelist") },
        text = {
            Column {
                TextButton(
                    onClick = {
                        onDismiss()
                        onPickFromContacts()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Contacts, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pick from Contacts")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Or enter manually:",
                    style = MaterialTheme.typography.labelMedium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Phone Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = errorMessage != null
                )
                
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAddManual(name, phoneNumber) },
                enabled = phoneNumber.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContactPickerSheet(
    contacts: List<ContactsHelper.Contact>,
    selectedContactIds: Set<String>,
    onDismiss: () -> Unit,
    onToggleContact: (String) -> Unit,
    onAddSelected: () -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredContacts = remember(contacts, searchQuery) {
        if (searchQuery.isBlank()) {
            contacts
        } else {
            contacts.filter { contact ->
                contact.name.contains(searchQuery, ignoreCase = true) ||
                contact.phoneNumbers.any { it.contains(searchQuery) }
            }
        }
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Select Contacts",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                if (selectedContactIds.isNotEmpty()) {
                    Text(
                        text = "${selectedContactIds.size} selected",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = onSelectAll) {
                    Text("Select All")
                }
                if (selectedContactIds.isNotEmpty()) {
                    TextButton(onClick = onClearSelection) {
                        Text("Clear")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search by name or number") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 100.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredContacts) { contact ->
                    val isSelected = contact.id in selectedContactIds
                    Card(
                        onClick = { onToggleContact(contact.id) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isSelected) 
                                    Icons.Default.CheckCircle 
                                else 
                                    Icons.Default.RadioButtonUnchecked,
                                contentDescription = if (isSelected) "Selected" else "Not selected",
                                tint = if (isSelected)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Icon(Icons.Default.Person, contentDescription = null)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = contact.name,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = contact.phoneNumbers.firstOrNull() ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            
            if (selectedContactIds.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onAddSelected,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Add ${selectedContactIds.size} Contact${if (selectedContactIds.size > 1) "s" else ""}")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

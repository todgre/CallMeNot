package com.callmenot.app.ui.screens.activity

import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.callmenot.app.data.local.entity.CallAction
import com.callmenot.app.data.local.entity.CallEvent
import com.callmenot.app.data.local.entity.CallReason
import com.callmenot.app.data.repository.TimePeriod
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ActivityScreen(
    initialFilter: CallAction? = null,
    viewModel: ActivityViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    androidx.compose.runtime.LaunchedEffect(initialFilter) {
        if (initialFilter != null) {
            viewModel.setFilter(initialFilter)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Call Activity",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = uiState.filter == null,
                onClick = { viewModel.setFilter(null) },
                label = { Text("All") }
            )
            FilterChip(
                selected = uiState.filter == CallAction.BLOCKED,
                onClick = { viewModel.setFilter(CallAction.BLOCKED) },
                label = { Text("Blocked") }
            )
            FilterChip(
                selected = uiState.filter == CallAction.ALLOWED,
                onClick = { viewModel.setFilter(CallAction.ALLOWED) },
                label = { Text("Allowed") }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        
        TimePeriodFilter(
            selectedPeriod = uiState.timePeriod,
            onPeriodSelected = { viewModel.setTimePeriod(it) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.events.isEmpty()) {
            EmptyActivityMessage()
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.events, key = { it.id }) { event ->
                    CallEventCard(
                        event = event,
                        onAddToWhitelist = { viewModel.addToWhitelist(event) },
                        onRemoveFromWhitelist = { viewModel.removeFromWhitelist(event) },
                        onAllowTemporarily = { hours -> viewModel.allowTemporarily(event, hours) },
                        onAddToBlacklist = { viewModel.addToBlacklist(event) },
                        onRemoveFromBlacklist = { viewModel.removeFromBlacklist(event) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TimePeriodFilter(
    selectedPeriod: TimePeriod,
    onPeriodSelected: (TimePeriod) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Time: ",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FilterChip(
            selected = true,
            onClick = { expanded = true },
            label = { Text(selectedPeriod.label) }
        )
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            TimePeriod.entries.forEach { period ->
                DropdownMenuItem(
                    text = { Text(period.label) },
                    onClick = {
                        onPeriodSelected(period)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun EmptyActivityMessage() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CallReceived,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No call activity yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CallEventCard(
    event: CallEvent,
    onAddToWhitelist: () -> Unit,
    onRemoveFromWhitelist: () -> Unit,
    onAllowTemporarily: (Int) -> Unit,
    onAddToBlacklist: () -> Unit,
    onRemoveFromBlacklist: () -> Unit
) {
    val context = LocalContext.current
    val isBlocked = event.action == CallAction.BLOCKED
    val icon = if (isBlocked) Icons.Default.Block else Icons.Default.CheckCircle
    val iconColor = if (isBlocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
    val phoneNumber = event.phoneNumber
    
    var showMenu by remember { mutableStateOf(false) }
    var showTemporaryDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showMenu = true },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.displayName ?: event.phoneNumber ?: "Unknown",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )

                if (event.phoneNumber != null && event.displayName != null) {
                    Text(
                        text = event.phoneNumber,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = formatReason(event.reason),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isBlocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatTime(event.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Actions",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    if (phoneNumber != null) {
                        DropdownMenuItem(
                            text = { Text("Call") },
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL).apply {
                                    data = Uri.parse("tel:$phoneNumber")
                                }
                                context.startActivity(intent)
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Send SMS") },
                            onClick = {
                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("smsto:$phoneNumber")
                                }
                                context.startActivity(intent)
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Add to Contacts") },
                            onClick = {
                                val intent = Intent(Intent.ACTION_INSERT).apply {
                                    type = ContactsContract.Contacts.CONTENT_TYPE
                                    putExtra(ContactsContract.Intents.Insert.PHONE, phoneNumber)
                                    event.displayName?.let { name ->
                                        putExtra(ContactsContract.Intents.Insert.NAME, name)
                                    }
                                }
                                context.startActivity(intent)
                                showMenu = false
                            }
                        )
                        
                        HorizontalDivider()
                    }
                    
                    if (isBlocked) {
                        if (event.reason == CallReason.BLACKLISTED) {
                            DropdownMenuItem(
                                text = { Text("Remove from Blacklist") },
                                onClick = {
                                    onRemoveFromBlacklist()
                                    showMenu = false
                                }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("Add to Whitelist") },
                                onClick = {
                                    onAddToWhitelist()
                                    showMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Allow Temporarily...") },
                                onClick = {
                                    showTemporaryDialog = true
                                    showMenu = false
                                }
                            )
                        }
                    } else {
                        if (event.reason == CallReason.WHITELISTED) {
                            DropdownMenuItem(
                                text = { Text("Remove from Whitelist") },
                                onClick = {
                                    onRemoveFromWhitelist()
                                    showMenu = false
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Add to Blacklist") },
                            onClick = {
                                onAddToBlacklist()
                                showMenu = false
                            }
                        )
                    }
                }
            }
        }
    }
    
    if (showTemporaryDialog) {
        TemporaryAllowDialog(
            phoneNumber = event.displayName ?: event.phoneNumber ?: "Unknown",
            onDismiss = { showTemporaryDialog = false },
            onSelect = { hours ->
                onAllowTemporarily(hours)
                showTemporaryDialog = false
            }
        )
    }
}

@Composable
private fun TemporaryAllowDialog(
    phoneNumber: String,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Allow Temporarily") },
        text = { 
            Column {
                Text("Allow calls from $phoneNumber for:")
                Spacer(modifier = Modifier.height(16.dp))
                
                listOf(
                    1 to "1 hour",
                    4 to "4 hours",
                    24 to "24 hours",
                    168 to "1 week"
                ).forEach { (hours, label) ->
                    TextButton(
                        onClick = { onSelect(hours) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(label)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatReason(reason: CallReason): String {
    return when (reason) {
        CallReason.WHITELISTED -> "In whitelist"
        CallReason.STARRED_CONTACT -> "Starred contact"
        CallReason.KNOWN_CONTACT -> "In contacts"
        CallReason.RECENT_OUTGOING -> "Recent outgoing call"
        CallReason.EMERGENCY_BYPASS -> "Emergency bypass"
        CallReason.NOT_WHITELISTED -> "Not in whitelist"
        CallReason.BLACKLISTED -> "Blacklisted"
        CallReason.UNKNOWN_NUMBER_BLOCKED -> "Unknown number blocked"
        CallReason.SCREENING_DISABLED -> "Screening disabled"
        CallReason.SUBSCRIPTION_INACTIVE -> "Subscription inactive"
        CallReason.SCHEDULE_INACTIVE -> "Outside schedule"
    }
}

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

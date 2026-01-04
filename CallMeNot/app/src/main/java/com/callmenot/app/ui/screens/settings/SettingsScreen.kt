package com.callmenot.app.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.callmenot.app.service.SubscriptionStatus

@Composable
fun SettingsScreen(
    onNavigateToPaywall: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    if (uiState.showContactExclusionDialog) {
        ContactExclusionDialog(
            contacts = uiState.contacts,
            isLoading = uiState.isLoadingContacts,
            onToggleContact = { viewModel.toggleContactSelection(it) },
            onConfirm = { viewModel.confirmContactExclusions() },
            onDismiss = { viewModel.dismissContactExclusionDialog() }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        SettingsSection(title = "Blocking Rules") {
            SettingsToggle(
                title = "Allow Starred Contacts",
                description = "Contacts marked as favorites bypass whitelist",
                checked = uiState.allowStarredContacts,
                onCheckedChange = { viewModel.setAllowStarredContacts(it) }
            )
            
            HorizontalDivider()

            SettingsToggle(
                title = "Allow All Contacts",
                description = "Anyone in your phone's contact list can call",
                checked = uiState.allowAllContacts,
                onCheckedChange = { viewModel.setAllowAllContacts(it) }
            )
            
            HorizontalDivider()

            SettingsToggle(
                title = "Block Unknown Numbers",
                description = "Block calls with hidden caller ID",
                checked = uiState.blockUnknownNumbers,
                onCheckedChange = { viewModel.setBlockUnknownNumbers(it) }
            )
            
            HorizontalDivider()

            SettingsToggle(
                title = "Emergency Bypass",
                description = "Allow if same number calls twice in 3 minutes",
                checked = uiState.emergencyBypassEnabled,
                onCheckedChange = { viewModel.setEmergencyBypassEnabled(it) }
            )
            
            HorizontalDivider()

            SettingsToggle(
                title = "Allow Recent Outgoing",
                description = "Numbers you called in last 3 days can call back",
                checked = uiState.allowRecentOutgoing,
                onCheckedChange = { viewModel.setAllowRecentOutgoing(it) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        SettingsSection(title = "Account") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Subscription",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = when (uiState.subscriptionStatus) {
                            is SubscriptionStatus.Active -> {
                                if ((uiState.subscriptionStatus as SubscriptionStatus.Active).isYearly) {
                                    "Yearly subscription active"
                                } else {
                                    "Monthly subscription active"
                                }
                            }
                            is SubscriptionStatus.NotSubscribed -> {
                                if (uiState.trialDaysRemaining > 0) {
                                    "Trial: ${uiState.trialDaysRemaining} days left"
                                } else {
                                    "No active subscription"
                                }
                            }
                            else -> "Loading..."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (uiState.subscriptionStatus !is SubscriptionStatus.Active) {
                    OutlinedButton(onClick = onNavigateToPaywall) {
                        Text("Subscribe")
                    }
                }
            }

            HorizontalDivider()

            if (uiState.userEmail != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Signed in as",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = uiState.userEmail ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    TextButton(onClick = { viewModel.signOut() }) {
                        Text("Sign Out")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        SettingsSection(title = "Diagnostics") {
            Text(
                text = "Tap items below to fix missing permissions",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            uiState.permissionStatus?.let { status ->
                DiagnosticRow(
                    title = "Call Screening Role",
                    isEnabled = status.hasCallScreeningRole,
                    onClick = {
                        val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    }
                )
                HorizontalDivider()
                DiagnosticRow(
                    title = "Contacts Permission",
                    isEnabled = status.hasContactsPermission,
                    onClick = {
                        openAppSettings(context)
                    }
                )
                HorizontalDivider()
                DiagnosticRow(
                    title = "Call Log Permission",
                    isEnabled = status.hasCallLogPermission,
                    onClick = {
                        openAppSettings(context)
                    }
                )
                HorizontalDivider()
                DiagnosticRow(
                    title = "Battery Optimization Exempt",
                    isEnabled = status.isBatteryOptimizationIgnored,
                    onClick = {
                        try {
                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = Uri.parse("package:${context.packageName}")
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            try {
                                val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(fallbackIntent)
                            } catch (e2: Exception) {
                                openAppSettings(context)
                            }
                        }
                    }
                )
            }
        }
    }
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.parse("package:${context.packageName}")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp)
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column {
            content()
        }
    }
}

@Composable
private fun SettingsToggle(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun DiagnosticRow(
    title: String,
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )

        Icon(
            imageVector = if (isEnabled) Icons.Default.Check else Icons.Default.Close,
            contentDescription = null,
            tint = if (isEnabled) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
        )
        
        if (!isEnabled) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Tap to fix",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ContactExclusionDialog(
    contacts: List<ContactItem>,
    isLoading: Boolean,
    onToggleContact: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Select Contacts to Block")
        },
        text = {
            Column {
                Text(
                    text = "Choose any contacts you want to keep blocked. You can change this later from the Activity tab.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (contacts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No contacts found",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.height(300.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(contacts, key = { it.id }) { contact ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onToggleContact(contact.id) }
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = contact.isSelected,
                                    onCheckedChange = { onToggleContact(contact.id) }
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = contact.name,
                                        style = MaterialTheme.typography.bodyMedium
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
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                val selectedCount = contacts.count { it.isSelected }
                Text(if (selectedCount > 0) "Block $selectedCount & Enable" else "Enable Without Blocking")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

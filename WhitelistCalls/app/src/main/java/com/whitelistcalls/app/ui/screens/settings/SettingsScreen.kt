package com.whitelistcalls.app.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.whitelistcalls.app.service.SubscriptionStatus

@Composable
fun SettingsScreen(
    onNavigateToPaywall: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

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
                description = "Numbers you called in last 7 days can call back",
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
            uiState.permissionStatus?.let { status ->
                DiagnosticRow("Call Screening Role", status.hasCallScreeningRole)
                HorizontalDivider()
                DiagnosticRow("Contacts Permission", status.hasContactsPermission)
                HorizontalDivider()
                DiagnosticRow("Call Log Permission", status.hasCallLogPermission)
                HorizontalDivider()
                DiagnosticRow("Battery Optimization Exempt", status.isBatteryOptimizationIgnored)
            }
        }
    }
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
    isEnabled: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
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
    }
}

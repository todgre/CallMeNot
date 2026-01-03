package com.callmenot.app.ui.screens.home

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.callmenot.app.service.SubscriptionStatus

@Composable
fun HomeScreen(
    onNavigateToPaywall: () -> Unit,
    onNavigateToBlockedCalls: () -> Unit = {},
    onNavigateToAllowedCalls: () -> Unit = {},
    onNavigateToWhitelist: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "CallMeNot",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        ProtectionStatusCard(
            uiState = uiState,
            onToggleBlocking = { viewModel.toggleBlocking(it) },
            onNavigateToPaywall = onNavigateToPaywall
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Block,
                value = uiState.blockedToday.toString(),
                label = "Blocked Today",
                color = MaterialTheme.colorScheme.error,
                onClick = onNavigateToBlockedCalls
            )
            
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.CheckCircle,
                value = uiState.allowedToday.toString(),
                label = "Allowed Today",
                color = MaterialTheme.colorScheme.tertiary,
                onClick = onNavigateToAllowedCalls
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        StatCard(
            modifier = Modifier.fillMaxWidth(),
            icon = Icons.Default.People,
            value = uiState.whitelistCount.toString(),
            label = "Contacts in Whitelist",
            color = MaterialTheme.colorScheme.primary,
            onClick = onNavigateToWhitelist
        )
    }
}

@Composable
private fun ProtectionStatusCard(
    uiState: HomeUiState,
    onToggleBlocking: (Boolean) -> Unit,
    onNavigateToPaywall: () -> Unit
) {
    val isExpired = when (uiState.subscriptionStatus) {
        is SubscriptionStatus.Active -> false
        is SubscriptionStatus.NotSubscribed -> uiState.trialDaysRemaining <= 0
        else -> false
    }
    
    val statusColor = when {
        isExpired -> MaterialTheme.colorScheme.error
        uiState.isBlockingEnabled -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = statusColor.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isExpired) Icons.Default.Warning else Icons.Default.Shield,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = when {
                                isExpired -> "Protection Paused"
                                uiState.isBlockingEnabled -> "Protection Active"
                                else -> "Protection Disabled"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = statusColor
                        )
                        
                        when (uiState.subscriptionStatus) {
                            is SubscriptionStatus.Active -> {
                                Text(
                                    text = "Subscribed",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            is SubscriptionStatus.NotSubscribed -> {
                                if (uiState.trialDaysRemaining > 0) {
                                    Text(
                                        text = "Trial: ${uiState.trialDaysRemaining} days left",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    Text(
                                        text = "Trial ended",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            else -> {}
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (isExpired) {
                Button(
                    onClick = onNavigateToPaywall,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Resume Protection")
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Blocking Enabled",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Switch(
                        checked = uiState.isBlockingEnabled,
                        onCheckedChange = onToggleBlocking
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    value: String,
    label: String,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

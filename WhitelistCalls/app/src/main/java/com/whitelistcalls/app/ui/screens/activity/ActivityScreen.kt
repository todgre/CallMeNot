package com.whitelistcalls.app.ui.screens.activity

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.whitelistcalls.app.data.local.entity.CallAction
import com.whitelistcalls.app.data.local.entity.CallEvent
import com.whitelistcalls.app.data.local.entity.CallReason
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ActivityScreen(
    viewModel: ActivityViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

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

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.events.isEmpty()) {
            EmptyActivityMessage()
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.events, key = { it.id }) { event ->
                    CallEventCard(event = event)
                }
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
private fun CallEventCard(event: CallEvent) {
    val isBlocked = event.action == CallAction.BLOCKED
    val icon = if (isBlocked) Icons.Default.Block else Icons.Default.CheckCircle
    val iconColor = if (isBlocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary

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

            Text(
                text = formatTime(event.timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatReason(reason: CallReason): String {
    return when (reason) {
        CallReason.WHITELISTED -> "In whitelist"
        CallReason.STARRED_CONTACT -> "Starred contact"
        CallReason.RECENT_OUTGOING -> "Recent outgoing call"
        CallReason.EMERGENCY_BYPASS -> "Emergency bypass"
        CallReason.NOT_WHITELISTED -> "Not in whitelist"
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

package com.callmenot.app.ui.screens.paywall

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallScreen(
    onDismiss: () -> Unit,
    viewModel: PaywallViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context.findActivity()
    val snackbarHostState = remember { SnackbarHostState() }
    val selectedProduct = when (uiState.selectedProduct) {
        ProductType.MONTHLY -> uiState.monthlyProduct
        ProductType.YEARLY -> uiState.yearlyProduct
    }
    val selectedOffer = viewModel.selectedOffer(selectedProduct)
    val displayedTerms = selectedOffer?.let { formatOfferTerms(it) }
    
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Resume Protection",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Never be interrupted by spam again",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                FeatureList()

                Spacer(modifier = Modifier.height(32.dp))

                if (uiState.isLoading) {
                    CircularProgressIndicator()
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PricingCard(
                            modifier = Modifier.weight(1f),
                            title = "Monthly",
                            price = uiState.monthlyProduct?.let { product ->
                                viewModel.selectedOffer(product)?.pricingPhases?.pricingPhaseList?.lastOrNull()?.formattedPrice
                            } ?: "Unavailable",
                            period = uiState.monthlyProduct?.let { product ->
                                viewModel.selectedOffer(product)?.pricingPhases?.pricingPhaseList?.lastOrNull()
                                    ?.billingPeriod?.let { "per ${formatBillingPeriod(it)}" }
                            } ?: "Not available in Play",
                            isSelected = uiState.selectedProduct == ProductType.MONTHLY,
                            onClick = { viewModel.selectProduct(ProductType.MONTHLY) }
                        )

                        PricingCard(
                            modifier = Modifier.weight(1f),
                            title = "Yearly",
                            price = uiState.yearlyProduct?.let { product ->
                                viewModel.selectedOffer(product)?.pricingPhases?.pricingPhaseList?.lastOrNull()?.formattedPrice
                            } ?: "Unavailable",
                            period = uiState.yearlyProduct?.let { product ->
                                viewModel.selectedOffer(product)?.pricingPhases?.pricingPhaseList?.lastOrNull()
                                    ?.billingPeriod?.let { "per ${formatBillingPeriod(it)}" }
                            } ?: "Not available in Play",
                            isSelected = uiState.selectedProduct == ProductType.YEARLY,
                            onClick = { viewModel.selectProduct(ProductType.YEARLY) }
                        )
                    }
                }

                if (displayedTerms != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = displayedTerms,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                } else if (!uiState.isLoading) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "This plan is not currently available in Google Play.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                when (uiState.subscriptionStatus) {
                    is com.callmenot.app.service.SubscriptionStatus.Active -> {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Your subscription is active.", style = MaterialTheme.typography.bodyMedium)
                    }
                    is com.callmenot.app.service.SubscriptionStatus.Pending -> {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Your purchase is pending Google Play confirmation.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                    else -> Unit
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { activity?.let(viewModel::purchase) },
                modifier = Modifier.fillMaxWidth(),
                enabled = activity != null && !uiState.isLoading && !uiState.isPurchasing &&
                    selectedProduct != null && selectedOffer != null &&
                    uiState.subscriptionStatus !is com.callmenot.app.service.SubscriptionStatus.Active &&
                    uiState.subscriptionStatus !is com.callmenot.app.service.SubscriptionStatus.Pending
            ) {
                if (uiState.isPurchasing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        when (uiState.subscriptionStatus) {
                            is com.callmenot.app.service.SubscriptionStatus.Active -> "Subscription Active"
                            is com.callmenot.app.service.SubscriptionStatus.Pending -> "Purchase Pending"
                            else -> "Subscribe Now"
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = { viewModel.restorePurchases() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isRestoring
            ) {
                if (uiState.isRestoring) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp))
                } else {
                    Text("Restore Purchases")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Manage or cancel your subscription in Google Play.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun FeatureList() {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FeatureRow("Block all unwanted calls")
        FeatureRow("Whitelist-only protection")
        FeatureRow("Whitelist and call controls on this device")
        FeatureRow("Emergency bypass for urgent calls")
    }
}

@Composable
private fun FeatureRow(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PricingCard(
    modifier: Modifier = Modifier,
    title: String,
    price: String,
    period: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier,
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (isSelected) 
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary) 
        else 
            null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = price,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = period,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatOfferTerms(offer: com.android.billingclient.api.ProductDetails.SubscriptionOfferDetails): String {
    return offer.pricingPhases.pricingPhaseList.map { phase ->
        val period = formatBillingPeriod(phase.billingPeriod)
        when (phase.recurrenceMode) {
            com.android.billingclient.api.ProductDetails.RecurrenceMode.INFINITE_RECURRING ->
                "${phase.formattedPrice} every $period"
            com.android.billingclient.api.ProductDetails.RecurrenceMode.FINITE_RECURRING -> {
                val cycles = phase.billingCycleCount
                "${phase.formattedPrice} every $period for $cycles ${if (cycles == 1) "cycle" else "cycles"}"
            }
            else -> "${phase.formattedPrice} for $period"
        }
    }.joinToString(", then ")
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

package com.callmenot.app.ui.screens.paywall

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.ProductDetails
import com.callmenot.app.service.BillingManager
import com.callmenot.app.service.SubscriptionStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PaywallUiState(
    val monthlyProduct: ProductDetails? = null,
    val yearlyProduct: ProductDetails? = null,
    val selectedProduct: ProductType = ProductType.YEARLY,
    val isLoading: Boolean = true,
    val isPurchasing: Boolean = false,
    val isRestoring: Boolean = false,
    val subscriptionStatus: SubscriptionStatus = SubscriptionStatus.Loading,
    val error: String? = null
)

enum class ProductType {
    MONTHLY,
    YEARLY
}

@HiltViewModel
class PaywallViewModel @Inject constructor(
    private val billingManager: BillingManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaywallUiState())
    val uiState: StateFlow<PaywallUiState> = _uiState.asStateFlow()

    init {
        observeBilling()
        billingManager.initialize()
    }

    private fun observeBilling() {
        viewModelScope.launch {
            billingManager.productDetails.collectLatest { products ->
                _uiState.value = _uiState.value.copy(
                    monthlyProduct = products.find { it.productId == BillingManager.PRODUCT_MONTHLY },
                    yearlyProduct = products.find { it.productId == BillingManager.PRODUCT_YEARLY }
                )
            }
        }

        viewModelScope.launch {
            billingManager.productDetailsLoaded.collectLatest { loaded ->
                if (loaded) _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }

        viewModelScope.launch {
            billingManager.subscriptionStatus.collectLatest { status ->
                _uiState.value = _uiState.value.copy(
                    subscriptionStatus = status,
                    isPurchasing = if (status is SubscriptionStatus.Active ||
                        status is SubscriptionStatus.Pending
                    ) false else _uiState.value.isPurchasing
                )
            }
        }

        viewModelScope.launch {
            billingManager.billingError.collectLatest { error ->
                if (error != null) {
                    _uiState.value = _uiState.value.copy(
                        error = error,
                        isLoading = if (!billingManager.isConnected.value) false else _uiState.value.isLoading,
                        isPurchasing = false
                    )
                }
            }
        }
    }

    fun selectedOffer(product: ProductDetails? = selectedProductDetails()): ProductDetails.SubscriptionOfferDetails? =
        product?.subscriptionOfferDetails?.firstOrNull { it.pricingPhases.pricingPhaseList.isNotEmpty() }

    private fun selectedProductDetails(): ProductDetails? {
        val state = _uiState.value
        return when (state.selectedProduct) {
            ProductType.MONTHLY -> state.monthlyProduct
            ProductType.YEARLY -> state.yearlyProduct
        }
    }

    fun selectProduct(productType: ProductType) {
        _uiState.value = _uiState.value.copy(selectedProduct = productType)
    }

    fun purchase(activity: Activity) {
        val productDetails = selectedProductDetails()
        if (productDetails == null) {
            _uiState.value = _uiState.value.copy(
                error = "This subscription plan is not currently available in Google Play."
            )
            return
        }

        val offerToken = selectedOffer(productDetails)?.offerToken
        if (offerToken == null) {
            _uiState.value = _uiState.value.copy(error = "No eligible subscription offer is currently available.")
            return
        }

        _uiState.value = _uiState.value.copy(error = null, isPurchasing = true)
        val result = billingManager.launchBillingFlow(activity, productDetails, offerToken)
        if (result == null || result.responseCode != com.android.billingclient.api.BillingClient.BillingResponseCode.OK) {
            _uiState.value = _uiState.value.copy(isPurchasing = false)
        } else {
            // Play owns the purchase sheet; this flag only represents launching it.
            _uiState.value = _uiState.value.copy(isPurchasing = false)
        }
    }

    fun restorePurchases() {
        if (_uiState.value.isRestoring) return
        _uiState.value = _uiState.value.copy(isRestoring = true, error = null)
        billingManager.queryPurchases { result, purchases ->
            val current = _uiState.value
            if (result == null || result.responseCode != com.android.billingclient.api.BillingClient.BillingResponseCode.OK) {
                _uiState.value = current.copy(isRestoring = false)
            } else {
                val hasSubscription = purchases.orEmpty().any {
                    (it.products.contains(BillingManager.PRODUCT_MONTHLY) ||
                        it.products.contains(BillingManager.PRODUCT_YEARLY)) &&
                        (it.purchaseState == com.android.billingclient.api.Purchase.PurchaseState.PURCHASED ||
                            it.purchaseState == com.android.billingclient.api.Purchase.PurchaseState.PENDING)
                }
                _uiState.value = current.copy(
                    isRestoring = false,
                    error = if (hasSubscription) null else
                        "No previous purchases found. Check that you're signed in with the Google account used to subscribe."
                )
            }
        }
    }

    fun clearError() {
        billingManager.clearBillingError()
        _uiState.value = _uiState.value.copy(error = null)
    }
}

internal fun formatBillingPeriod(period: String): String {
    val match = Regex("^P(?:(\\d+)Y)?(?:(\\d+)M)?(?:(\\d+)W)?(?:(\\d+)D)?$").matchEntire(period)
        ?: return period
    val (years, months, weeks, days) = match.destructured
    val parts = listOf(
        years to "year",
        months to "month",
        weeks to "week",
        days to "day"
    ).mapNotNull { (amount, unit) ->
        amount.toIntOrNull()?.let { "$it ${if (it == 1) unit else "${unit}s"}" }
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" ") ?: period
}

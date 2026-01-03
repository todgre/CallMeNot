package com.callmenot.app.ui.screens.paywall

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.ProductDetails
import com.callmenot.app.service.BillingManager
import com.callmenot.app.service.SubscriptionStatus
import dagger.hilt.android.lifecycle.HiltViewModel
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
    }

    private fun observeBilling() {
        viewModelScope.launch {
            billingManager.productDetails.collect { products ->
                _uiState.value = _uiState.value.copy(
                    monthlyProduct = billingManager.getMonthlyProductDetails(),
                    yearlyProduct = billingManager.getYearlyProductDetails(),
                    isLoading = false
                )
            }
        }
        
        viewModelScope.launch {
            billingManager.subscriptionStatus.collect { status ->
                _uiState.value = _uiState.value.copy(subscriptionStatus = status)
            }
        }
    }

    fun selectProduct(productType: ProductType) {
        _uiState.value = _uiState.value.copy(selectedProduct = productType)
    }

    fun purchase(activity: Activity) {
        val state = _uiState.value
        val productDetails = when (state.selectedProduct) {
            ProductType.MONTHLY -> state.monthlyProduct
            ProductType.YEARLY -> state.yearlyProduct
        }

        if (productDetails == null) {
            _uiState.value = _uiState.value.copy(
                error = "Billing not available. The app must be installed from the Play Store to subscribe."
            )
            return
        }

        val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
        if (offerToken == null) {
            _uiState.value = _uiState.value.copy(error = "Subscription offer not available")
            return
        }

        _uiState.value = _uiState.value.copy(isPurchasing = true)
        billingManager.launchBillingFlow(activity, productDetails, offerToken)
        _uiState.value = _uiState.value.copy(isPurchasing = false)
    }

    fun restorePurchases() {
        billingManager.queryPurchases()
        _uiState.value = _uiState.value.copy(
            error = "No previous purchases found. If you have an active subscription, make sure you're signed in with the same Google account."
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

package com.callmenot.app.service

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class BillingManager @Inject constructor(
    @ApplicationContext private val context: Context
) : PurchasesUpdatedListener {

    companion object {
        const val PRODUCT_MONTHLY = "callmenot_monthly"
        const val PRODUCT_YEARLY = "callmenot_yearly"
    }

    private var billingClient: BillingClient? = null
    
    private val _subscriptionStatus = MutableStateFlow<SubscriptionStatus>(SubscriptionStatus.Loading)
    val subscriptionStatus: StateFlow<SubscriptionStatus> = _subscriptionStatus.asStateFlow()
    
    private val _productDetails = MutableStateFlow<List<ProductDetails>>(emptyList())
    val productDetails: StateFlow<List<ProductDetails>> = _productDetails.asStateFlow()
    
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    fun initialize() {
        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases()
            .build()
        
        startConnection()
    }

    private fun startConnection() {
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    _isConnected.value = true
                    queryProductDetails()
                    queryPurchases()
                } else {
                    _isConnected.value = false
                    _subscriptionStatus.value = SubscriptionStatus.Error("Billing setup failed")
                }
            }

            override fun onBillingServiceDisconnected() {
                _isConnected.value = false
                startConnection()
            }
        })
    }

    private fun queryProductDetails() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_MONTHLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_YEARLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )
        
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()
        
        billingClient?.queryProductDetailsAsync(params) { result, productDetailsList ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                _productDetails.value = productDetailsList
            }
        }
    }

    fun queryPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        
        billingClient?.queryPurchasesAsync(params) { result, purchaseList ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                processPurchases(purchaseList)
            } else {
                _subscriptionStatus.value = SubscriptionStatus.NotSubscribed
            }
        }
    }

    private fun processPurchases(purchases: List<Purchase>) {
        val validPurchase = purchases.find { purchase ->
            purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
            (purchase.products.contains(PRODUCT_MONTHLY) || purchase.products.contains(PRODUCT_YEARLY))
        }
        
        if (validPurchase != null) {
            if (!validPurchase.isAcknowledged) {
                acknowledgePurchase(validPurchase)
            }
            
            val isYearly = validPurchase.products.contains(PRODUCT_YEARLY)
            _subscriptionStatus.value = SubscriptionStatus.Active(
                isYearly = isYearly,
                expiryTime = validPurchase.purchaseTime + if (isYearly) 365L * 24 * 60 * 60 * 1000 else 30L * 24 * 60 * 60 * 1000
            )
        } else {
            _subscriptionStatus.value = SubscriptionStatus.NotSubscribed
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        
        billingClient?.acknowledgePurchase(params) { }
    }

    fun launchBillingFlow(activity: Activity, productDetails: ProductDetails, offerToken: String): BillingResult? {
        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(offerToken)
                .build()
        )
        
        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()
        
        return billingClient?.launchBillingFlow(activity, billingFlowParams)
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            processPurchases(purchases)
        } else if (result.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            // User cancelled
        } else {
            _subscriptionStatus.value = SubscriptionStatus.Error("Purchase failed: ${result.debugMessage}")
        }
    }

    fun isSubscriptionActive(): Boolean {
        return when (val status = _subscriptionStatus.value) {
            is SubscriptionStatus.Active -> true
            else -> false
        }
    }

    fun getMonthlyProductDetails(): ProductDetails? {
        return _productDetails.value.find { it.productId == PRODUCT_MONTHLY }
    }

    fun getYearlyProductDetails(): ProductDetails? {
        return _productDetails.value.find { it.productId == PRODUCT_YEARLY }
    }

    fun disconnect() {
        billingClient?.endConnection()
    }
}

sealed class SubscriptionStatus {
    object Loading : SubscriptionStatus()
    object NotSubscribed : SubscriptionStatus()
    data class Active(val isYearly: Boolean, val expiryTime: Long) : SubscriptionStatus()
    data class Error(val message: String) : SubscriptionStatus()
}

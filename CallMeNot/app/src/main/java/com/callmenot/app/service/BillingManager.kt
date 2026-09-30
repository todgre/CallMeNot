package com.callmenot.app.service

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

// A bounded offline grace period avoids abruptly disabling protection during
// a Play outage. Re-check Play whenever connected; a confirmed absence revokes it.
internal const val VERIFIED_ENTITLEMENT_CACHE_TTL_MILLIS = 7 * 24 * 60 * 60 * 1000L

@Singleton
class BillingManager @Inject constructor(
    @ApplicationContext private val context: Context
) : PurchasesUpdatedListener {

    companion object {
        const val PRODUCT_MONTHLY = "callmenot_monthly"
        const val PRODUCT_YEARLY = "callmenot_yearly"
        private const val ENTITLEMENT_CACHE_PREFS = "verified_subscription_cache"
        private const val ENTITLEMENT_VERIFIED_AT_KEY = "verified_at"
    }

    private var billingClient: BillingClient? = null
    private var isConnecting = false
    private val connectionLock = Any()
    
    private val _subscriptionStatus = MutableStateFlow<SubscriptionStatus>(SubscriptionStatus.Loading)
    val subscriptionStatus: StateFlow<SubscriptionStatus> = _subscriptionStatus.asStateFlow()
    
    private val _productDetails = MutableStateFlow<List<ProductDetails>>(emptyList())
    val productDetails: StateFlow<List<ProductDetails>> = _productDetails.asStateFlow()
    
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _billingError = MutableStateFlow<String?>(null)
    val billingError: StateFlow<String?> = _billingError.asStateFlow()

    private val _productDetailsLoaded = MutableStateFlow(false)
    val productDetailsLoaded: StateFlow<Boolean> = _productDetailsLoaded.asStateFlow()

    fun initialize() {
        synchronized(connectionLock) {
            if (billingClient == null) {
                billingClient = BillingClient.newBuilder(context)
                    .setListener(this)
                    .enablePendingPurchases(
                        PendingPurchasesParams.newBuilder()
                            .enableOneTimeProducts()
                            .build()
                    )
                    .enableAutoServiceReconnection()
                    .build()
            }
        }
        startConnection()
    }

    private fun startConnection() {
        val client = synchronized(connectionLock) {
            val current = billingClient ?: return
            if (_isConnected.value || isConnecting) return
            isConnecting = true
            current
        }
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                synchronized(connectionLock) { isConnecting = false }
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    _isConnected.value = true
                    _billingError.value = null
                    queryProductDetails()
                    queryPurchases()
                } else {
                    _isConnected.value = false
                    reportBillingFailure("Billing setup failed: ${result.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                synchronized(connectionLock) { isConnecting = false }
                _isConnected.value = false
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
        
        billingClient?.queryProductDetailsAsync(params) { result, queryResult ->
            _productDetailsLoaded.value = true
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                _productDetails.value = queryResult.productDetailsList
            } else {
                reportBillingFailure("Could not load subscription plans: ${result.debugMessage}")
            }
        }
    }

    fun clearBillingError() {
        _billingError.value = null
    }

    fun queryPurchases(onComplete: ((BillingResult?, List<Purchase>?) -> Unit)? = null) {
        val client = billingClient
        if (client == null || !_isConnected.value) {
            reportBillingFailure("Google Play Billing is not connected. Please try again.")
            onComplete?.invoke(null, null)
            return
        }
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        
        client.queryPurchasesAsync(params) { result, purchaseList ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                processPurchases(purchaseList)
            } else {
                reportBillingFailure("Could not check purchases: ${result.debugMessage}")
            }
            onComplete?.invoke(result, purchaseList)
        }
    }

    private fun processPurchases(purchases: List<Purchase>) {
        val matchingPurchases = purchases.filter { purchase ->
            purchase.products.contains(PRODUCT_MONTHLY) || purchase.products.contains(PRODUCT_YEARLY)
        }
        val purchased = matchingPurchases.firstOrNull {
            it.purchaseState == Purchase.PurchaseState.PURCHASED
        }

        if (purchased != null) {
            if (!purchased.isAcknowledged) acknowledgePurchase(purchased)
            cacheVerifiedEntitlement()
            _subscriptionStatus.value = SubscriptionStatus.Active(
                isYearly = purchased.products.contains(PRODUCT_YEARLY),
                purchaseTime = purchased.purchaseTime
            )
        } else if (matchingPurchases.any { it.purchaseState == Purchase.PurchaseState.PENDING }) {
            clearCachedEntitlement()
            _subscriptionStatus.value = SubscriptionStatus.Pending
        } else {
            clearCachedEntitlement()
            _subscriptionStatus.value = SubscriptionStatus.NotSubscribed
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        
        billingClient?.acknowledgePurchase(params) { result ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                reportBillingFailure("Purchase succeeded but could not be acknowledged: ${result.debugMessage}")
            }
        } ?: reportBillingFailure("Purchase succeeded but Google Play Billing is not connected to acknowledge it.")
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
        
        val client = billingClient
        if (client == null || !_isConnected.value) {
            reportBillingFailure("Google Play Billing is not connected. Please try again.")
            return null
        }
        return client.launchBillingFlow(activity, billingFlowParams).also { result ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK &&
                result.responseCode != BillingClient.BillingResponseCode.USER_CANCELED
            ) {
                reportBillingFailure("Could not start the purchase: ${result.debugMessage}")
            }
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            // The callback contains *changed* purchases, not necessarily the
            // account's complete inventory. Pending plan changes must not
            // revoke an existing active subscription.
            if (purchases.any { it.purchaseState == Purchase.PurchaseState.PURCHASED }) {
                processPurchases(purchases)
            }
            queryPurchases()
        } else if (result.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            // Cancellation is not an error and does not change the current entitlement.
        } else if (result.responseCode == BillingClient.BillingResponseCode.OK) {
            queryPurchases()
        } else {
            reportBillingFailure("Purchase failed: ${result.debugMessage}")
        }
    }

    private fun reportBillingFailure(message: String) {
        _billingError.value = message
        // An unsuccessful Play query is not proof that a subscription is absent.
        if (_subscriptionStatus.value !is SubscriptionStatus.Active) {
            _subscriptionStatus.value = SubscriptionStatus.Error(message)
        }
    }

    fun isSubscriptionActive(): Boolean {
        return when (_subscriptionStatus.value) {
            is SubscriptionStatus.Active -> true
            is SubscriptionStatus.NotSubscribed,
            is SubscriptionStatus.Pending -> false
            is SubscriptionStatus.Loading,
            is SubscriptionStatus.Error -> hasRecentVerifiedEntitlement()
        }
    }

    private fun cacheVerifiedEntitlement() {
        context.getSharedPreferences(ENTITLEMENT_CACHE_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putLong(ENTITLEMENT_VERIFIED_AT_KEY, System.currentTimeMillis())
            .apply()
    }

    private fun clearCachedEntitlement() {
        context.getSharedPreferences(ENTITLEMENT_CACHE_PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(ENTITLEMENT_VERIFIED_AT_KEY)
            .apply()
    }

    private fun hasRecentVerifiedEntitlement(): Boolean {
        val verifiedAt = context.getSharedPreferences(ENTITLEMENT_CACHE_PREFS, Context.MODE_PRIVATE)
            .getLong(ENTITLEMENT_VERIFIED_AT_KEY, 0L)
        return isVerifiedEntitlementCacheFresh(verifiedAt, System.currentTimeMillis())
    }

    fun getMonthlyProductDetails(): ProductDetails? {
        return _productDetails.value.find { it.productId == PRODUCT_MONTHLY }
    }

    fun getYearlyProductDetails(): ProductDetails? {
        return _productDetails.value.find { it.productId == PRODUCT_YEARLY }
    }

    fun disconnect() {
        synchronized(connectionLock) {
            billingClient?.endConnection()
            billingClient = null
            isConnecting = false
            _isConnected.value = false
        }
    }
}

internal fun isVerifiedEntitlementCacheFresh(verifiedAt: Long, now: Long): Boolean =
    verifiedAt > 0L && now >= verifiedAt &&
        now - verifiedAt < VERIFIED_ENTITLEMENT_CACHE_TTL_MILLIS

sealed class SubscriptionStatus {
    object Loading : SubscriptionStatus()
    object NotSubscribed : SubscriptionStatus()
    object Pending : SubscriptionStatus()
    data class Active(val isYearly: Boolean, val purchaseTime: Long) : SubscriptionStatus()
    data class Error(val message: String) : SubscriptionStatus()
}

package com.neonrush.game

import android.app.Activity
import android.content.Context
import android.util.Log
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.getCustomerInfoWith
import com.revenuecat.purchases.getOfferingsWith
import com.revenuecat.purchases.interfaces.PurchaseCallback
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.restorePurchasesWith
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object RevenueCatManager {
    private const val TAG = "RevenueCatManager"
    private const val REVENUECAT_API_KEY = "goog_sveqtpBHLaPtuW1JfUvRySdYoc0"

    // --- PRODUCT / PACKAGE IDENTIFIERS ---
    // These must match the identifiers configured in the RevenueCat dashboard.
    const val PRODUCT_ID_REMOVE_ADS = "remove_ads"
    const val PRODUCT_ID_STARTER_PACK = "starter_pack"
    const val PRODUCT_ID_GEMS_SMALL = "gems_small"
    const val PRODUCT_ID_GEMS_MEDIUM = "gems_medium"
    const val PRODUCT_ID_GEMS_LARGE = "gems_large"
    const val PRODUCT_ID_PRO_MONTHLY = "pro_monthly"
    const val PRODUCT_ID_PRO_ANNUAL = "pro_annual"

    // --- DISPLAY CONSTANTS (prices/amounts shown in the UI) ---
    // NOTE: these are display-only. The actual charged price always comes from
    // the store via the matched Package; update these if the dashboard prices change.
    const val REMOVE_ADS_PRICE_USD = "$2.99"
    const val STARTER_PACK_GEMS_AMOUNT = 500
    const val STARTER_PACK_PRICE_USD = "$4.99"
    const val GEMS_SMALL_AMOUNT = 100
    const val GEMS_SMALL_PRICE_USD = "$0.99"
    const val GEMS_MEDIUM_AMOUNT = 550
    const val GEMS_MEDIUM_PRICE_USD = "$4.99"
    const val GEMS_LARGE_AMOUNT = 1200
    const val GEMS_LARGE_PRICE_USD = "$9.99"
    const val SUBSCRIPTION_PRICE_MONTHLY_USD = "$4.99/mo"
    const val SUBSCRIPTION_PRICE_ANNUAL_USD = "$29.99/yr"

    private var currentOfferingPackages: List<Package> = emptyList()

    private val _isPro = MutableStateFlow(false)
    val isPro: StateFlow<Boolean> = _isPro.asStateFlow()

    fun initialize(context: Context) {
        try {
            Purchases.configure(
                PurchasesConfiguration.Builder(context, REVENUECAT_API_KEY).build()
            )
            fetchOfferings()
            refreshProStatus()
            Log.d(TAG, "RevenueCat initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "RevenueCat initialization failed: ${e.message}")
        }
    }

    private fun refreshProStatus() {
        Purchases.sharedInstance.getCustomerInfoWith(
            onError = { error ->
                Log.e(TAG, "Failed to fetch customer info: ${error.message}")
            },
            onSuccess = { customerInfo ->
                _isPro.value = isProActive(customerInfo)
            }
        )
    }

    fun restorePurchases(onComplete: (Boolean) -> Unit = {}) {
        Purchases.sharedInstance.restorePurchasesWith(
            onError = { error ->
                Log.e(TAG, "Restore purchases failed: ${error.message}")
                onComplete(false)
            },
            onSuccess = { customerInfo ->
                _isPro.value = isProActive(customerInfo)
                onComplete(true)
            }
        )
    }

    fun purchaseProSubscription(activity: Activity, annual: Boolean = false, onResult: (Boolean) -> Unit) {
        val productId = if (annual) PRODUCT_ID_PRO_ANNUAL else PRODUCT_ID_PRO_MONTHLY
        purchasePackage(
            activity = activity,
            packageIdentifier = productId,
            onSuccess = { onResult(true) },
            onError = { error ->
                Log.e(TAG, "Pro subscription purchase failed: $error")
                onResult(false)
            }
        )
    }

    // --- SIMPLE (BOOLEAN-CALLBACK) WRAPPERS ---
    // NeonRushViewModel purchases everything — gem packs, the starter pack,
    // remove-ads, pilot suits, and fuel tank tiers — through these. They all
    // route through the same purchasePackage(...) call above; this just gives
    // each call site a plain success/fail callback instead of a CustomerInfo.
    private fun purchaseProduct(activity: Activity, productId: String, onResult: (Boolean) -> Unit) {
        purchasePackage(
            activity = activity,
            packageIdentifier = productId,
            onSuccess = { onResult(true) },
            onError = { error ->
                Log.e(TAG, "Purchase failed for '$productId': $error")
                onResult(false)
            }
        )
    }

    // Used for both actual gem packs and fuel tank tier purchases — despite the
    // name, this is just "buy the product at this productId."
    fun purchaseGemPack(activity: Activity, productId: String, onResult: (Boolean) -> Unit) =
        purchaseProduct(activity, productId, onResult)

    fun purchaseStarterPack(activity: Activity, onResult: (Boolean) -> Unit) =
        purchaseProduct(activity, PRODUCT_ID_STARTER_PACK, onResult)

    fun purchaseRemoveAds(activity: Activity, onResult: (Boolean) -> Unit) =
        purchaseProduct(activity, PRODUCT_ID_REMOVE_ADS, onResult)

    fun purchasePilotSuit(activity: Activity, productId: String, onResult: (Boolean) -> Unit) =
        purchaseProduct(activity, productId, onResult)

    fun fetchOfferings(onResult: ((Boolean) -> Unit)? = null) {
        Purchases.sharedInstance.getOfferingsWith(
            onError = { error ->
                Log.e(TAG, "Error fetching offerings: ${error.message}")
                onResult?.invoke(false)
            },
            onSuccess = { offerings ->
                val currentOffering = offerings.current
                if (currentOffering != null) {
                    currentOfferingPackages = currentOffering.availablePackages
                    Log.d(TAG, "Offerings loaded: ${currentOfferingPackages.size} packages ready")
                    onResult?.invoke(true)
                } else {
                    Log.e(TAG, "No current offering configured in RevenueCat dashboard")
                    onResult?.invoke(false)
                }
            }
        )
    }

    fun purchasePackage(
        activity: Activity,
        packageIdentifier: String,
        onSuccess: (CustomerInfo) -> Unit,
        onError: (String) -> Unit
    ) {
        val pkgToPurchase = currentOfferingPackages.find { 
            it.identifier == packageIdentifier || 
            it.product.id == packageIdentifier ||
            it.product.id.contains(packageIdentifier)
        }

        if (pkgToPurchase != null) {
            executePurchaseCall(activity, pkgToPurchase, onSuccess, onError)
        } else {
            fetchOfferings { success ->
                if (success) {
                    val retryPkg = currentOfferingPackages.find { 
                        it.identifier == packageIdentifier || 
                        it.product.id == packageIdentifier ||
                        it.product.id.contains(packageIdentifier)
                    }
                    if (retryPkg != null) {
                        executePurchaseCall(activity, retryPkg, onSuccess, onError)
                    } else {
                        onError("Package identifier '$packageIdentifier' not found in RevenueCat offerings.")
                    }
                } else {
                    onError("Unable to load store billing info. Check connection.")
                }
            }
        }
    }

    private fun executePurchaseCall(
        activity: Activity,
        pkg: Package,
        onSuccess: (CustomerInfo) -> Unit,
        onError: (String) -> Unit
    ) {
        val params = PurchaseParams.Builder(activity, pkg).build()
        
        Purchases.sharedInstance.purchase(
            params,
            object : PurchaseCallback {
                override fun onCompleted(storeTransaction: StoreTransaction, customerInfo: CustomerInfo) {
                    Log.d(TAG, "Purchase completed successfully")
                    _isPro.value = isProActive(customerInfo)
                    onSuccess(customerInfo)
                }

                override fun onError(error: PurchasesError, userCancelled: Boolean) {
                    if (!userCancelled) {
                        Log.e(TAG, "Purchase Error: ${error.message}")
                        onError(error.message)
                    }
                }
            }
        )
    }

    fun isProActive(customerInfo: CustomerInfo): Boolean {
        return customerInfo.entitlements["pro"]?.isActive == true
    }
}

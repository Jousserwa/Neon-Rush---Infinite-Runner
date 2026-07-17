package com.neonrush.game

import android.app.Activity
import android.content.Context
import android.util.Log
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.interfaces.ReceiveOfferingsCallback
import com.revenuecat.purchases.interfaces.PurchaseCallback
import com.revenuecat.purchases.models.StoreTransaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object RevenueCatManager {
    private const val TAG = "RevenueCatManager"
    
    // YOUR REAL REVENUECAT API KEY
    private const val REVENUECAT_API_KEY = "test_xfMauZuucykZLbUkQyBtZriGWEv"
    
    // Product IDs
    const val PRODUCT_ID_PRO_MONTHLY = "neonrush_pro_monthly"
    const val PRODUCT_ID_PRO_ANNUAL = "neonrush_pro_annual"
    const val PRODUCT_ID_GEMS_SMALL = "neonrush_gems_small"
    const val PRODUCT_ID_GEMS_MEDIUM = "neonrush_gems_medium"
    const val PRODUCT_ID_GEMS_LARGE = "neonrush_gems_large"
    
    // Prices (will be fetched from RevenueCat, these are defaults)
    const val SUBSCRIPTION_PRICE_MONTHLY_USD = "$4.99"
    const val SUBSCRIPTION_PRICE_ANNUAL_USD = "$29.99"
    const val GEMS_SMALL_PRICE_USD = "$0.99"
    const val GEMS_MEDIUM_PRICE_USD = "$4.99"
    const val GEMS_LARGE_PRICE_USD = "$9.99"
    
    const val GEMS_SMALL_AMOUNT = 100
    const val GEMS_MEDIUM_AMOUNT = 550
    const val GEMS_LARGE_AMOUNT = 1200

    private val _isPro = MutableStateFlow(false)
    val isPro: StateFlow<Boolean> = _isPro.asStateFlow()

    private var isInitialized = false

    fun initialize(context: Context) {
        if (isInitialized) return
        try {
            val configuration = PurchasesConfiguration.Builder(context, REVENUECAT_API_KEY).build()
            Purchases.configure(configuration)
            isInitialized = true
            Log.d(TAG, "RevenueCat initialized successfully")
            
            // Check subscription status
            checkSubscriptionStatus()
        } catch (e: Exception) {
            Log.e(TAG, "RevenueCat initialization failed: ${e.message}")
        }
    }

    private fun checkSubscriptionStatus() {
        try {
            Purchases.sharedInstance.getCustomerInfo(
                object : ReceiveCustomerInfoCallback {
                    override fun onReceived(customerInfo: com.revenuecat.purchases.CustomerInfo) {
                        val hasPro = customerInfo.entitlements.active.containsKey("Neon Rush Pro")
                        _isPro.value = hasPro
                        Log.d(TAG, "Pro status: $hasPro")
                    }

                    override fun onError(error: PurchasesError) {
                        Log.e(TAG, "Error fetching customer info: ${error.message}")
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error checking subscription: ${e.message}")
        }
    }

    fun purchaseProSubscription(activity: Activity, onResult: (Boolean) -> Unit) {
        try {
            Purchases.sharedInstance.getOfferings(
                object : ReceiveOfferingsCallback {
                    override fun onReceived(offerings: com.revenuecat.purchases.Offerings) {
                        val monthlyPackage = offerings.current?.getPackage("monthly")
                        if (monthlyPackage != null) {
                            val purchaseParams = com.revenuecat.purchases.PurchaseParams.Builder(activity, monthlyPackage).build()
                            Purchases.sharedInstance.purchase(
                                purchaseParams,
                                object : PurchaseCallback {
                                    override fun onCompleted(storeTransaction: StoreTransaction, customerInfo: com.revenuecat.purchases.CustomerInfo) {
                                        _isPro.value = true
                                        onResult(true)
                                    }

                                    override fun onError(error: PurchasesError, userCancelled: Boolean) {
                                        Log.e(TAG, "Purchase failed: ${error.message}")
                                        onResult(false)
                                    }
                                }
                            )
                        } else {
                            Log.e(TAG, "Monthly package not found")
                            onResult(false)
                        }
                    }

                    override fun onError(error: PurchasesError) {
                        Log.e(TAG, "Error fetching offerings: ${error.message}")
                        onResult(false)
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Exception during purchase: ${e.message}")
            onResult(false)
        }
    }

    fun purchaseProSubscriptionAnnual(activity: Activity, onResult: (Boolean) -> Unit) {
        try {
            Purchases.sharedInstance.getOfferings(
                object : ReceiveOfferingsCallback {
                    override fun onReceived(offerings: com.revenuecat.purchases.Offerings) {
                        val annualPackage = offerings.current?.getPackage("annual")
                        if (annualPackage != null) {
                            val purchaseParams = com.revenuecat.purchases.PurchaseParams.Builder(activity, annualPackage).build()
                            Purchases.sharedInstance.purchase(
                                purchaseParams,
                                object : PurchaseCallback {
                                    override fun onCompleted(storeTransaction: StoreTransaction, customerInfo: com.revenuecat.purchases.CustomerInfo) {
                                        _isPro.value = true
                                        onResult(true)
                                    }

                                    override fun onError(error: PurchasesError, userCancelled: Boolean) {
                                        onResult(false)
                                    }
                                }
                            )
                        } else {
                            onResult(false)
                        }
                    }

                    override fun onError(error: PurchasesError) {
                        onResult(false)
                    }
                }
            )
        } catch (e: Exception) {
            onResult(false)
        }
    }

    fun purchaseGemPack(activity: Activity, productId: String, onResult: (Boolean) -> Unit) {
        try {
            Purchases.sharedInstance.getOfferings(
                object : ReceiveOfferingsCallback {
                    override fun onReceived(offerings: com.revenuecat.purchases.Offerings) {
                        // Find the package by product ID
                        val packageToBuy = offerings.all.values
                            .flatMap { it.availablePackages }
                            .find { it.product.identifier == productId }
                        
                        if (packageToBuy != null) {
                            val purchaseParams = com.revenuecat.purchases.PurchaseParams.Builder(activity, packageToBuy).build()
                            Purchases.sharedInstance.purchase(
                                purchaseParams,
                                object : PurchaseCallback {
                                    override fun onCompleted(storeTransaction: StoreTransaction, customerInfo: com.revenuecat.purchases.CustomerInfo) {
                                        onResult(true)
                                    }

                                    override fun onError(error: PurchasesError, userCancelled: Boolean) {
                                        onResult(false)
                                    }
                                }
                            )
                        } else {
                            onResult(false)
                        }
                    }

                    override fun onError(error: PurchasesError) {
                        onResult(false)
                    }
                }
            )
        } catch (e: Exception) {
            onResult(false)
        }
    }

    fun purchasePilotSuit(activity: Activity, productId: String, onResult: (Boolean) -> Unit) {
        // Pilot suits use the same purchase flow as gem packs
        purchaseGemPack(activity, productId, onResult)
    }

    fun restorePurchases(onResult: (Boolean) -> Unit) {
        try {
            Purchases.sharedInstance.restorePurchases(
                object : ReceiveCustomerInfoCallback {
                    override fun onReceived(customerInfo: com.revenuecat.purchases.CustomerInfo) {
                        val hasPro = customerInfo.entitlements.active.containsKey("Neon Rush Pro")
                        _isPro.value = hasPro
                        onResult(true)
                    }

                    override fun onError(error: PurchasesError) {
                        onResult(false)
                    }
                }
            )
        } catch (e: Exception) {
            onResult(false)
        }
    }
}

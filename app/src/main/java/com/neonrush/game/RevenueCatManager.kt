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
import com.revenuecat.purchases.getOfferingsWith
import com.revenuecat.purchases.interfaces.PurchaseCallback
import com.revenuecat.purchases.models.StoreTransaction

object RevenueCatManager {
    private const val TAG = "RevenueCatManager"
    private const val REVENUECAT_API_KEY = "goog_sveqtpBHLaPtuW1JfUvRySdYoc0"

    private var currentOfferingPackages: List<Package> = emptyList()

    fun initialize(context: Context) {
        try {
            Purchases.configure(
                PurchasesConfiguration.Builder(context, REVENUECAT_API_KEY).build()
            )
            fetchOfferings()
            Log.d(TAG, "RevenueCat initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "RevenueCat initialization failed: ${e.message}")
        }
    }

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

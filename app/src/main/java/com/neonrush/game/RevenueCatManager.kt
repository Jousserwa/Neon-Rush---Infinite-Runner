package com.neonrush.game

import android.app.Activity
import android.content.Context
import android.util.Log
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.getOfferingsWith
import com.revenuecat.purchases.purchaseWith

object RevenueCatManager {
    private const val TAG = "RevenueCatManager"
    private const val REVENUECAT_API_KEY = "goog_sveqtpBHLaPtuW1JfUvRySdYoc0" // Your RevenueCat Key

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
                    Log.e(TAG, "No current offering configured in RevenueCat")
                    onResult?.invoke(false)
                }
            }
        )
    }

    // Purchase any package by package identifier (e.g. "$rc_monthly", "$rc_annual", "gems_pack", "skin_pack")
    fun purchasePackage(
        activity: Activity,
        packageIdentifier: String,
        onSuccess: (CustomerInfo) -> Unit,
        onError: (String) -> Unit
    ) {
        val pkgToPurchase = currentOfferingPackages.find { 
            it.identifier == packageIdentifier || it.product.id.contains(packageIdentifier)
        }

        if (pkgToPurchase != null) {
            Purchases.sharedInstance.purchase(
                PurchaseParams.Builder(activity, pkgToPurchase).build(),
                onError = { error, userCancelled ->
                    if (!userCancelled) {
                        Log.e(TAG, "Purchase failed: ${error.message}")
                        onError(error.message)
                    }
                },
                onSuccess = { _, customerInfo ->
                    Log.d(TAG, "Purchase successful!")
                    onSuccess(customerInfo)
                }
            )
        } else {
            // Fallback: If packages aren't loaded yet, try refreshing once
            fetchOfferings { success ->
                if (success) {
                    val retryPkg = currentOfferingPackages.find { 
                        it.identifier == packageIdentifier || it.product.id.contains(packageIdentifier) 
                    }
                    if (retryPkg != null) {
                        Purchases.sharedInstance.purchase(
                            PurchaseParams.Builder(activity, retryPkg).build(),
                            onError = { error, userCancelled ->
                                if (!userCancelled) onError(error.message)
                            },
                            onSuccess = { _, customerInfo -> onSuccess(customerInfo) }
                        )
                    } else {
                        onError("Package $packageIdentifier not found in RevenueCat offerings.")
                    }
                } else {
                    onError("Unable to connect to Play Store billing. Please try again.")
                }
            }
        }
    }

    fun isProActive(customerInfo: CustomerInfo): Boolean {
        return customerInfo.entitlements["pro"]?.isActive == true
    }
}

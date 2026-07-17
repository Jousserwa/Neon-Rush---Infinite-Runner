package com.neonrush.game

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

object AdMobManager {
    private const val TAG = "AdMobManager"
    
    // YOUR REAL ADMOB APP ID
    const val APP_ID = "ca-app-pub-3841327492203214~9145496921"
    
    // YOUR REAL AD UNIT IDs
    const val BANNER_AD_UNIT_ID = "ca-app-pub-3841327492203214/6533049489"
    const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3841327492203214/3907006287"
    const val REWARDED_AD_UNIT_ID = "ca-app-pub-3841327492203214/4182218315"
    
    // TEST IDs (use these for testing, switch to real IDs for production)
    // const val BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
    // const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
    // const val REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    
    private var gameOverCount = 0
    private const val INTERSTITIAL_INTERVAL = 3 // Show interstitial every 3 game overs
    private const val PAYWALL_THRESHOLD = 5 // Show paywall after 5 ad views

    fun initialize(context: Context) {
        try {
            MobileAds.initialize(context) { initializationStatus ->
                Log.d(TAG, "AdMob initialized: $initializationStatus")
            }
            loadInterstitial(context)
            loadRewarded(context)
        } catch (e: Exception) {
            Log.e(TAG, "AdMob initialization failed: ${e.message}")
        }
    }

    private fun loadInterstitial(context: Context) {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(context, INTERSTITIAL_AD_UNIT_ID, adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    Log.d(TAG, "Interstitial ad loaded")
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    Log.e(TAG, "Interstitial failed to load: ${error.message}")
                }
            })
    }

    private fun loadRewarded(context: Context) {
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(context, REWARDED_AD_UNIT_ID, adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    Log.d(TAG, "Rewarded ad loaded")
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    Log.e(TAG, "Rewarded failed to load: ${error.message}")
                }
            })
    }

    fun createBannerAdView(context: Context): AdView {
        return AdView(context).apply {
            setAdSize(AdSize.BANNER)
            adUnitId = BANNER_AD_UNIT_ID
            loadAd(AdRequest.Builder().build())
        }
    }

    fun showInterstitialIfReady(activity: Activity, onComplete: () -> Unit) {
        val ad = interstitialAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    loadInterstitial(activity)
                    onComplete()
                }
                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    interstitialAd = null
                    loadInterstitial(activity)
                    onComplete()
                }
                override fun onAdShowedFullScreenContent() {
                    // Ad is showing
                }
            }
            ad.show(activity)
        } else {
            onComplete()
            loadInterstitial(activity)
        }
    }

    fun showRewardedIfReady(activity: Activity, onRewarded: () -> Unit) {
        val ad = rewardedAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    rewardedAd = null
                    loadRewarded(activity)
                }
                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    rewardedAd = null
                    loadRewarded(activity)
                }
            }
            ad.show(activity) { rewardItem ->
                Log.d(TAG, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
                onRewarded()
            }
        } else {
            // No ad available, still give reward for good UX
            onRewarded()
            loadRewarded(activity)
        }
    }

    fun incrementGameOver() {
        gameOverCount++
    }

    fun isInterstitialDue(): Boolean {
        return gameOverCount % INTERSTITIAL_INTERVAL == 0 && gameOverCount > 0
    }

    fun isPaywallDue(): Boolean {
        return gameOverCount >= PAYWALL_THRESHOLD && gameOverCount % PAYWALL_THRESHOLD == 0
    }

    fun resetCounters() {
        gameOverCount = 0
    }
}

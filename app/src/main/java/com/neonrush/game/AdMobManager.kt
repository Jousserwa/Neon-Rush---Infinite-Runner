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
    
    // REAL ADMOB APP ID
    const val APP_ID = "ca-app-pub-3841327492203214~9145496921"
    
    // REAL AD UNIT IDs
    const val BANNER_AD_UNIT_ID = "ca-app-pub-3841327492203214/6533049489"
    const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3841327492203214/3907006287"
    const val REWARDED_REVIVE_AD_UNIT_ID = "ca-app-pub-3841327492203214/4182218315"
    const val REWARDED_DOUBLE_GEMS_AD_UNIT_ID = "ca-app-pub-3841327492203214/8797213140"

    private var interstitialAd: InterstitialAd? = null
    private var rewardedReviveAd: RewardedAd? = null
    private var rewardedDoubleGemsAd: RewardedAd? = null
    
    private var gameOverCount = 0
    private const val INTERSTITIAL_INTERVAL = 3 
    private const val PAYWALL_THRESHOLD = 5 

    fun initialize(context: Context) {
        try {
            MobileAds.initialize(context) { initializationStatus ->
                Log.d(TAG, "AdMob initialized: $initializationStatus")
            }
            loadInterstitial(context)
            loadRewardedRevive(context)
            loadRewardedDoubleGems(context)
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

    private fun loadRewardedRevive(context: Context) {
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(context, REWARDED_REVIVE_AD_UNIT_ID, adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedReviveAd = ad
                    Log.d(TAG, "Rewarded Revive ad loaded")
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedReviveAd = null
                    Log.e(TAG, "Rewarded Revive failed to load: ${error.message}")
                }
            })
    }

    private fun loadRewardedDoubleGems(context: Context) {
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(context, REWARDED_DOUBLE_GEMS_AD_UNIT_ID, adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedDoubleGemsAd = ad
                    Log.d(TAG, "Rewarded Double Gems ad loaded")
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedDoubleGemsAd = null
                    Log.e(TAG, "Rewarded Double Gems failed to load: ${error.message}")
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
            }
            ad.show(activity)
        } else {
            onComplete()
            loadInterstitial(activity)
        }
    }

    // Call this method for Revive Button
    fun showRewardedReviveIfReady(activity: Activity, onRewarded: () -> Unit) {
        val ad = rewardedReviveAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    rewardedReviveAd = null
                    loadRewardedRevive(activity)
                }
                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    rewardedReviveAd = null
                    loadRewardedRevive(activity)
                }
            }
            ad.show(activity) { rewardItem ->
                AnalyticsManager.logAdViewed("rewarded_revive")
                onRewarded()
            }
        } else {
            onRewarded()
            loadRewardedRevive(activity)
        }
    }

    // Call this method for Double Gems Button
    fun showRewardedDoubleGemsIfReady(activity: Activity, onRewarded: () -> Unit) {
        val ad = rewardedDoubleGemsAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    rewardedDoubleGemsAd = null
                    loadRewardedDoubleGems(activity)
                }
                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    rewardedDoubleGemsAd = null
                    loadRewardedDoubleGems(activity)
                }
            }
            ad.show(activity) { rewardItem ->
                AnalyticsManager.logAdViewed("rewarded_double_gems")
                onRewarded()
            }
        } else {
            onRewarded()
            loadRewardedDoubleGems(activity)
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

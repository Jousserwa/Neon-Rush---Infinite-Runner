package com.neonrush.game

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase

object AnalyticsManager {
    private var firebaseAnalytics: FirebaseAnalytics? = null

    fun initialize(context: Context) {
        firebaseAnalytics = Firebase.analytics
    }

    fun logGameStart() {
        firebaseAnalytics?.logEvent("game_start", null)
    }

    fun logGameOver(score: Int, isNewPB: Boolean, zoneReached: Int) {
        val params = Bundle().apply {
            putInt("score", score)
            putBoolean("is_new_pb", isNewPB)
            putInt("zone_reached", zoneReached)
        }
        firebaseAnalytics?.logEvent("game_over", params)
    }

    fun logAdViewed(adType: String) {
        val params = Bundle().apply {
            putString("ad_type", adType) // "rewarded" or "interstitial"
        }
        firebaseAnalytics?.logEvent("ad_viewed", params)
    }

    fun logPurchaseAttempted(productId: String) {
        val params = Bundle().apply {
            putString(FirebaseAnalytics.Param.ITEM_ID, productId)
        }
        firebaseAnalytics?.logEvent("purchase_attempted", params)
    }

    fun logPurchaseCompleted(productId: String) {
        val params = Bundle().apply {
            putString(FirebaseAnalytics.Param.ITEM_ID, productId)
        }
        firebaseAnalytics?.logEvent(FirebaseAnalytics.Event.PURCHASE, params)
    }

    fun logScoreMilestone(milestone: Int) {
        val params = Bundle().apply {
            putInt("milestone", milestone)
        }
        firebaseAnalytics?.logEvent("score_milestone", params)
    }
}

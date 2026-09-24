package com.neonrush.game

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import com.neonrush.game.db.GameDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class GameUiState(
    val gems: Int = 0,
    val isAdsDisabled: Boolean = false,
    val unlockedSkins: Set<String> = setOf("default_skin"),
    val activeSkin: String = "default_skin"
)

class NeonRushViewModel(
    private val gameDao: GameDao,
    private val context: Context
) : ViewModel() {

    companion object {
        private const val TAG = "NeonRushViewModel"
        const val STARTER_PACK_GEMS_AMOUNT = 500
    }

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    // --- REVENUECAT IN-APP PURCHASE HANDLERS ---

    fun purchaseGemPack(activity: Activity, amount: Int, packageId: String) {
        RevenueCatManager.purchasePackage(
            activity = activity,
            packageIdentifier = packageId,
            onSuccess = {
                addGems(amount)
            },
            onError = { error ->
                Log.e(TAG, "Gem pack purchase failed: $error")
            }
        )
    }

    fun purchaseStarterPack(activity: Activity) {
        RevenueCatManager.purchasePackage(
            activity = activity,
            packageIdentifier = "starter_pack",
            onSuccess = {
                addGems(STARTER_PACK_GEMS_AMOUNT)
                setAdsDisabled(true)
            },
            onError = { error ->
                Log.e(TAG, "Starter pack purchase failed: $error")
            }
        )
    }

    fun purchaseRemoveAds(activity: Activity) {
        RevenueCatManager.purchasePackage(
            activity = activity,
            packageIdentifier = "remove_ads",
            onSuccess = {
                setAdsDisabled(true)
            },
            onError = { error ->
                Log.e(TAG, "Remove ads purchase failed: $error")
            }
        )
    }

    fun purchasePilotSuit(activity: Activity, skinId: String) {
        RevenueCatManager.purchasePackage(
            activity = activity,
            packageIdentifier = skinId,
            onSuccess = {
                unlockSkin(skinId)
            },
            onError = { error ->
                Log.e(TAG, "Pilot suit purchase failed: $error")
            }
        )
    }

    // --- STATE MUTATIONS ---

    fun addGems(amount: Int) {
        _uiState.update { it.copy(gems = it.gems + amount) }
    }

    fun setAdsDisabled(disabled: Boolean) {
        _uiState.update { it.copy(isAdsDisabled = disabled) }
    }

    fun unlockSkin(skinId: String) {
        _uiState.update { 
            it.copy(
                unlockedSkins = it.unlockedSkins + skinId,
                activeSkin = skinId 
            ) 
        }
    }
}

package com.neonrush.game

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neonrush.game.db.GameDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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

    init {
        loadUserData()
    }

    private fun loadUserData() {
        viewModelScope.launch {
            try {
                // Fetch cached state or load initial values from Database / DataStore
                val initialGems = gameDao.getGemsCount() ?: 0
                val adsRemoved = gameDao.areAdsRemoved() ?: false
                val savedSkins = gameDao.getUnlockedSkins() ?: setOf("default_skin")

                _uiState.update { currentState ->
                    currentState.copy(
                        gems = initialGems,
                        isAdsDisabled = adsRemoved,
                        unlockedSkins = savedSkins
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading user data: ${e.message}")
            }
        }
    }

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

    // --- STATE MUTATION & PERSISTENCE ---

    fun addGems(amount: Int) {
        viewModelScope.launch {
            val updatedGems = _uiState.value.gems + amount
            _uiState.update { it.copy(gems = updatedGems) }
            gameDao.saveGemsCount(updatedGems)
        }
    }

    fun setAdsDisabled(disabled: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAdsDisabled = disabled) }
            gameDao.saveAdsRemoved(disabled)
        }
    }

    fun unlockSkin(skinId: String) {
        viewModelScope.launch {
            val updatedSkins = _uiState.value.unlockedSkins + skinId
            _uiState.update { 
                it.copy(
                    unlockedSkins = updatedSkins,
                    activeSkin = skinId 
                ) 
            }
            gameDao.saveUnlockedSkins(updatedSkins)
        }
    }
}

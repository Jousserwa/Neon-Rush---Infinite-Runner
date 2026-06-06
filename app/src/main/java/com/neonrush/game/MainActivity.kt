package com.neonrush.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.neonrush.game.db.GameDao
import com.neonrush.game.ui.NeonRushApp
import com.neonrush.game.ui.theme.NeonRushTheme

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: NeonRushViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Services Safely to Prevent Startup Crashes
        try {
            NeonSoundEngine.init(applicationContext)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            AdMobManager.init(applicationContext)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            FirebaseLeaderboardManager.init(applicationContext)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            RevenueCatManager.init(applicationContext)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val gameDao = GameDao(applicationContext)
        viewModel = NeonRushViewModel(gameDao, applicationContext)

        intent?.data?.let { uri ->
            if (uri.scheme == "neonrush" && uri.host == "companion") {
                val challengeId = uri.getQueryParameter("challenge")
                if (!challengeId.isNullOrEmpty()) {
                    viewModel.toggleFollowUser("RetroWave")
                }
            }
        }

        setContent {
            NeonRushTheme {
                NeonRushApp(viewModel = viewModel)
            }
        }
    }
}

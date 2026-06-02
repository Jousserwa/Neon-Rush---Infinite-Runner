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

        // Initialize Services
        NeonSoundEngine.init(applicationContext)
        AdMobManager.init(applicationContext)
        FirebaseLeaderboardManager.init(applicationContext)
        RevenueCatManager.init(applicationContext)

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

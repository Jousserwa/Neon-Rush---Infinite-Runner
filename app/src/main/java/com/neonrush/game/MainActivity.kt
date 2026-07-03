package com.neonrush.game

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.neonrush.game.db.GameDao
import com.neonrush.game.ui.NeonRushApp
import com.neonrush.game.ui.theme.NeonRushTheme

class MainActivity : ComponentActivity() {

    private val viewModel: NeonRushViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val gameDao = GameDao(applicationContext)
                return NeonRushViewModel(gameDao, applicationContext) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            NeonRushTheme {
                NeonRushApp(viewModel = viewModel)
            }
        }
    }
}

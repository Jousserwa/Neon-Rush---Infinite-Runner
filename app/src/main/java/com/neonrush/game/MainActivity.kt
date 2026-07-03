package com.neonrush.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.neonrush.game.ui.NeonRushApp
import com.neonrush.game.ui.theme.NeonRushTheme

class MainActivity : ComponentActivity() {

    private val viewModel: NeonRushViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            NeonRushTheme {
                NeonRushApp(viewModel = viewModel)
            }
        }
    }
}

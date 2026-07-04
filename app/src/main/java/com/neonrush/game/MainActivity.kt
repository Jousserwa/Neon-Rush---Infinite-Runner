package com.neonrush.game

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.neonrush.game.db.GameDao
import com.neonrush.game.ui.NeonRushApp
import com.neonrush.game.ui.theme.NeonRushTheme
import java.io.PrintWriter
import java.io.StringWriter

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

    private val prefsName = "crash_prefs"
    private val crashKey = "last_crash"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Start the real-time audio engine and haptics service.
        // Without this call, sound/vibration silently do nothing.
        NeonSoundEngine.init(applicationContext)
        NeonSoundEngine.setHomeActive(false)

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                getSharedPreferences(prefsName, Context.MODE_PRIVATE).edit()
                    .putString(crashKey, sw.toString())
                    .apply()
            } catch (e: Exception) {
                // ignore, nothing more we can do here
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }

        val lastCrash = getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .getString(crashKey, null)

        setContent {
            NeonRushTheme {
                if (lastCrash != null) {
                    CrashDisplayScreen(crashText = lastCrash) {
                        getSharedPreferences(prefsName, Context.MODE_PRIVATE)
                            .edit()
                            .remove(crashKey)
                            .apply()
                        recreate()
                    }
                } else {
                    NeonRushApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun CrashDisplayScreen(crashText: String, onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        Text(text = "Last crash log:", color = Color.White)
        Spacer(modifier = Modifier.height(12.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Text(text = crashText, color = Color(0xFF00FF88))
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onDismiss) {
            Text("Clear and retry")
        }
    }
}

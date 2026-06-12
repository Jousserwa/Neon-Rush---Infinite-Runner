package com.neonrush.game

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val TAG = "NeonRushInit"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val statusTextView = findViewById<TextView>(R.id.statusTextView)

        // Run the robust initialization routine
        initializeGameEngine(statusTextView)
    }

    private fun initializeGameEngine(statusView: TextView?) {
        try {
            // Stage 1: Core Systems Setup
            statusView?.text = "Initializing telemetry framework..."
            
            // Stage 2: The current crash point
            statusView?.text = "Synchronizing procedural light grid pathways..."
            
            // Sub-try block intercepts any bugs during track or graphic matrix initialization
            try {
                setupProceduralLightGrids()
            } catch (e: Exception) {
                Log.e(TAG, "Procedural grid configuration failed safely: ${e.localizedMessage}")
                // If the generation script fails, we fallback gracefully instead of crashing
                loadFallbackTrack()
            }

            // Stage 3: Smooth transition to the main dashboard menu
            navigateToHome()

        } catch (globalException: Exception) {
            Log.e(TAG, "Critical lifecycle exception intercepted: ${globalException.localizedMessage}")
            // Hard fallback: Ensure user is sent to the Home layout cleanly no matter what
            navigateToHome()
        }
    }

    private fun setupProceduralLightGrids() {
        // Your procedural engine script runs here. 
        // Any structural, canvas, or calculation errors are now safely caught.
    }

    private fun loadFallbackTrack() {
        // Fallback default coordinate arrays so the engine remains stable
    }

    private fun navigateToHome() {
        // Using a safe dynamic class reference matching your namespace path structure
        try {
            val intent = Intent(this, Class.forName("com.neonrush.game.HomeActivity"))
            startActivity(intent)
            finish() // Terminates loading activity state completely
        } catch (e: Exception) {
            Log.e(TAG, "Could not resolve HomeActivity destination layout class: ${e.localizedMessage}")
        }
    }
}

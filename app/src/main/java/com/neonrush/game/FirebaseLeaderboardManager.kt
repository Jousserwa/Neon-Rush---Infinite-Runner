package com.neonrush.game

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

data class LeaderboardPilot(
    val name: String = "",
    val bestScore: Int = 0,
    val activeZone: String = "",
    val rank: Int = 0,
    val isFollowed: Boolean = false
)

data class SocialComment(
    val username: String = "",
    val comment: String = "",
    val timeAgo: String = "Just now"
)

class FirebaseLeaderboardManager(context: Context) {
    // Auto-initialized from google-services.json — no manual config needed
    private val db = FirebaseFirestore.getInstance()
    private val _leaderboard = MutableStateFlow<List<LeaderboardPilot>>(emptyList())
    val leaderboard: StateFlow<List<LeaderboardPilot>> = _leaderboard.asStateFlow()

    private val _socialComments = MutableStateFlow<List<SocialComment>>(emptyList())
    val socialComments: StateFlow<List<SocialComment>> = _socialComments.asStateFlow()

    init {
        loadMockLeaderboard()
        loadMockSocialComments()
    }

    private fun loadMockLeaderboard() {
        _leaderboard.value = listOf(
            LeaderboardPilot("CyberRunner", 2450, "Golden Age", 1, false),
            LeaderboardPilot("ZeroGlitch", 1980, "Neon Front", 2, true),
            LeaderboardPilot("RetroWave", 1540, "Blackout", 3, true),
            LeaderboardPilot("NeonPilot_99", 1200, "Storm Zone 3", 4, false),
            LeaderboardPilot("GlitchHunter", 980, "Derelict Signal", 5, false),
            LeaderboardPilot("VoidWalker", 850, "Cell Block Zero", 6, false),
            LeaderboardPilot("ChromeReaper", 720, "Green Hell", 7, false),
            LeaderboardPilot("SignalGhost", 640, "Neon Front", 8, false),
            LeaderboardPilot("ToxicBloom", 510, "Blackout", 9, false),
            LeaderboardPilot("CircuitBreaker", 480, "Golden Age", 10, false)
        )
    }

    private fun loadMockSocialComments() {
        _socialComments.value = listOf(
            SocialComment("CyberRunner", "Just hit 2450 on Golden Age! The new update is insane.", "2m ago"),
            SocialComment("ZeroGlitch", "Anyone else notice the ghost trails are faster now?", "15m ago"),
            SocialComment("RetroWave", "Finally unlocked Chrome Reaper skin. Worth every gem.", "1h ago"),
            SocialComment("NeonPilot_99", "Daily challenge is brutal today. Only 2 attempts left!", "2h ago"),
            SocialComment("GlitchHunter", "Pro tip: save your shield for Zone 5. Trust me.", "3h ago"),
            SocialComment("VoidWalker", "Just bought the monthly pass. No more ads!", "5h ago"),
            SocialComment("SignalGhost", "The audio engine in this game is unreal. Haptic feedback on point.", "8h ago"),
            SocialComment("ToxicBloom", "Anyone want to race ghost telemetry? I'm online now.", "12h ago")
        )
    }

    suspend fun submitScore(username: String, score: Int, zone: String) {
        try {
            val data = hashMapOf(
                "username" to username,
                "bestScore" to score,
                "activeZone" to zone,
                "timestamp" to System.currentTimeMillis()
            )
            db.collection("leaderboard").document(username).set(data).await()
        } catch (e: Exception) {
            // Silently fail — leaderboard is non-critical
        }
    }

    suspend fun fetchLeaderboard() {
        try {
            val snapshot = db.collection("leaderboard")
                .orderBy("bestScore", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .await()

            val pilots = snapshot.documents.mapIndexed { index, doc ->
                LeaderboardPilot(
                    name = doc.getString("username") ?: "",
                    bestScore = doc.getLong("bestScore")?.toInt() ?: 0,
                    activeZone = doc.getString("activeZone") ?: "",
                    rank = index + 1,
                    isFollowed = false
                )
            }
            if (pilots.isNotEmpty()) {
                _leaderboard.value = pilots
            }
        } catch (e: Exception) {
            // Keep mock data if Firebase fails
        }
    }
}

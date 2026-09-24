package com.neonrush.game

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

// NOTE: LeaderboardPilot and SocialComment are defined in NeonRushViewModel.kt
// Do NOT redefine them here to avoid duplicate class errors

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
            LeaderboardPilot(id = 1, name = "CyberRunner", score = 2450, bestScore = 2450, activeZone = "Golden Age", activeSkinId = "cyan_diamond", isFollowed = false, isBot = true, challengeId = "ghost_cyberrunner"),
            LeaderboardPilot(id = 2, name = "ZeroGlitch", score = 1980, bestScore = 1980, activeZone = "Neon Front", activeSkinId = "purple_square", isFollowed = true, isBot = true, challengeId = "ghost_zeroglitch"),
            LeaderboardPilot(id = 3, name = "RetroWave", score = 1540, bestScore = 1540, activeZone = "Blackout", activeSkinId = "green_triangle", isFollowed = true, isBot = true, challengeId = "ghost_retro"),
            LeaderboardPilot(id = 4, name = "NeonPilot_99", score = 1200, bestScore = 1200, activeZone = "Storm Zone 3", activeSkinId = "cyan_diamond", isFollowed = false, isBot = false, challengeId = "ghost_user"),
            LeaderboardPilot(id = 5, name = "GlitchHunter", score = 980, bestScore = 980, activeZone = "Derelict Signal", activeSkinId = "magenta_pulse", isFollowed = false, isBot = true, challengeId = "ghost_glitch"),
            LeaderboardPilot(id = 6, name = "VoidWalker", score = 850, bestScore = 850, activeZone = "Cell Block Zero", activeSkinId = "green_triangle", isFollowed = false, isBot = true, challengeId = "ghost_void"),
            LeaderboardPilot(id = 7, name = "ChromeReaper", score = 720, bestScore = 720, activeZone = "Green Hell", activeSkinId = "purple_square", isFollowed = false, isBot = true, challengeId = "ghost_chrome"),
            LeaderboardPilot(id = 8, name = "SignalGhost", score = 640, bestScore = 640, activeZone = "Neon Front", activeSkinId = "cyan_diamond", isFollowed = false, isBot = true, challengeId = "ghost_signal"),
            LeaderboardPilot(id = 9, name = "ToxicBloom", score = 510, bestScore = 510, activeZone = "Blackout", activeSkinId = "magenta_pulse", isFollowed = false, isBot = true, challengeId = "ghost_toxic"),
            LeaderboardPilot(id = 10, name = "CircuitBreaker", score = 480, bestScore = 480, activeZone = "Golden Age", activeSkinId = "green_triangle", isFollowed = false, isBot = true, challengeId = "ghost_circuit")
        )
    }

    private fun loadMockSocialComments() {
        val now = System.currentTimeMillis()
        _socialComments.value = listOf(
            SocialComment(id = "comment_1", author = "CyberRunner", text = "Just hit 2450 on Golden Age! The new update is insane.", timestamp = now - 2 * 60_000L, zone = "Golden Age"),
            SocialComment(id = "comment_2", author = "ZeroGlitch", text = "Anyone else notice the ghost trails are faster now?", timestamp = now - 15 * 60_000L, zone = "Neon Front"),
            SocialComment(id = "comment_3", author = "RetroWave", text = "Finally unlocked Chrome Reaper skin. Worth every gem.", timestamp = now - 60 * 60_000L, zone = "Blackout"),
            SocialComment(id = "comment_4", author = "NeonPilot_99", text = "Daily challenge is brutal today. Only 2 attempts left!", timestamp = now - 2 * 60 * 60_000L, zone = "Storm Zone 3"),
            SocialComment(id = "comment_5", author = "GlitchHunter", text = "Pro tip: save your shield for Zone 5. Trust me.", timestamp = now - 3 * 60 * 60_000L, zone = "Derelict Signal"),
            SocialComment(id = "comment_6", author = "VoidWalker", text = "Just bought the monthly pass. No more ads!", timestamp = now - 5 * 60 * 60_000L, zone = "Cell Block Zero"),
            SocialComment(id = "comment_7", author = "SignalGhost", text = "The audio engine in this game is unreal. Haptic feedback on point.", timestamp = now - 8 * 60 * 60_000L, zone = "Neon Front"),
            SocialComment(id = "comment_8", author = "ToxicBloom", text = "Anyone want to race ghost telemetry? I'm online now.", timestamp = now - 12 * 60 * 60_000L, zone = "Blackout")
        )
    }

    suspend fun submitScore(username: String, score: Int, activeSkinId: String) {
        try {
            val data = hashMapOf(
                "username" to username,
                "bestScore" to score,
                "activeSkinId" to activeSkinId,
                "timestamp" to System.currentTimeMillis()
            )
            db.collection("leaderboard").document(username).set(data).await()
        } catch (e: Exception) {
            // Silently fail — leaderboard is non-critical
        }
    }

    suspend fun fetchTopScores() {
        try {
            val snapshot = db.collection("leaderboard")
                .orderBy("bestScore", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .await()

            val pilots = snapshot.documents.mapIndexed { index, doc ->
                LeaderboardPilot(
                    rank = index + 1,
                    name = doc.getString("username") ?: "",
                    bestScore = doc.getLong("bestScore") ?: 0L,
                    activeZone = "Unknown",
                    activeSkinId = doc.getString("activeSkinId") ?: "cyan_diamond",
                    isFollowed = false,
                    isBot = false,
                    challengeId = ""
                )
            }
            if (pilots.isNotEmpty()) {
                _leaderboard.value = pilots
            }
        } catch (e: Exception) {
            // Keep mock data if Firebase fails
        }
    }

    companion object {
        // Singleton instance for ViewModel access
        private lateinit var instance: FirebaseLeaderboardManager
        
        val globalRankings: StateFlow<List<LeaderboardPilot>>
            get() = instance.leaderboard
        
        fun initialize(context: Context) {
            instance = FirebaseLeaderboardManager(context)
        }
        
        suspend fun submitScore(username: String, score: Int, activeSkinId: String) {
            instance.submitScore(username, score, activeSkinId)
        }
        
        suspend fun fetchTopScores() {
            instance.fetchTopScores()
        }
    }
}


package com.neonrush.game

data class LeaderboardPilot(
    val id: String = "",
    val name: String = "Unknown Pilot",
    val score: String = "0",
    val bestScore: String = "0",
    val activeZone: String = "",
    val activeSkinId: String = "default_skin",
    val isFollowed: Boolean = false,
    val isBot: Boolean = false,
    val rank: Long = 0L,
    val challengeId: String? = null
)

data class SocialComment(
    val id: String = "",
    val author: String = "Anonymous",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

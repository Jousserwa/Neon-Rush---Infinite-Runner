package com.neonrush.game

data class LeaderboardPilot(
    val id: Int = 0,
    val name: String = "Unknown Pilot",
    val score: Long = 0L,
    val bestScore: Long = 0L,
    val activeZone: String = "",
    val activeSkinId: String = "default_skin",
    val isFollowed: Boolean = false,
    val isBot: Boolean = false,
    val rank: Int = 0,
    val challengeId: String? = null
)

data class SocialComment(
    val id: String = "",
    val author: String = "Anonymous",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

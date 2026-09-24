package com.neonrush.game

data class LeaderboardPilot(
    val rank: Int = 0,
    val id: String = "",
    val name: String = "Unknown Pilot",
    val score: Long = 0L,
    val bestScore: Long = 0L,
    val activeZone: String = "",
    val activeSkinId: String = "default_skin",
    val isFollowed: Boolean = false,
    val isBot: Boolean = false,
    val challengeId: String? = null
)

data class SocialComment(
    val id: String = "",
    val author: String = "Anonymous",
    val text: String = "",
    val timestamp: String = ""
)

package com.neonrush.game

data class LeaderboardPilot(
    val id: String = "",
    val name: String = "Unknown Pilot",
    val score: Long = 0L,
    val skinId: String = "default_skin"
)

data class SocialComment(
    val id: String = "",
    val author: String = "Anonymous",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

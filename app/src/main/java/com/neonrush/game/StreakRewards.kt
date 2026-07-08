package com.neonrush.game

data class StreakReward(
    val day: Int,
    val gems: Int,
    val label: String
)

object StreakRewards {
    val ALL: List<StreakReward> = listOf(
        StreakReward(1, 5, "Day 1"),
        StreakReward(2, 8, "Day 2"),
        StreakReward(3, 10, "Day 3"),
        StreakReward(4, 15, "Day 4"),
        StreakReward(5, 20, "Day 5"),
        StreakReward(6, 25, "Day 6"),
        StreakReward(7, 35, "Day 7 — Full Streak!")
    )

    fun rewardForDay(day: Int): StreakReward {
        // Cycles back to day 1 after day 7 (day 8 = day 1's reward, day 9 = day 2's, etc.)
        val cyclePosition = ((day - 1) % 7) + 1
        return ALL.first { it.day == cyclePosition }
    }
}

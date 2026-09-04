package com.neonrush.game

import java.util.Calendar

enum class MutationDay(
    val title: String,
    val description: String,
    val emoji: String,
    val colorHex: String
) {
    MONDAY("DOUBLE GEMS", "Double all gem rewards on flight run accomplishments today!", "💎💎", "#FFD23F"),
    TUESDAY("SPEED SURGE", "+20% speed boost! Extreme rocket velocity stream calibration active.", "⚡", "#3A86FF"),
    WEDNESDAY("POWERUP PARTY", "Powerup multiplier enabled! Match ghost paths for extra points.", "🎉", "#FF006E"),
    THURSDAY("MIRROR WORLD", "Lateral inverted coordinate paths. Left-right offsets are flipped!", "🔮", "#8338EC"),
    FRIDAY("GOLDEN HOUR", "Gems spark golden trails. +5 bonus gems on run completion.", "🌅", "#F72585"),
    SATURDAY("BOSS RUSH", "Extreme high speed Boss battles active in all 5th tier zones.", "👾", "#FF0055"),
    SUNDAY("LEGENDARY DAY", "Maximum high-score prestige multipliers enabled globally.", "🏆", "#00F5D4")
}

object DailyMutations {
    fun getActiveMutation(): MutationDay {
        val calendar = Calendar.getInstance()
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> MutationDay.MONDAY
            Calendar.TUESDAY -> MutationDay.TUESDAY
            Calendar.WEDNESDAY -> MutationDay.WEDNESDAY
            Calendar.THURSDAY -> MutationDay.THURSDAY
            Calendar.FRIDAY -> MutationDay.FRIDAY
            Calendar.SATURDAY -> MutationDay.SATURDAY
            Calendar.SUNDAY -> MutationDay.SUNDAY
            else -> MutationDay.MONDAY // Fallback default
        }
    }
}

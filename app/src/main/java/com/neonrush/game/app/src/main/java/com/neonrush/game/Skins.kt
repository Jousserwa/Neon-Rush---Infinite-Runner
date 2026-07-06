package com.neonrush.game

data class Skin(
    val id: String,
    val name: String,
    val description: String,
    val previewImageRes: Int,
    val priceUsd: String,
    val requiresPro: Boolean,
    val unlockWorldId: Int?, // null = no story unlock requirement, purchasable anytime
    val pilotFrameOverrides: List<Int>? = null // null = uses default pilot_run_1..6 frames
)

object Skins {
    val ALL: List<Skin> = listOf(
        Skin(
            id = "default",
            name = "Standard Issue",
            description = "The pilot's original gear. Reliable, unremarkable, free.",
            previewImageRes = R.drawable.pilot_run_1,
            priceUsd = "Free",
            requiresPro = false,
            unlockWorldId = null
        )
        // Additional skins added here as art becomes available
    )

    fun isUnlocked(skin: Skin, isPro: Boolean, worldsCompleted: Set<Int>): Boolean {
        if (skin.unlockWorldId == null) return true
        if (skin.requiresPro && !isPro) return false
        return worldsCompleted.contains(skin.unlockWorldId)
    }
}

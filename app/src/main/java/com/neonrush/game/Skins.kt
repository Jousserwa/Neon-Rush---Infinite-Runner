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
        ),
        Skin(
            id = "blackout_runner",
            name = "Blackout Runner",
            description = "Salvaged war-front plating. Earned by clearing Blackout Front.",
            previewImageRes = R.drawable.pilot_run_1, // placeholder — swap when art exists
            priceUsd = "Free (story reward)",
            requiresPro = false,
            unlockWorldId = 1
        ),
        Skin(
            id = "signal_ghost",
            name = "Signal Ghost",
            description = "Something from the station clings to this suit. Earned by clearing Derelict Signal.",
            previewImageRes = R.drawable.pilot_run_1, // placeholder
            priceUsd = "Free (story reward)",
            requiresPro = false,
            unlockWorldId = 2
        ),
        Skin(
            id = "convict_grey",
            name = "Convict Grey",
            description = "Prison-issue fatigues, worn since the break. Earned by clearing Cell Block Zero.",
            previewImageRes = R.drawable.pilot_run_1, // placeholder
            priceUsd = "Free (story reward)",
            requiresPro = false,
            unlockWorldId = 3
        ),
        Skin(
            id = "apex_predator",
            name = "Apex Predator",
            description = "You outran the thing that was hunting you. Earned by clearing Green Hell.",
            previewImageRes = R.drawable.pilot_run_1, // placeholder
            priceUsd = "Free (story reward)",
            requiresPro = true,
            unlockWorldId = 4
        ),
        Skin(
            id = "chrome_reaper",
            name = "Chrome Reaper",
            description = "No story behind this one. Just looks incredible.",
            previewImageRes = R.drawable.pilot_run_1, // placeholder
            priceUsd = "$1.99",
            requiresPro = false,
            unlockWorldId = null
        ),
        Skin(
            id = "solar_flare",
            name = "Solar Flare",
            description = "Blindingly bright. Pure vanity.",
            previewImageRes = R.drawable.pilot_run_1, // placeholder
            priceUsd = "$1.99",
            requiresPro = false,
            unlockWorldId = null
        ),
        Skin(
            id = "void_walker",
            name = "Void Walker",
            description = "Matte black plating that seems to swallow the neon around it.",
            previewImageRes = R.drawable.pilot_run_1, // placeholder
            priceUsd = "$1.99",
            requiresPro = false,
            unlockWorldId = null
        ),
        Skin(
            id = "toxic_bloom",
            name = "Toxic Bloom",
            description = "Acid-green venting, straight out of the reserve's worst nightmares.",
            previewImageRes = R.drawable.pilot_run_1, // placeholder
            priceUsd = "$2.99",
            requiresPro = false,
            unlockWorldId = null
        ),
        Skin(
            id = "circuit_breaker",
            name = "Circuit Breaker",
            description = "Live current running through every seam. Handle with care.",
            previewImageRes = R.drawable.pilot_run_1, // placeholder
            priceUsd = "$2.99",
            requiresPro = false,
            unlockWorldId = null
        ),
        Skin(
            id = "golden_protocol",
            name = "Golden Protocol",
            description = "Command-grade gold plating. Reserved for the pilots who never miss.",
            previewImageRes = R.drawable.pilot_run_1, // placeholder
            priceUsd = "$3.99",
            requiresPro = false,
            unlockWorldId = null
        ),
        Skin(
            id = "glitch_core",
            name = "Glitch Core",
            description = "Something's wrong with this suit's signal — and it looks incredible.",
            previewImageRes = R.drawable.pilot_run_1, // placeholder
            priceUsd = "$3.99",
            requiresPro = false,
            unlockWorldId = null
        )
    )

    fun isUnlocked(skin: Skin, isPro: Boolean, worldsCompleted: Set<Int>): Boolean {
        if (skin.unlockWorldId == null) return true
        if (skin.requiresPro && !isPro) return false
        return worldsCompleted.contains(skin.unlockWorldId)
    }
}

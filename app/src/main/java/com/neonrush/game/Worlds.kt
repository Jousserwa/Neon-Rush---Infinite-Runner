package com.neonrush.game

data class World(
    val id: Int,
    val title: String,
    val subtitle: String,
    val startZone: Int,
    val endZone: Int,
    val openingText: String,
    val midRunText: String,
    val bossIntroText: String,
    val endingText: String,
    val requiresPro: Boolean,
    val environmentIds: List<Int> // indices into ZoneGenerator.ENVIRONMENTS matching this world's theme
)
object Worlds {

    val ALL: List<World> = listOf(
        World(
            id = 1,
            title = "BLACKOUT FRONT",
            subtitle = "Military Extraction",
            startZone = 1,
            endZone = 8,
            openingText = "Extraction job, sector Blackout Front. Enemy lines just collapsed — you're the last one still moving. Get to the rally point.",
            midRunText = "Radio crackles: \"Runner, they've got armor inbound. Don't stop for anything.\"",
            bossIntroText = "A war-drone locks onto your heat signature — built to hunt stragglers.",
            endingText = "You clear the front line as the sky lights up behind you. Command's already got your next job queued.",
            requiresPro = false,
            environmentIds = listOf(0, 5, 7) // Neon City, Electric Storm, Ancient Ruins
        ),
        
        World(
            id = 2,
            title = "DERELICT SIGNAL",
            subtitle = "Sci-Fi Retrieval",
            startZone = 9,
            endZone = 16,
            openingText = "Job's simple: retrieve the core from a research station. One problem — something's still awake in there.",
            midRunText = "The station's AI whispers through the comms: \"You should not have come back.\"",
            bossIntroText = "Whatever's guarding the core isn't human anymore — and it's fast.",
            endingText = "The station goes dark behind you for good. What you're carrying... you're not sure you want to know.",
            requiresPro = false,
            environmentIds = listOf(1, 4, 14) // Deep Space, Void Realm, Dimensional Rift
        
        ),
        World(
            id = 3,
            title = "CELL BLOCK ZERO",
            subtitle = "Prison Break",
            startZone = 17,
            endZone = 24,
            openingText = "Wrongfully locked up, or maybe not — doesn't matter now. Riot's started. This is your only window out.",
            midRunText = "Guards seal the east wing. \"Runner's loose! Lock it down!\"",
            bossIntroText = "The warden himself blocks the final gate — and he's not going down easy.",
            endingText = "You clear the wall as sirens fade behind you. Free — for now.",
            requiresPro = false,
            environmentIds = listOf(10, 2, 12) // Shadow World, Crystal Cave, Frozen Tundra
        
        ),
        World(
            id = 4,
            title = "GREEN HELL",
            subtitle = "Wild Escape",
            startZone = 25,
            endZone = 32,
            openingText = "Chopper went down deep in the reserve. Something big found the wreckage before rescue did.",
            midRunText = "Something's pacing you through the trees — matching your speed, staying just out of sight.",
            bossIntroText = "It finally shows itself — and it's been hunting you since you landed.",
            endingText = "You break the treeline as the jungle goes quiet behind you. You made it. Barely.",
            requiresPro = true,
            environmentIds = listOf(8, 6, 3) // Cyber Garden, Ocean Deep, Lava Forge
        ),
        World(
            id = 5,
            title = "RED PROTOCOL",
            subtitle = "Citywide Manhunt",
            startZone = 33,
            endZone = 40,
            openingText = "Your face just hit every screen in the city. RED PROTOCOL is active — every drone, every camera, every door is against you now.",
            midRunText = "Command channel, encrypted: \"They've sealed the outer district. You have one route left — through the transit spine.\"",
            bossIntroText = "A hunter-class enforcer drops from the skyline, already locked onto your signal.",
            endingText = "You clear the city limits as the alert finally goes dark behind you. For now, Red Protocol is over. For you, it never really ends.",
            requiresPro = true,
            environmentIds = listOf(0, 13, 9) // Neon City, Plasma Field, Prismatic
        ),
        World(
    id = 6,
    title = "SIGNAL FRACTURE",
    subtitle = "Data Heist",
    startZone = 41,
    endZone = 48,
    openingText = "Red Protocol went dark, but someone's been watching the whole time. They want what's in your head — the run data. Time to disappear into the wire.",
    midRunText = "A voice bleeds through every speaker you pass: \"We know where you're going.\"",
    bossIntroText = "A rogue trace-daemon breaches the tunnel — it's been hunting your signal for weeks.",
    endingText = "You slip the last firewall as the city's grid flickers back to normal. For now, you're a ghost again.",
    requiresPro = true,
    environmentIds = listOf(15) // Signal Fracture (dedicated new environment)
),
World(
    id = 7,
    title = "FROZEN VEIL",
    subtitle = "Arctic Infiltration",
    startZone = 49,
    endZone = 56,
    openingText = "Coordinates lead north, to a research site buried under a century of ice. Something down there was never meant to surface.",
    midRunText = "The facility's old intercom crackles: \"Site containment has failed. Do not proceed.\"",
    bossIntroText = "The ice cracks behind you — something enormous was sleeping just beneath it.",
    endingText = "You break the surface into blinding white, the facility collapsing into the dark below.",
    requiresPro = true,
    environmentIds = listOf(16) // Frozen Veil (dedicated new environment)
),
World(
    id = 5,
    title = "RED PROTOCOL",
    subtitle = "Citywide Manhunt",
    startZone = 33,
    endZone = 40,
    openingText = "Your face just hit every screen in the city. RED PROTOCOL is active — every drone, every camera, every door is against you now.",
    midRunText = "Command channel, encrypted: \"They've sealed the outer district. You have one route left — through the transit spine.\"",
    bossIntroText = "A hunter-class enforcer drops from the skyline, already locked onto your signal.",
    endingText = "You clear the city limits as the alert finally goes dark behind you. For now, Red Protocol is over. For you, it never really ends.",
    requiresPro = true,
    environmentIds = listOf(0, 13, 9) // Neon City, Plasma Field, Prismatic
)
    )

    // Special Mode worlds — not part of normal endless progression.
    // Unlocked permanently via mission-tier qualification (see MissionManager/ViewModel).
    val SPECIAL_WORLDS: List<World> = listOf(
        World(
            id = 6,
            title = "SIGNAL FRACTURE",
            subtitle = "Data Heist",
            startZone = 1,
            endZone = 999,
            openingText = "Red Protocol went dark, but someone's been watching the whole time. They want what's in your head — the run data. Time to disappear into the wire.",
            midRunText = "A voice bleeds through every speaker you pass: \"We know where you're going.\"",
            bossIntroText = "A rogue trace-daemon breaches the tunnel — it's been hunting your signal for weeks.",
            endingText = "You slip the last firewall as the city's grid flickers back to normal. For now, you're a ghost again.",
            requiresPro = false,
            environmentIds = listOf(15) // Signal Fracture (dedicated new environment)
        ),
        World(
            id = 7,
            title = "FROZEN VEIL",
            subtitle = "Arctic Infiltration",
            startZone = 1,
            endZone = 999,
            openingText = "Coordinates lead north, to a research site buried under a century of ice. Something down there was never meant to surface.",
            midRunText = "The facility's old intercom crackles: \"Site containment has failed. Do not proceed.\"",
            bossIntroText = "The ice cracks behind you — something enormous was sleeping just beneath it.",
            endingText = "You break the surface into blinding white, the facility collapsing into the dark below.",
            requiresPro = false,
            environmentIds = listOf(16) // Frozen Veil (dedicated new environment)
        ),
        World(
            id = 8,
            title = "APEX SIGNAL",
            subtitle = "Final Ascent",
            startZone = 1,
            endZone = 999,
            openingText = "Every job, every job you've ever run — it all led here. The Apex Signal. Whatever's broadcasting it built everything you've survived so far.",
            midRunText = "The signal speaks directly into your comms now: \"You were always going to come.\"",
            bossIntroText = "The source reveals itself — not human, not machine, something in between.",
            endingText = "The signal goes silent. For the first time since this all began, so does your mind.",
            requiresPro = false,
            environmentIds = listOf(17) // Apex Signal (dedicated new environment)
        )
    )

    fun worldForZone(zone: Int): World {
        return ALL.find { zone in it.startZone..it.endZone } ?: ALL.last()
    }

    fun specialWorldForTier(tier: Int): World? {
        return when (tier) {
            1 -> SPECIAL_WORLDS.getOrNull(0)
            2 -> SPECIAL_WORLDS.getOrNull(1)
            3 -> SPECIAL_WORLDS.getOrNull(2)
            else -> null
        }
    }

    fun isWorldUnlocked(world: World, isPro: Boolean): Boolean {
        return !world.requiresPro || isPro
    }
}

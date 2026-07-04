package com.neonrush.game

import kotlin.math.*
import kotlin.random.Random

data class ZoneInfo(
    val level: Int,
    val name: String,
    val description: String,
    val coreColorHex: String,
    val baselineSpeedKmh: Int,
    val difficultyMultiplier: Float,
    val hazardType: String
)

data class ZoneDNA(
    val zoneNumber: Int,
    val name: String,
    val description: String,
    val baseSpeed: Float,
    val obstacleSetId: Int, // 1..24
    val obstacleSetName: String,
    val mechanicIds: List<Int>, // 1..20
    val mechanicNames: List<String>,
    val environmentId: Int, // 1..16
    val environmentName: String,
    val environmentEmoji: String,
    val environmentColor: String, // Hex code color primary
    val rhythmPattern: List<Int>,
    val powerupDensity: Float,
    val bossGauntletAt: Int?, // Tick index for boss (from 24 to 40)
    val specialEventAt: Int?,
    val obstacleSpacingAndDensity: Int // spacing distance pixel
)

object ZoneGenerator {
    // 24 sets
    val OBSTACLE_SETS = listOf(
        "PILLARS", "LASERS", "BLADES", "STALACTITES", "STALAGMITES", "SANDWICH",
        "WAVES", "SPIRAL", "PULSE", "MAZE", "MIRROR", "CHAOS", "CRYSTALS",
        "VORTEX", "ZAP FIELD", "GRAVITY WELLS", "PHANTOM", "SPLITTER", "CHAIN",
        "FORTRESS", "WATERFALL", "TUNNEL", "GUARDIAN", "VOID"
    )

    // 20 mechanics
    val MECHANICS = listOf(
        "HYPERDRIVE", "GRAVITY FLIP", "GHOST MODE", "MAGNET", "MIRROR RUN",
        "SLOW MOTION", "BLIND SPOT", "DOUBLE TRACK", "STORM", "NEON OVERLOAD",
        "POWERUP SHOWER", "CRYSTAL MAZE", "VOID CORRIDOR", "ECHO CHAMBER", "BOSS GAUNTLET",
        "PRECISION RUN", "BULLET HELL", "RHYTHM ZONE", "SURVIVAL", "LEGENDARY RUN"
    )

    // 16 environments
    val ENVIRONMENTS = listOf(
        Triple("NEON CITY", "🌆", "#00E5FF"),
        Triple("DEEP SPACE", "🌌", "#9D00FF"),
        Triple("CRYSTAL CAVE", "❄️", "#00FFF0"),
        Triple("LAVA FORGE", "🌋", "#FF007F"),
        Triple("VOID REALM", "🌀", "#1A1A24"),
        Triple("ELECTRIC STORM", "⚡", "#FFD700"),
        Triple("OCEAN DEEP", "🐳", "#0088FF"),
        Triple("ANCIENT RUINS", "🏛️", "#8D99AE"),
        Triple("CYBER GARDEN", "🌿", "#00FF66"),
        Triple("PRISMATIC", "🌈", "#FF5EFF"),
        Triple("SHADOW WORLD", "🌘", "#111111"),
        Triple("GOLDEN AGE", "👑", "#D4AF37"),
        Triple("FROZEN TUNDRA", "🏔️", "#E0FAFF"),
        Triple("PLASMA FIELD", "🔋", "#FF00B4"),
        Triple("DIMENSIONAL RIFT", "👾", "#AE00FF"),
        Triple("TRANSCENDENT", "✨", "#FFFFFF")
    )

    fun calculateSpeed(zone: Int): Float {
        // Grace period: zones 1-3 stay flat so new players get their bearings
        val effectiveZone = (zone - 3).coerceAtLeast(0)
        val speed = 3.0f + (effectiveZone * 0.3f) - (effectiveZone * effectiveZone * 0.0015f)
        return speed.coerceAtMost(22.0f)
    }

    fun selectEnvironment(zone: Int): Int {
        if (zone >= 100) return 15 // Transcendent is ENV16 (0-indexed 15)
        return (zone * 7 + 13) % 15 // avoid ENV16 until zone 100
    }

    fun selectMechanics(zone: Int, random: Random): List<Int> {
        val count = when {
            zone >= 100 -> 3
            zone >= 50 -> 2
            else -> 1
        }
        if (zone in 1..4) return emptyList() // Tutorial zones: no forced hard mechanic
        if (zone == 5) return listOf(5) // ZM6 Slow Motion (5)
        if (zone == 10) return listOf(14) // ZM15 Boss Gauntlet (14)
        
        val pool = (0..19).toList()
        val list = mutableListOf<Int>()
        var tries = 0
        while (list.size < count && tries < 30) {
            val pick = pool[random.nextInt(pool.size)]
            // Ensure no duplicate or immediate previous rep (could check state, but since it's procedural seed, it's fine)
            if (!list.contains(pick)) {
                list.add(pick)
            }
            tries++
        }
        return list
    }

    fun generateZone(zone: Int, seed: Long): ZoneDNA {
        // Deterministic procedural generation based on zone index and seed
        val rand = Random(seed + zone * 1337 + 101)
        val envIdx = selectEnvironment(zone)
        val env = ENVIRONMENTS[envIdx]
        val obsSetId = (((zone * 1597 + seed) % 24) + 24) % 24      
        val obsSetName = OBSTACLE_SETS[obsSetId.toInt()]
        
        val mechIds = selectMechanics(zone, rand)
        val mechNames = mechIds.map { MECHANICS[it] }
        
        val rhythmicPattern = List(5 + (zone % 6)) { rand.nextInt(10, 30) }
        val density = (0.15f + (zone * 0.012f)).coerceAtMost(0.75f)
        
        // Spacing: Zone 1: 292px, Zone 10: 220px, Zone 25: 100px. Minimum 80px
        val obstacleSpacing = (300 - (zone * 8)).coerceAtLeast(80)
        
        return ZoneDNA(
            zoneNumber = zone,
            name = "${env.second} ${env.first} ($zone)",
            description = "Navigate extreme ${obsSetName} structures inside the high intensity holographic ${env.first} stream.",
            baseSpeed = calculateSpeed(zone),
            obstacleSetId = obsSetId.toInt() + 1,
            obstacleSetName = obsSetName,
            mechanicIds = mechIds.map { it + 1 },
            mechanicNames = mechNames,
            environmentId = envIdx + 1,
            environmentName = env.first,
            environmentEmoji = env.second,
            environmentColor = env.third,
            rhythmPattern = rhythmicPattern,
            powerupDensity = density,
            bossGauntletAt = if (zone % 5 == 0) 24 else null, // Boss appears in the final 200m (last 16 ticks) of every 5th zone
            specialEventAt = if (rand.nextBoolean()) rand.nextInt(5, 35) else null,
            obstacleSpacingAndDensity = obstacleSpacing
        )
    }

    // Dynamic ZoneInfo generator for backward-compatible properties in view models
    fun getZoneForScore(score: Int): ZoneInfo {
        val estimatedZone = (score / 150).coerceAtLeast(0) + 1
        return getZoneInfoForDistance((estimatedZone - 1) * 500f + 50f, 42)
    }

    fun getZoneInfoForDistance(distanceMeters: Float, seed: Long): ZoneInfo {
        val zone = (distanceMeters / 500f).toInt() + 1
        val dna = generateZone(zone, seed)
        return ZoneInfo(
            level = zone,
            name = dna.name,
            description = dna.description,
            coreColorHex = dna.environmentColor,
            baselineSpeedKmh = (dna.baseSpeed * 40).toInt(),
            difficultyMultiplier = 1.0f + (zone * 0.15f),
            hazardType = dna.obstacleSetName
        )
    }

    // Generate simulated y-positions list with 24 obstacle sets logic
    fun generateTelemetryCsv(score: Int, seed: Long): String {
        val count = 40
        val estimatedZone = (score / 150).coerceAtLeast(0) + 1
        val dna = generateZone(estimatedZone, seed)
        
        val random = java.util.Random(seed + estimatedZone)
        val list = mutableListOf<Int>()
        var y = 50 // starting center scale 10 to 90
        
        for (i in 0 until count) {
            // Apply different flight characteristics to ghost path depending on Obstacle Set
            val noise = random.nextGaussian()
            val drift = when (dna.obstacleSetId) {
                1 -> { // Pillars: tall rects, big gaps -> sudden vertical steps
                    if (i % 5 == 0) (noise * 18).toInt() else (noise * 4).toInt()
                }
                4 -> { // Stalactites: spikes from top, must fly low (force low position)
                    val base = -4 + (noise * 4).toInt()
                    if (y > 45) base - 10 else base
                }
                5 -> { // Stalagmites: spikes from bottom, must fly high (force high position)
                    val base = 4 + (noise * 4).toInt()
                    if (y < 55) base + 10 else base
                }
                6 -> { // Sandwich: top and bottom blocks -> thread narrow center corridor
                    val diff = 50 - y
                    (diff * 0.12f + noise * 3).toInt()
                }
                7 -> { // Waves: nice sine waves
                    val angle = (i * 2.0 * Math.PI / 10.0)
                    (sin(angle) * 15 + noise * 2).toInt() - (y - 50) / 4
                }
                8 -> { // Spiral: orbiting offsets
                    val angle = (i * 2.0 * Math.PI / 8.0)
                    (sin(angle) * 18 + cos(angle) * 10 + noise * 2).toInt() - (y - 50) / 4
                }
                22 -> { // Tunnel: narrow corridor shifting slowly upper and lower
                    val centerCorridor = 35 + ((i / 10) * 20)
                    ((centerCorridor - y) * 0.25f + noise * 2).toInt()
                }
                else -> (noise * 7).toInt()
            }
            
            y = (y + drift).coerceIn(15, 85)
            list.add(y)
        }
        return list.joinToString(",")
    }

    fun parseTelemetry(csv: String): List<Int> {
        if (csv.isEmpty()) return emptyList()
        return csv.split(",").mapNotNull { it.toIntOrNull() }
    }
}

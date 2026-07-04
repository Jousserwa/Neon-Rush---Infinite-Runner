package com.neonrush.game

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neonrush.game.db.GameDao
import com.neonrush.game.db.GameProfile
import com.neonrush.game.db.GhostChallengeEntity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class LeaderboardPilot(
    val rank: Int,
    val name: String,
    val bestScore: Int,
    val activeZone: String,
    val activeSkinId: String,
    val isFollowed: Boolean,
    val isBot: Boolean = true,
    val challengeId: String
)

data class SocialComment(
    val username: String,
    val comment: String,
    val timeAgo: String,
    val associatedZone: String
)

data class VisualTrackElement(
    val id: String,
    val xOffsetFraction: Float, // 1.2 is right edge, 0.2 is player column, -0.1 is off-screen left
    val yMatchPos: Int, // Y coordinate of target on track (10..90)
    val type: String, // "gem", "powerup", "obstacle", "bullet", "fuel"
    val subType: String = "", // "PU1".."PU12", "PILLAR", "LASER", "BLADE"
    val isCollected: Boolean = false
)

// Main active racing simulation state including full procedural and architectural properties
data class SimulationState(
    val activeGhost: GhostChallengeEntity? = null,
    val isCompleted: Boolean = false,
    val isStarted: Boolean = false,
    val tickIndex: Int = 0,
    val userYPath: List<Int> = emptyList(),
    val ghostYPath: List<Int> = emptyList(),
    val userYPos: Int = 50,
    val ghostYPos: Int = 50,
    val score: Int = 0,
    val currentZoneName: String = "Cyber Alley",
    val speedKmh: Int = 120,
    val fuelLevelPercent: Int = 100,
    val distanceMeters: Float = 0f,
    val feedbackMessage: String = "Ready to Sync Rushes",
    
    // Procedural Infinite System state parameters
    val currentZoneNumber: Int = 1,
    val zoneDNA: ZoneDNA = ZoneGenerator.generateZone(1, 42),
    val activeTrackElements: List<VisualTrackElement> = emptyList(),
    val activePowerupDurations: Map<String, Int> = emptyMap(), // "PU1".."PU12" -> remaining tick count
    val screenShakeX: Float = 0f,
    val screenShakeY: Float = 0f,
    val bossActive: Boolean = false,
    val bossHealth: Float = 1.0f,
    val bossY: Int = 50,
    val collectedGemsCount: Int = 0,
    val ghostTierMode: Int = 1,
    val isTranscendenceUnlocked: Boolean = false,
    val currentMutationName: String = "",
    val frustrationLevelIndex: Float = 0.5f,
    val obstacleDensityMod: Float = 1.0f, // modified dynamically by AI Adaptation
    val lastZoneTransitionTick: Int = -999
)

class NeonRushViewModel(
    private val gameDao: GameDao,
    context: Context
) : ViewModel() {

    private val soundEngine = NeonSoundEngine()

    // Database Flows
    val profile = gameDao.getProfileFlow()

    // Dynamic Shop Skins
    val shopSkins = listOf(
        Triple("cyan_diamond", "Cyan Diamond", 0), // Starter (Free)
        Triple("purple_square", "Purple Square", 30), // Free or cheap
        Triple("green_triangle", "Green Triangle", 50),
        Triple("magenta_pulse", "Magenta Pulse Racer", 90),
        Triple("gold_transcendence", "Gold Transcendence Vessel", 250),
        Triple("matrix_grid", "Hex Grid Cyber-Fighter", 400)
    )

    // Global Leaderboards state from Firestore
    val leaderboard: StateFlow<List<LeaderboardPilot>> = FirebaseLeaderboardManager.globalRankings

    // Simulation Engine state
    private val _simState = MutableStateFlow(SimulationState())
    val simState: StateFlow<SimulationState> = _simState.asStateFlow()

    // Social Feed state
    private val _socialComments = MutableStateFlow<List<SocialComment>>(emptyList())
    val socialComments: StateFlow<List<SocialComment>> = _socialComments.asStateFlow()

    // Daily social challenge properties
    val dailyChallengeTitle = "Methane Glitch Rush"
    val dailyChallengeDesc = "Maximum wind resistance in Zone 3 with critical fuel cells! Finish above 200 points to score bonus gems."
    val dailyChallengeGoal = 200

    private var simJob: Job? = null

    init {
        loadSocialComments()
        prepopulateSampleGhostChallenges()
        FirebaseLeaderboardManager.fetchTopScores()
    }

    private fun prepopulateSampleGhostChallenges() {
        viewModelScope.launch {
            // Re-populate some robust local ghost runs to challenge
            gameDao.insertGhost(
                GhostChallengeEntity(
                    challengeId = "ghost_retro",
                    playerName = "RetroWave",
                    score = 240,
                    zoneReached = 2,
                    yPositionsCsv = ZoneGenerator.generateTelemetryCsv(240, 42)
                )
            )
            gameDao.insertGhost(
                GhostChallengeEntity(
                    challengeId = "ghost_zeroglitch",
                    playerName = "ZeroGlitch",
                    score = 480,
                    zoneReached = 3,
                    yPositionsCsv = ZoneGenerator.generateTelemetryCsv(480, 84)
                )
            )
            gameDao.insertGhost(
                GhostChallengeEntity(
                    challengeId = "ghost_cyberrunner",
                    playerName = "CyberRunner",
                    score = 850,
                    zoneReached = 4,
                    yPositionsCsv = ZoneGenerator.generateTelemetryCsv(850, 111)
                )
            )
        }
    }

    private fun loadDefaultLeaderboard() {
        FirebaseLeaderboardManager.fetchTopScores()
    }

    private fun loadSocialComments() {
        _socialComments.value = listOf(
            SocialComment("CyberRunner", "Just secured a 850 run using the Matrix Grid fighter! The drafting is key in Warp Voids.", "10m ago", "Singularity Terminus"),
            SocialComment("ZeroGlitch", "That freeze hazard in Zone 3 completely ruined my sliding angles. Watch your thrusters!", "1h ago", "Methane Basin"),
            SocialComment("RetroWave", "Anyone else thinks the Cyan Diamond skin handles smoother on Chromium light rings?", "3h ago", "Chromium Grid"),
            SocialComment("DriftKing_X", "Is the Daily Rush active? Heard it gives 50 free gems today.", "4h ago", "Cyber Alley"),
            SocialComment("NeonVolt", "Unbelievable speed. I made the top 10 today but dropped immediately.", "6h ago", "Chromium Grid")
        )
    }

    // Toggle Following global pilot
    fun toggleFollowUser(pilotName: String) {
        viewModelScope.launch {
            val prof = gameDao.getProfileDirect() ?: GameProfile()
            val followedList = prof.followedUsersCsv.split(",").filter { it.isNotEmpty() }.toMutableList()

            if (followedList.contains(pilotName)) {
                followedList.remove(pilotName)
            } else {
                followedList.add(pilotName)
            }

            val updated = prof.copy(followedUsersCsv = followedList.joinToString(","))
            gameDao.saveProfile(updated)
            loadDefaultLeaderboard() // Refresh followed status
        }
    }

    // Purchase skin
    fun purchaseSkin(skinId: String, cost: Int) {
        viewModelScope.launch {
            val prof = gameDao.getProfileDirect() ?: GameProfile()
            val unlockedSkins = prof.unlockedSkinsCsv.split(",").toMutableList()

            if (unlockedSkins.contains(skinId)) {
                // If already unlocked, just active selection
                val updated = prof.copy(activeSkinId = skinId)
                gameDao.saveProfile(updated)
                soundEngine.playTone(400f, 100, "sine")
                loadDefaultLeaderboard()
                return@launch
            }

            if (prof.gems >= cost) {
                unlockedSkins.add(skinId)
                val updated = prof.copy(
                    gems = prof.gems - cost,
                    unlockedSkinsCsv = unlockedSkins.joinToString(","),
                    activeSkinId = skinId
                )
                gameDao.saveProfile(updated)
                soundEngine.playUnlockSkin()
                loadDefaultLeaderboard()
            }
        }
    }

    // Adaptive AI Player Profile statistics for Layer 10
    var avgZoneReached: Float = 10f
    var frustrationIndex: Float = 0.5f
    var deathTimes: MutableList<Long> = mutableListOf()
    val deathHeatmap: MutableMap<String, Int> = mutableMapOf()
    var hitObstaclesHistory: MutableList<String> = mutableListOf()
    var totalPlayedSeconds: Float = 0f
    var lastPlayTime: Long = System.currentTimeMillis()

    // Selects a procedural powerup type based on probability and Sunday Legendary Day mutation (Level 4)
    private fun chooseProceduralPowerupType(rand: kotlin.random.Random, isSunday: Boolean): String {
        val roll = rand.nextInt(100)
        // Sunday Legendary Day makes ultra rare powerups 5x more likely!
        val legendaryThreshold = if (isSunday) 25 else 5
        val rareThreshold = if (isSunday) 55 else 25
        
        return when {
            roll < legendaryThreshold -> {
                // PU11, PU12 (Ultra Rare)
                if (rand.nextBoolean()) "PU11" else "PU12"
            }
            roll < legendaryThreshold + rareThreshold -> {
                // PU6 to PU10 (Rare)
                "PU" + rand.nextInt(6, 11)
            }
            else -> {
                // PU1 to PU5 (Common)
                "PU" + rand.nextInt(1, 6)
            }
        }
    }

    // Procedural Obstacle spawning generator based on active Obstacle Set ID (Level 1 / 2)
    private fun spawnObstacleForSet(obstacleSetId: Int, tick: Int, rand: kotlin.random.Random, ghostY: Int): List<VisualTrackElement> {
        val elements = mutableListOf<VisualTrackElement>()
        val baseId = "obs_${tick}_"
        
        when (obstacleSetId) {
            1 -> { // PILLARS: tall rect blocks with central safe gap
                val gapSize = rand.nextInt(20, 30)
                val gapCenter = (ghostY + rand.nextInt(-10, 10)).coerceIn(35, 65)
                elements.add(VisualTrackElement("${baseId}p1", 1.2f, gapCenter + gapSize/2, "obstacle", "PILLAR_BOTTOM"))
                elements.add(VisualTrackElement("${baseId}p2", 1.2f, gapCenter - gapSize/2, "obstacle", "PILLAR_TOP"))
            }
            2 -> { // LASERS: horizontal blinking hazard lines
                elements.add(VisualTrackElement("${baseId}l1", 1.2f, rand.nextInt(20, 80), "obstacle", "LASER"))
                if (rand.nextBoolean()) {
                    elements.add(VisualTrackElement("${baseId}l2", 1.25f, rand.nextInt(20, 80), "obstacle", "LASER"))
                }
            }
            3 -> { // BLADES: rotating hazard nodes
                elements.add(VisualTrackElement("${baseId}b1", 1.3f, ghostY, "obstacle", "BLADE"))
            }
            4 -> { // STALACTITES: ceiling spikes, force low flight
                elements.add(VisualTrackElement("${baseId}st1", 1.2f, rand.nextInt(15, 45), "obstacle", "STALACTITE"))
            }
            5 -> { // STALAGMITES: ground spikes, force high flight
                elements.add(VisualTrackElement("${baseId}sm1", 1.2f, rand.nextInt(55, 85), "obstacle", "STALAGMITE"))
            }
            6 -> { // SANDWICH: ceiling and ground blocks simultaneously
                elements.add(VisualTrackElement("${baseId}sa1", 1.2f, 20, "obstacle", "STALACTITE"))
                elements.add(VisualTrackElement("${baseId}sa2", 1.2f, 80, "obstacle", "STALAGMITE"))
            }
            7, 11 -> { // WAVES / MIRROR: symmetrical patterns
                elements.add(VisualTrackElement("${baseId}m1", 1.2f, ghostY - 12, "obstacle", "BARRIER"))
                elements.add(VisualTrackElement("${baseId}m2", 1.2f, ghostY + 12, "obstacle", "BARRIER"))
            }
            15 -> { // ZAP BARRIERS
                elements.add(VisualTrackElement("${baseId}z1", 1.2f, rand.nextInt(10, 90), "obstacle", "ZAP_FIELD"))
            }
            17 -> { // PHANTOM: blocks fade invisible
                elements.add(VisualTrackElement("${baseId}f1", 1.2f, ghostY + rand.nextInt(-10, 10).coerceIn(15, 85), "obstacle", "PHANTOM"))
            }
            18 -> { // SPLITTER: blocks split
                elements.add(VisualTrackElement("${baseId}s1", 1.2f, ghostY - 6, "obstacle", "SPLITTER"))
                elements.add(VisualTrackElement("${baseId}s2", 1.2f, ghostY + 6, "obstacle", "SPLITTER"))
            }
            22 -> { // TUNNEL CORRIDOR
                elements.add(VisualTrackElement("${baseId}tu1", 1.2f, ghostY - 20, "obstacle", "TUNNEL_TOP"))
                elements.add(VisualTrackElement("${baseId}tu2", 1.2f, ghostY + 20, "obstacle", "TUNNEL_BOTTOM"))
            }
            else -> { // Standard geometric hazard points
                elements.add(VisualTrackElement("${baseId}st", 1.2f, ghostY + rand.nextInt(-12, 12).coerceIn(15, 85), "obstacle", "STANDARD"))
            }
        }
        return elements
    }

    // Transcendence Prestige Reset System (Layer 7)
    fun triggerTranscendence() {
        viewModelScope.launch {
            val prof = gameDao.getProfileDirect() ?: GameProfile()
            // Available when player has reached Zone 50 (or score/best score >= 5000)
            if (prof.bestScore >= 5000 || prof.transcendenceCount > 0) {
                val nextPrestige = prof.transcendenceCount + 1
                
                // Unlock tier-inclusive premium skins
                val currentUnlocked = prof.unlockedSkinsCsv.split(",").filter { it.isNotEmpty() }.toMutableList()
                val exclusiveSkin = when (nextPrestige) {
                    1 -> "gold_transcendence"
                    2 -> "matrix_grid"
                    else -> "elite_nebula"
                }
                if (!currentUnlocked.contains(exclusiveSkin)) {
                    currentUnlocked.add(exclusiveSkin)
                }
                
                // Reset score to 0 and award 500 prestige gem cells
                val updated = prof.copy(
                    transcendenceCount = nextPrestige,
                    bestScore = 0,
                    gems = prof.gems + 500,
                    unlockedSkinsCsv = currentUnlocked.joinToString(","),
                    activeSkinId = exclusiveSkin
                )
                gameDao.saveProfile(updated)
                soundEngine.playTone(880f, 500, "sawtooth")
                loadDefaultLeaderboard()
            }
        }
    }

    // Daily Attempt Submission
    fun runDailyRushChallenge(onComplete: (Boolean, Int) -> Unit) {
        viewModelScope.launch {
            val prof = gameDao.getProfileDirect() ?: GameProfile()
            val todayDate = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())

            val attempts = if (prof.lastDailyRushDate == todayDate) prof.dailyAttemptsToday else 0
            if (attempts >= 3) {
                // Out of attempts today!
                onComplete(false, 0)
                return@launch
            }

            // Lock out/increment attempts
            val updated = prof.copy(
                dailyAttemptsToday = attempts + 1,
                lastDailyRushDate = todayDate
            )
            gameDao.saveProfile(updated)

            // Start daily race vs virtual hard ghost!
            val hardGhost = GhostChallengeEntity(
                challengeId = "daily_hard_ghost",
                playerName = "GlitchViper [VIRTUAL]",
                score = 350,
                zoneReached = 3,
                yPositionsCsv = ZoneGenerator.generateTelemetryCsv(350, 99)
            )
            startRacingSimulation(hardGhost)
            onComplete(true, attempts + 1)
        }
    }

    // Core Procedural Infinite Racing Simulator Simulator Engine (Levels 1 - 10)
    fun startRacingSimulation(ghost: GhostChallengeEntity) {
        simJob?.cancel()
        
        val startupDna = ZoneGenerator.generateZone(1, 42)
        val todayMutation = DailyMutations.getActiveMutation()
        val isFirstLucky = (System.currentTimeMillis() - lastPlayTime) > 3 * 24 * 3600 * 1000L // Lucky run if idle 3 days
        
        _simState.value = SimulationState(
            activeGhost = ghost,
            isStarted = true,
            isCompleted = false,
            tickIndex = 0,
            ghostYPath = ZoneGenerator.parseTelemetry(ghost.yPositionsCsv),
            userYPath = parseTelemetryFromSimParameters(),
            feedbackMessage = if (isFirstLucky) "LUCKY DRIFT ENGAGED (SLOWER SPEED, MORE POWERUPS)!" else "Synchronizing procedural light grid pathways...",
            currentZoneNumber = 1,
            zoneDNA = startupDna,
            activeTrackElements = emptyList(),
            activePowerupDurations = emptyMap(),
            currentMutationName = todayMutation.title
        )

        soundEngine.setHomeScreenActiveState(false)
        soundEngine.playThrusterCharge()

        simJob = viewModelScope.launch {
            val milestonesTriggered = mutableSetOf<Int>()
            val prof = gameDao.getProfileDirect() ?: GameProfile()
            var tick = 0
            val random = kotlin.random.Random(System.currentTimeMillis())
            var runStartTime = System.currentTimeMillis()
            
            // AI Performance Calibration: Adapt difficulty based on profile frustration level (Level 10)
            val currentFrustration = frustrationIndex
            val spacingBias = if (currentFrustration > 3.0f) 0.85f else if (avgZoneReached > 15f) 1.10f else 1.0f
            val isLuckyActive = isFirstLucky
            var liveDifficultyMultiplier = 1.0f
            var ticksSinceLastHit = 0
            
            while (_simState.value.isStarted && !_simState.value.isCompleted) {
                delay(120) // Game step tick delay
                val state = _simState.value
                val userY = state.userYPos
                
                // Get active Daily Mutation
                val activeMutation = DailyMutations.getActiveMutation()
                val isMondayGems = activeMutation == MutationDay.MONDAY
                val isTuesdaySpeed = activeMutation == MutationDay.TUESDAY
                val isWednesdayPower = activeMutation == MutationDay.WEDNESDAY
                val isThursdayMirror = activeMutation == MutationDay.THURSDAY
                val isFridayGolden = activeMutation == MutationDay.FRIDAY
                val isSaturdayBoss = activeMutation == MutationDay.SATURDAY
                val isSundayLegendary = activeMutation == MutationDay.SUNDAY

                // 1. Calculate procedurally advancing distance & speed (Level 2)
                val baseSpeedVal = ZoneGenerator.calculateSpeed(state.currentZoneNumber)
                
                // Prestige Speed boost: +0.2 px/frame per prestige rank (Level 7)
                val prestigeSpeedBoost = prof.transcendenceCount * 0.2f
                var speedInPx = baseSpeedVal + prestigeSpeedBoost
                
                // Apply speed surge mutation (Tuesday)
                if (isTuesdaySpeed) {
                    speedInPx *= 1.20f
                }
                
                // Apply Slow Time (PU3 / ZM6) active modifiers
                val hasSlowTime = state.activePowerupDurations.containsKey("PU3") || state.zoneDNA.mechanicIds.contains(6)
                if (hasSlowTime || isLuckyActive) {
                    speedInPx *= 0.50f
                }
                
                // Apply Hyperdrive (ZM1) active modifiers
                val hasHyperdrive = state.zoneDNA.mechanicIds.contains(1)
                if (hasHyperdrive) {
                    speedInPx *= 2.0f
                }
                // Live adaptive difficulty: speeds up if doing well, eases off if struggling
                speedInPx *= liveDifficultyMultiplier
                
                // Advance distance by speed multiplier (1 px/frame = ~3.6 meters/tick)
                val tickDistanceOffset = speedInPx * 3.6f
                val nextDistance = state.distanceMeters + tickDistanceOffset
                
                // Identify active Zone Number (Each zone is 500 meters)
                val nextZoneNumber = (nextDistance / 500f).toInt() + 1
                val activeDna = ZoneGenerator.generateZone(nextZoneNumber, random.nextLong())
                
                // 2. Play beautiful smooth environment crossfades & entering flasher (Level 3 / 5)
                var updatedMsg = state.feedbackMessage
                if (nextZoneNumber != state.currentZoneNumber) {
                    soundEngine.playTone(660f, 300, "sawtooth")
                    updatedMsg = "ENTERING: ${activeDna.environmentName} ${activeDna.environmentEmoji}"
                } else if (state.feedbackMessage.startsWith("ENTERING") && tick - state.lastZoneTransitionTick > 25) {
                    updatedMsg = "SYNCHRONIZED WITH ${activeDna.environmentName}"
                }

                // 3. Spatially move, slide, and magnet pull active track elements
                val updatedElements = mutableListOf<VisualTrackElement>()
                val pullActive = state.activePowerupDurations.containsKey("PU2") || activeDna.mechanicIds.contains(4) // MAGNET POWERUP / ZONE MECHANIC
                
                for (elem in state.activeTrackElements) {
                    // Slide element leftward
                    val nextX = elem.xOffsetFraction - (speedInPx * 0.018f)
                    if (nextX < -0.1f) continue // De-spawn past left border
                    
                    var actualY = elem.yMatchPos
                    // Magnet attraction (Level 4): pull toward user Y position if nearby
                    if (pullActive && elem.type != "obstacle" && elem.type != "bullet" && nextX > 0.15f && nextX < 0.65f) {
                        val diffY = userY - actualY
                        actualY += (diffY * 0.25f).toInt()
                    }
                    
                    updatedElements.add(elem.copy(xOffsetFraction = nextX, yMatchPos = actualY))
                }

                // 4. Procedural Obstacles, power-ups, gems, and fuel-cell spawner triggers (Level 1 / 4)
                if (tick % (10 / spacingBias).coerceIn(4f, 25f).toInt() == 0 && !activeDna.mechanicIds.contains(19)) { // ZM19 removes pickups
                    val gridX = 1.2f
                    val routeTargetY = state.ghostYPath.getOrNull(tick % state.ghostYPath.size.coerceAtLeast(1)) ?: 50
                    
                    // Spawn Gems and Fuel cells along target route
                    if (random.nextInt(100) < 40) {
                        updatedElements.add(VisualTrackElement("gem_${tick}", gridX, routeTargetY + random.nextInt(-8, 8), "gem"))
                    } else if (random.nextInt(100) < 15) {
                        updatedElements.add(VisualTrackElement("fuel_${tick}", gridX, routeTargetY + random.nextInt(-5, 5), "fuel"))
                    }
                }

                // Spawn Powerup or Obstacle Set Hazards dynamically based on active spacings
                val spacingVal = (activeDna.obstacleSpacingAndDensity / (12f * spacingBias * liveDifficultyMultiplier)).coerceAtLeast(4f).toInt()
                if (tick % spacingVal == 0) {
                    val targetGhostY = state.ghostYPath.getOrNull(tick % state.ghostYPath.size.coerceAtLeast(1)) ?: 50
                    
                    // Spawn themed obstacle sets
                    val obstacles = spawnObstacleForSet(activeDna.obstacleSetId, tick, random, targetGhostY)
                    updatedElements.addAll(obstacles)

                    // Spawn powerups
                    if (random.nextFloat() < activeDna.powerupDensity) {
                        val puType = chooseProceduralPowerupType(random, isSundayLegendary)
                        updatedElements.add(VisualTrackElement("pu_${tick}", 1.22f, targetGhostY + random.nextInt(-10, 10), "powerup", puType))
                    }
                }

                // 5. Boss Gauntlet System Actions (Level 5)
                val hasBossZone = (nextZoneNumber % 5 == 0 && (tick % 40) >= 24) || isSaturdayBoss
                var bossHealthState = state.bossHealth
                var bossYState = state.bossY
                
                if (hasBossZone) {
                    if (!state.bossActive) {
                        soundEngine.playTone(220f, 600, "sawtooth")
                        updatedMsg = "CRITICAL WARNING: ZONE BOSS INCOMING!"
                        bossHealthState = 1.0f
                    }
                    
                    // AI tracks vertically to block corridors
                    val trackingYBias = userY - bossYState
                    bossYState += (trackingYBias * 0.12f).toInt()
                    
                    // Boss fires bullets (Bullet Hell ZM17)
                    if (tick % 6 == 0) {
                        updatedElements.add(VisualTrackElement("bullet_${tick}", 1.15f, bossYState + random.nextInt(-5, 5), "bullet"))
                    }
                    
                    // Auto reduce boss health by flying past
                    bossHealthState -= 0.04f
                    if (bossHealthState <= 0f) {
                        soundEngine.playTone(990f, 400, "sine")
                        updatedMsg = "BOSS DEFEATED! Prestige Reward Cells gathered!"
                        // Reward prestige gems
                        val bossReward = nextZoneNumber * 12
                        val currentGems = prof.gems + bossReward
                        gameDao.saveProfile(prof.copy(gems = currentGems))
                    }
                }

                // 6. Handle collisions and collection processing
                var fuelLevelState = state.fuelLevelPercent
                var gemsGathered = state.collectedGemsCount
                val nextDurationsMap = state.activePowerupDurations.toMutableMap()
                val finalElements = mutableListOf<VisualTrackElement>()
                
                // Filter durations
                for (key in nextDurationsMap.keys.toList()) {
                    val remaining = nextDurationsMap[key] ?: 0
                    if (remaining <= 1) {
                        nextDurationsMap.remove(key)
                    } else {
                        nextDurationsMap[key] = remaining - 1
                    }
                }

                val hasInvincibility = nextDurationsMap.containsKey("PU7") || nextDurationsMap.containsKey("PU12") // Invincibility / Legendary Aura
                val hasGhostMode = nextDurationsMap.containsKey("PU4") || activeDna.mechanicIds.contains(3)
                var hasShield = nextDurationsMap.containsKey("PU1")
                var hitThisTick = false

                for (elem in updatedElements) {
                    // Check horizontal alignment bounds
                    val isAligned = elem.xOffsetFraction >= 0.16f && elem.xOffsetFraction <= 0.26f
                    if (isAligned) {
                        val verticalDist = Math.abs(userY - elem.yMatchPos)
                        
                        // Shrink powerup (PU9) reduces vertical hit profile by 50%
                        val collisionRadius = if (state.activePowerupDurations.containsKey("PU9")) 8 else 15
                        
                        if (verticalDist < collisionRadius) {
                            // Element intersection!
                            when (elem.type) {
                                "gem" -> {
                                    soundEngine.playGemCollect()
                                    val multiFactor = if (isMondayGems) 2 else 1
                                    gemsGathered += multiFactor
                                }
                                "fuel" -> {
                                    soundEngine.playTone(523f, 80, "sine")
                                    fuelLevelState = (fuelLevelState + 18).coerceAtMost(100)
                                }
                                "powerup" -> {
                                    soundEngine.playShieldPowerup()
                                    val puId = elem.subType
                                    
                                    // Trigger instant modifiers
                                    if (puId == "PU10") { // LANE WARP: teleport to safe trajectory line
                                        val perfectY = state.ghostYPath.getOrNull(tick % state.ghostYPath.size.coerceAtLeast(1)) ?: 50
                                        _simState.value = _simState.value.copy(userYPos = perfectY)
                                        updatedMsg = "LANE WARP COMPLETE! DRIFT SAFE LINE SECURED!"
                                    } else if (puId == "PU11") { // ZONE SKIP: advance current zone
                                        val mToSkip = 500f - (nextDistance % 500f) + 10f
                                        _simState.value = state.copy(distanceMeters = nextDistance + mToSkip, currentZoneNumber = nextZoneNumber + 1)
                                        updatedMsg = "QUANTUM ZONE LEAP ENGAGED!"
                                    } else if (puId == "PU12") { // Legendary Aura: activate all buffs
                                        for (i in 1..9) {
                                            nextDurationsMap["PU$i"] = 40 // ~5 seconds
                                        }
                                        updatedMsg = "LEGENDARY PRESTIGE MATRIX SYNC ACTIVE!"
                                    } else {
                                        nextDurationsMap[puId] = 80 // ~10 seconds
                                    }
                                }
                                "obstacle", "bullet" -> {
                                    if (hasInvincibility || hasGhostMode) {
                                        // phase safely
                                    } else if (hasShield) {
                                        // Shatter shield safely
                                        soundEngine.playShieldBreak()
                                        nextDurationsMap.remove("PU1")
                                        hasShield = false
                                        updatedMsg = "DRIFT DEFLECTED: SHIELD BARRIER OVERLOADED"
                                    } else {
                                        // Standard hit, drain fuel cells
                                        soundEngine.playCollision()
                                        fuelLevelState = (fuelLevelState - 20).coerceAtLeast(0)
                                        updatedMsg = "WARNING: IMPACT DETECTED! HULL INTEGRITY LOST"
                                        hitObstaclesHistory.add(elem.subType)
                                        hitThisTick = true
                                    }
                                }
                            }
                            continue // Filter matching items (collected/hit)
                        }
                    }
                    finalElements.add(elem)
                }
                if (hitThisTick) {
                    ticksSinceLastHit = 0
                    liveDifficultyMultiplier = (liveDifficultyMultiplier - 0.15f).coerceAtLeast(0.6f)
                } else {
                    ticksSinceLastHit++
                    if (ticksSinceLastHit > 40) {
                        liveDifficultyMultiplier = (liveDifficultyMultiplier + 0.01f).coerceAtMost(1.4f)
                    }
                }

                // 7. Calculate score matching points (Level 2)
                val targetGhostY = state.ghostYPath.getOrNull(tick % state.ghostYPath.size.coerceAtLeast(1)) ?: 50
                // Gravity Flip (ZM2) reverse inputs / mirror coordinates
                val processedGhostY = if (activeDna.mechanicIds.contains(2)) 100 - targetGhostY else targetGhostY
                val rawError = Math.abs(userY - processedGhostY)
                
                var tickScore = when {
                    rawError < 8 -> if (isWednesdayPower) 30 else 18
                    rawError < 18 -> if (isWednesdayPower) 15 else 10
                    else -> 2
                }

                // Friday Golden Hour multiplier (3x) and Sunday Legendary Day multiplier
                var multi = 1.0f
                if (isFridayGolden) multi *= 3.0f
                
                // Prestige prestige multiplier: +5% per transcendence Level (Level 7)
                multi += (prof.transcendenceCount * 0.05f)
                
                // Active powerup score boosts (PU5 - 2x, PU6 - 5x)
                if (nextDurationsMap.containsKey("PU6")) {
                    multi *= 5.0f
                } else if (nextDurationsMap.containsKey("PU5")) {
                    multi *= 2.0f
                }

                val finalTickPoints = (tickScore * multi).toInt()
                val nextScore = state.score + finalTickPoints

                // 8. Dynamic Audio Engine Synthesizer updates (Level 3)
                soundEngine.updateGameTelemetries(speedInPx * 40f, nextScore)
                
                // Track score milestone sound trigger alarms
                for (ms in listOf(150, 500, 1500, 4000, 10000)) {
                    if (nextScore >= ms && !milestonesTriggered.contains(ms)) {
                        milestonesTriggered.add(ms)
                        soundEngine.playSpeedMilestone(ms)
                    }
                }

                // Natural continuous battery drain
                fuelLevelState = (fuelLevelState - 1)
                if (fuelLevelState <= 0) {
                    _simState.value = state.copy(isCompleted = true, score = nextScore)
                    break
                }

                // Screen shaking logic (ZM9 Storm active)
                var shakeX = 0f
                var shakeY = 0f
                if (activeDna.mechanicIds.contains(9) || (hasBossZone && bossHealthState > 0)) {
                    shakeX = (random.nextFloat() * 8f - 4f)
                    shakeY = (random.nextFloat() * 8f - 4f)
                }

                // Increment index
                tick++
                
                // Update active simulation states
                _simState.value = state.copy(
                    tickIndex = tick,
                    ghostYPos = processedGhostY,
                    score = nextScore,
                    currentZoneName = activeDna.name,
                    speedKmh = (speedInPx * 40).toInt(),
                    distanceMeters = nextDistance,
                    fuelLevelPercent = fuelLevelState,
                    feedbackMessage = updatedMsg,
                    currentZoneNumber = nextZoneNumber,
                    zoneDNA = activeDna,
                    activeTrackElements = finalElements,
                    activePowerupDurations = nextDurationsMap,
                    screenShakeX = shakeX,
                    screenShakeY = shakeY,
                    bossActive = hasBossZone && bossHealthState > 0f,
                    bossHealth = bossHealthState.coerceIn(0f, 1f),
                    bossY = bossYState,
                    collectedGemsCount = gemsGathered,
                    isTranscendenceUnlocked = nextZoneNumber >= 50 || prof.transcendenceCount > 0
                )
            }

            // Play collision / complete summary runs and record diagnostic metrics (Level 10)
            avgZoneReached = (avgZoneReached * 0.8f) + (_simState.value.currentZoneNumber * 0.2f)
            deathTimes.add(System.currentTimeMillis())
            totalPlayedSeconds += (tick * 0.12f)
            val durationMin = totalPlayedSeconds / 60f
            frustrationIndex = (deathTimes.size / durationMin.coerceAtLeast(1f))
            lastPlayTime = System.currentTimeMillis()

            completeSimulationRun()
        }
    }

    private suspend fun completeSimulationRun() {
        val finalState = _simState.value
        _simState.value = finalState.copy(isCompleted = true, feedbackMessage = "Sync terminal drift halt. Run finalized!")

        val prof = gameDao.getProfileDirect() ?: GameProfile()
        
        // Save user's best score if they surpassed original limit
        val isNewPB = finalState.score > prof.bestScore
        
        // Award gems based on run gathered count
        val todayMutation = DailyMutations.getActiveMutation()
        val isMonday = todayMutation == MutationDay.MONDAY
        val isFriday = todayMutation == MutationDay.FRIDAY

        val valMultiplier = if (isMonday) 2 else 1 // Monday Double Gems
        val FridayBonus = if (isFriday) 25 else 0 // Friday Golden Hour 

        var bonusGems = 0
        if (finalState.activeGhost?.challengeId == "daily_hard_ghost" && finalState.score >= dailyChallengeGoal) {
            bonusGems = 55
            soundEngine.playUnlockSkin()
        }

        val totalGemsEarned = ((finalState.collectedGemsCount + bonusGems + FridayBonus) * valMultiplier)
        
        val updated = prof.copy(
            bestScore = if (isNewPB) finalState.score else prof.bestScore,
            gems = prof.gems + totalGemsEarned
        )
        gameDao.saveProfile(updated)
        
        if (isNewPB) {
            soundEngine.playPersonalBestBroken()
            // --- SUBMIT SECURE REAL SCORE TO FIREBASE FIRESTORE ---
            FirebaseLeaderboardManager.submitScore(prof.username, finalState.score, prof.activeSkinId)
        } else {
            soundEngine.playCollision()
        }
    }

    fun reviveSimulation() {
        val currentState = _simState.value
        _simState.value = currentState.copy(
            isCompleted = false,
            fuelLevelPercent = 100,
            feedbackMessage = "Revive code accepted! Launching drone boosters..."
        )
        val currentTick = currentState.tickIndex
        
        simJob?.cancel()
        soundEngine.playRevive()
        
        simJob = viewModelScope.launch {
            val milestonesTriggered = mutableSetOf<Int>()
            // pre-populate existing score milestones
            for (ms in listOf(150, 500, 1500, 4000, 10000)) {
                if (currentState.score >= ms) milestonesTriggered.add(ms)
            }

            val random = kotlin.random.Random(System.currentTimeMillis())
            val prof = gameDao.getProfileDirect() ?: GameProfile()
            var tick = currentTick
            
            while (_simState.value.isStarted && !_simState.value.isCompleted) {
                delay(120)
                val state = _simState.value
                val userY = state.userYPos
                
                val activeMutation = DailyMutations.getActiveMutation()
                val isMondayGems = activeMutation == MutationDay.MONDAY
                val isTuesdaySpeed = activeMutation == MutationDay.TUESDAY
                val isWednesdayPower = activeMutation == MutationDay.WEDNESDAY
                val isThursdayMirror = activeMutation == MutationDay.THURSDAY
                val isFridayGolden = activeMutation == MutationDay.FRIDAY
                val isSaturdayBoss = activeMutation == MutationDay.SATURDAY
                val isSundayLegendary = activeMutation == MutationDay.SUNDAY

                val baseSpeedVal = ZoneGenerator.calculateSpeed(state.currentZoneNumber)
                val prestigeSpeedBoost = prof.transcendenceCount * 0.2f
                var speedInPx = baseSpeedVal + prestigeSpeedBoost
                
                if (isTuesdaySpeed) {
                    speedInPx *= 1.20f
                }
                
                val hasSlowTime = state.activePowerupDurations.containsKey("PU3") || state.zoneDNA.mechanicIds.contains(6)
                if (hasSlowTime) {
                    speedInPx *= 0.50f
                }
                
                val hasHyperdrive = state.zoneDNA.mechanicIds.contains(1)
                if (hasHyperdrive) {
                    speedInPx *= 2.0f
                }
                
                val tickDistanceOffset = speedInPx * 3.6f
                val nextDistance = state.distanceMeters + tickDistanceOffset
                
                val nextZoneNumber = (nextDistance / 500f).toInt() + 1
                val activeDna = ZoneGenerator.generateZone(nextZoneNumber, random.nextLong())
                
                var updatedMsg = state.feedbackMessage
                if (nextZoneNumber != state.currentZoneNumber) {
                    soundEngine.playTone(660f, 300, "sawtooth")
                    updatedMsg = "ENTERING: ${activeDna.environmentName} ${activeDna.environmentEmoji}"
                } else if (state.feedbackMessage.startsWith("ENTERING") && tick - state.lastZoneTransitionTick > 25) {
                    updatedMsg = "SYNCHRONIZED WITH ${activeDna.environmentName}"
                }

                val updatedElements = mutableListOf<VisualTrackElement>()
                val pullActive = state.activePowerupDurations.containsKey("PU2") || activeDna.mechanicIds.contains(4)
                
                for (elem in state.activeTrackElements) {
                    val nextX = elem.xOffsetFraction - (speedInPx * 0.018f)
                    if (nextX < -0.1f) continue
                    
                    var actualY = elem.yMatchPos
                    if (pullActive && elem.type != "obstacle" && elem.type != "bullet" && nextX > 0.15f && nextX < 0.65f) {
                        val diffY = userY - actualY
                        actualY += (diffY * 0.25f).toInt()
                    }
                    updatedElements.add(elem.copy(xOffsetFraction = nextX, yMatchPos = actualY))
                }

                if (tick % 10 == 0 && !activeDna.mechanicIds.contains(19)) {
                    val gridX = 1.2f
                    val routeTargetY = state.ghostYPath.getOrNull(tick % state.ghostYPath.size.coerceAtLeast(1)) ?: 50
                    if (random.nextInt(100) < 40) {
                        updatedElements.add(VisualTrackElement("gem_${tick}", gridX, routeTargetY + random.nextInt(-8, 8), "gem"))
                    } else if (random.nextInt(100) < 15) {
                        updatedElements.add(VisualTrackElement("fuel_${tick}", gridX, routeTargetY + random.nextInt(-5, 5), "fuel"))
                    }
                }

                val spacingVal = (activeDna.obstacleSpacingAndDensity / 12f).coerceAtLeast(4f).toInt()
                if (tick % spacingVal == 0) {
                    val targetGhostY = state.ghostYPath.getOrNull(tick % state.ghostYPath.size.coerceAtLeast(1)) ?: 50
                    val obstacles = spawnObstacleForSet(activeDna.obstacleSetId, tick, random, targetGhostY)
                    updatedElements.addAll(obstacles)

                    if (random.nextFloat() < activeDna.powerupDensity) {
                        val puType = chooseProceduralPowerupType(random, isSundayLegendary)
                        updatedElements.add(VisualTrackElement("pu_${tick}", 1.22f, targetGhostY + random.nextInt(-10, 10), "powerup", puType))
                    }
                }

                val hasBossZone = (nextZoneNumber % 5 == 0 && (tick % 40) >= 24) || isSaturdayBoss
                var bossHealthState = state.bossHealth
                var bossYState = state.bossY
                
                if (hasBossZone) {
                    if (!state.bossActive) {
                        soundEngine.playTone(220f, 600, "sawtooth")
                        updatedMsg = "CRITICAL WARNING: ZONE BOSS INCOMING!"
                        bossHealthState = 1.0f
                    }
                    val trackingYBias = userY - bossYState
                    bossYState += (trackingYBias * 0.12f).toInt()
                    
                    if (tick % 6 == 0) {
                        updatedElements.add(VisualTrackElement("bullet_${tick}", 1.15f, bossYState + random.nextInt(-5, 5), "bullet"))
                    }
                    
                    bossHealthState -= 0.04f
                    if (bossHealthState <= 0f) {
                        soundEngine.playTone(990f, 400, "sine")
                        updatedMsg = "BOSS DEFEATED!"
                    }
                }

                var fuelLevelState = state.fuelLevelPercent
                var gemsGathered = state.collectedGemsCount
                val nextDurationsMap = state.activePowerupDurations.toMutableMap()
                val finalElements = mutableListOf<VisualTrackElement>()
                
                for (key in nextDurationsMap.keys.toList()) {
                    val remaining = nextDurationsMap[key] ?: 0
                    if (remaining <= 1) {
                        nextDurationsMap.remove(key)
                    } else {
                        nextDurationsMap[key] = remaining - 1
                    }
                }

                val hasInvincibility = nextDurationsMap.containsKey("PU7") || nextDurationsMap.containsKey("PU12")
                val hasGhostMode = nextDurationsMap.containsKey("PU4") || activeDna.mechanicIds.contains(3)
                var hasShield = nextDurationsMap.containsKey("PU1")

                for (elem in updatedElements) {
                    val isAligned = elem.xOffsetFraction >= 0.16f && elem.xOffsetFraction <= 0.26f
                    if (isAligned) {
                        val verticalDist = Math.abs(userY - elem.yMatchPos)
                        val collisionRadius = if (state.activePowerupDurations.containsKey("PU9")) 8 else 15
                        
                        if (verticalDist < collisionRadius) {
                            when (elem.type) {
                                "gem" -> {
                                    soundEngine.playGemCollect()
                                    val multiFactor = if (isMondayGems) 2 else 1
                                    gemsGathered += multiFactor
                                }
                                "fuel" -> {
                                    soundEngine.playTone(523f, 80, "sine")
                                    fuelLevelState = (fuelLevelState + 18).coerceAtMost(100)
                                }
                                "powerup" -> {
                                    soundEngine.playShieldPowerup()
                                    val puId = elem.subType
                                    if (puId == "PU10") {
                                        val perfectY = state.ghostYPath.getOrNull(tick % state.ghostYPath.size.coerceAtLeast(1)) ?: 50
                                        _simState.value = _simState.value.copy(userYPos = perfectY)
                                    } else if (puId == "PU11") {
                                        val mToSkip = 500f - (nextDistance % 500f) + 10f
                                        _simState.value = state.copy(distanceMeters = nextDistance + mToSkip, currentZoneNumber = nextZoneNumber + 1)
                                    } else if (puId == "PU12") {
                                        for (i in 1..9) {
                                            nextDurationsMap["PU$i"] = 40
                                        }
                                    } else {
                                        nextDurationsMap[puId] = 80
                                    }
                                }
                                "obstacle", "bullet" -> {
                                    if (hasInvincibility || hasGhostMode) {
                                        // phase
                                    } else if (hasShield) {
                                        soundEngine.playShieldBreak()
                                        nextDurationsMap.remove("PU1")
                                        hasShield = false
                                        updatedMsg = "SHIELD BROKEN!"
                                    } else {
                                        soundEngine.playCollision()
                                        fuelLevelState = (fuelLevelState - 20).coerceAtLeast(0)
                                        updatedMsg = "IMPACT IMPACT!"
                                    }
                                }
                            }
                            continue
                        }
                    }
                    finalElements.add(elem)
                }

                val targetGhostY = state.ghostYPath.getOrNull(tick % state.ghostYPath.size.coerceAtLeast(1)) ?: 50
                val processedGhostY = if (activeDna.mechanicIds.contains(2)) 100 - targetGhostY else targetGhostY
                val rawError = Math.abs(userY - processedGhostY)
                
                var tickScore = when {
                    rawError < 8 -> if (isWednesdayPower) 30 else 18
                    rawError < 18 -> if (isWednesdayPower) 15 else 10
                    else -> 2
                }

                var multi = 1.0f
                if (isFridayGolden) multi *= 3.0f
                multi += (prof.transcendenceCount * 0.05f)
                
                if (nextDurationsMap.containsKey("PU6")) {
                    multi *= 5.0f
                } else if (nextDurationsMap.containsKey("PU5")) {
                    multi *= 2.0f
                }

                val finalTickPoints = (tickScore * multi).toInt()
                val nextScore = state.score + finalTickPoints

                soundEngine.updateGameTelemetries(speedInPx * 40f, nextScore)
                
                for (ms in listOf(150, 500, 1500, 4000, 10000)) {
                    if (nextScore >= ms && !milestonesTriggered.contains(ms)) {
                        milestonesTriggered.add(ms)
                        soundEngine.playSpeedMilestone(ms)
                    }
                }

                fuelLevelState = (fuelLevelState - 1)
                if (fuelLevelState <= 0) {
                    _simState.value = state.copy(isCompleted = true, score = nextScore)
                    break
                }

                var shakeX = 0f
                var shakeY = 0f
                if (activeDna.mechanicIds.contains(9) || (hasBossZone && bossHealthState > 0)) {
                    shakeX = (random.nextFloat() * 8f - 4f)
                    shakeY = (random.nextFloat() * 8f - 4f)
                }

                tick++
                _simState.value = state.copy(
                    tickIndex = tick,
                    ghostYPos = processedGhostY,
                    score = nextScore,
                    currentZoneName = activeDna.name,
                    speedKmh = (speedInPx * 40).toInt(),
                    distanceMeters = nextDistance,
                    fuelLevelPercent = fuelLevelState,
                    feedbackMessage = "REVIVED SYNCHRONIZING...",
                    currentZoneNumber = nextZoneNumber,
                    zoneDNA = activeDna,
                    activeTrackElements = finalElements,
                    activePowerupDurations = nextDurationsMap,
                    screenShakeX = shakeX,
                    screenShakeY = shakeY,
                    bossActive = hasBossZone && bossHealthState > 0f,
                    bossHealth = bossHealthState.coerceIn(0f, 1f),
                    bossY = bossYState,
                    collectedGemsCount = gemsGathered,
                    isTranscendenceUnlocked = nextZoneNumber >= 50 || prof.transcendenceCount > 0
                )
            }
            completeSimulationRun()
        }
    }

    fun resetSimulation() {
        simJob?.cancel()
        soundEngine.setHomeScreenActiveState(true)
        _simState.value = SimulationState()
    }

    // Help feed live joystick inputs during race path tracking
    fun adjustUserY(delta: Int) {
        val activeMechs = _simState.value.zoneDNA.mechanicIds
        // Gravity Flip (ZM2) inverts the player controls! (Level 2)
        val relativeDelta = if (activeMechs.contains(2)) -delta else delta
        val nextY = (_simState.value.userYPos + relativeDelta).coerceIn(10, 90)
        _simState.value = _simState.value.copy(userYPos = nextY)
    }

    // High performance selector for Ghost Telemetries (Level 8 / 9)
    fun setGhostModeTier(mode: Int) {
        viewModelScope.launch {
            val prof = gameDao.getProfileDirect() ?: GameProfile()
            var simulatedScore = when (mode) {
                1 -> prof.bestScore.coerceAtLeast(100) // Personal best
                2 -> 400 // Zone spec run
                3 -> 1200 // Global top challenger
                4 -> 2200 // Composite optimal route
                else -> 5000 // Prev transcendence seed path
            }
            val simulatedTelemetry = ZoneGenerator.generateTelemetryCsv(simulatedScore, mode * 42L)
            
            _simState.value = _simState.value.copy(
                ghostTierMode = mode,
                ghostYPath = ZoneGenerator.parseTelemetry(simulatedTelemetry)
            )
            soundEngine.playTone(440f, 100, "sine")
        }
    }

    private fun parseTelemetryFromSimParameters(): List<Int> {
        return ZoneGenerator.parseTelemetry(ZoneGenerator.generateTelemetryCsv(200, 77))
    }

    override fun onCleared() {
        super.onCleared()
        simJob?.cancel()
    }
}

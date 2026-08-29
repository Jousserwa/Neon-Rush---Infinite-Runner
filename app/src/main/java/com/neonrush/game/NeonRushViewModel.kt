package com.neonrush.game
import android.app.Activity
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
    val xOffsetFraction: Float,
    val yMatchPos: Int,
    val type: String,
    val subType: String = "",
    val isCollected: Boolean = false
)

data class Particle(
    val id: String,
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val age: Int = 0,
    val maxAge: Int = 15,
    val colorArgb: Long,
    val kind: String
)

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
    val currentZoneNumber: Int = 1,
    val zoneDNA: ZoneDNA = ZoneGenerator.generateZone(1, 42),
    val activeTrackElements: List<VisualTrackElement> = emptyList(),
    val activePowerupDurations: Map<String, Int> = emptyMap(),
    val screenShakeX: Float = 0f,
    val screenShakeY: Float = 0f,
    val bossActive: Boolean = false,
    val bossHealth: Float = 1.0f,
    val bossY: Int = 50,
    val collectedGemsCount: Int = 0,
    val gemsEarnedLastRun: Int = 0,
    val ghostTierMode: Int = 1,
    val isTranscendenceUnlocked: Boolean = false,
    val currentMutationName: String = "",
    val frustrationLevelIndex: Float = 0.5f,
    val obstacleDensityMod: Float = 1.0f,
    val lastZoneTransitionTick: Int = -999,
    val particles: List<Particle> = emptyList(),
    val reviveCount: Int = 0,
    val shieldUntilTick: Int = -1,
    val fuelRefillCount: Int = 0,
    val specialWorldId: Int? = null
)
class NeonRushViewModel(
    private val gameDao: GameDao,
    context: Context
) : ViewModel() {

    private val soundEngine = NeonSoundEngine()

    val profile = gameDao.getProfileFlow()

    val shopSkins = listOf(
        Triple("cyan_diamond", "Cyan Diamond", 0),
        Triple("purple_square", "Purple Square", 30),
        Triple("green_triangle", "Green Triangle", 50),
        Triple("magenta_pulse", "Magenta Pulse Racer", 90),
        Triple("gold_transcendence", "Gold Transcendence Vessel", 250),
        Triple("matrix_grid", "Hex Grid Cyber-Fighter", 400)
    )

    val leaderboard: StateFlow<List<LeaderboardPilot>> = FirebaseLeaderboardManager.globalRankings

    private val _simState = MutableStateFlow(SimulationState())
    val simState: StateFlow<SimulationState> = _simState.asStateFlow()

    private val _socialComments = MutableStateFlow<List<SocialComment>>(emptyList())
    val socialComments: StateFlow<List<SocialComment>> = _socialComments.asStateFlow()

    val dailyChallengeTitle = "Methane Glitch Rush"
    val dailyChallengeDesc = "Maximum wind resistance in Zone 3 with critical fuel cells! Finish above 200 points to score bonus gems."
    val dailyChallengeGoal = 200

    private var simJob: Job? = null

    private val _storyEvent = MutableSharedFlow<StoryEvent>(extraBufferCapacity = 4)
    val storyEvent: SharedFlow<StoryEvent> = _storyEvent.asSharedFlow()

    val currentWorld: StateFlow<World> = simState
        .map { Worlds.worldForZone(it.currentZoneNumber) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, Worlds.ALL.first())

    private val firedStoryBeats = mutableSetOf<Pair<Int, StoryBeatType>>()
    private var lastWorldIdForStory = -1

    private fun checkWorldEndingBeat(previousZoneNumber: Int, nextZoneNumber: Int) {
        val world = Worlds.worldForZone(previousZoneNumber)
        if (nextZoneNumber > world.endZone && firedStoryBeats.add(world.id to StoryBeatType.ENDING)) {
            _storyEvent.tryEmit(StoryEvent(world, StoryBeatType.ENDING, world.endingText))
            val rewardSkinId = when (world.id) {
                1 -> "blackout_runner"
                2 -> "signal_ghost"
                3 -> "convict_grey"
                4 -> "apex_predator"
                else -> null
            }
            rewardSkinId?.let { unlockPilotSkinFromStory(it) }
        }
    }

    private fun checkStoryBeats(zoneNumber: Int, bossRisingEdge: Boolean) {
        val world = Worlds.worldForZone(zoneNumber)
        if (world.id != lastWorldIdForStory) {
            lastWorldIdForStory = world.id
            if (firedStoryBeats.add(world.id to StoryBeatType.OPENING)) {
                _storyEvent.tryEmit(StoryEvent(world, StoryBeatType.OPENING, world.openingText))
            }
        }
        val midZone = world.startZone + (world.endZone - world.startZone) / 2
        if (zoneNumber >= midZone && firedStoryBeats.add(world.id to StoryBeatType.MID_RUN)) {
            _storyEvent.tryEmit(StoryEvent(world, StoryBeatType.MID_RUN, world.midRunText))
        }
        if (bossRisingEdge && firedStoryBeats.add(world.id to StoryBeatType.BOSS_INTRO)) {
            _storyEvent.tryEmit(StoryEvent(world, StoryBeatType.BOSS_INTRO, world.bossIntroText))
        }
    }

    private fun resetStoryProgress() {
        firedStoryBeats.clear()
        lastWorldIdForStory = -1
    }

    fun triggerPaywallTeaser(world: World) {
        _storyEvent.tryEmit(
            StoryEvent(
                world,
                StoryBeatType.PAYWALL_TEASER,
                "Command's voice cuts through static: \"Copter's down in the reserve. Something's already found the wreckage. You in?\""
            )
        )
    }

    fun purchaseGemPack(activity: Activity, productId: String, gemAmount: Int) {
    RevenueCatManager.purchaseGemPack(activity, productId) { success ->
        if (success) {
            viewModelScope.launch {
                gameDao.updateProfile { prof -> prof.copy(gems = prof.gems + gemAmount) }
                soundEngine.playUnlockSkin()
            }
        } else {
            _purchaseErrorEvent.tryEmit("Purchase failed. Please try again.")
        }
    }
}
    fun recordAdWatched() {
    viewModelScope.launch {
        gameDao.updateProfile { prof -> MissionManager.recordAdWatched(prof) }
    }
}
    fun purchaseStarterPack(activity: Activity) {
    RevenueCatManager.purchaseStarterPack(activity) { success ->
        if (success) {
            viewModelScope.launch {
                gameDao.updateProfile { prof -> prof.copy(gems = prof.gems + RevenueCatManager.STARTER_PACK_GEMS_AMOUNT) }
                soundEngine.playUnlockSkin()
            }
        } else {
            _purchaseErrorEvent.tryEmit("Purchase failed. Please try again.")
        }
    }
}
fun purchaseRemoveAds(activity: Activity) {
    RevenueCatManager.purchaseRemoveAds(activity) { success ->
        if (success) {
            viewModelScope.launch {
                gameDao.updateProfile { prof -> prof.copy(adsRemoved = true) }
                soundEngine.playUnlockSkin()
            }
        } else {
            _purchaseErrorEvent.tryEmit("Purchase failed. Please try again.")
        }
    }
}
fun claimMission(tier: MissionTier, missionId: String) {
    viewModelScope.launch {
        var rewardGemsResult = 0
        var oldSpecialWorldTier = 0
        var claimed = false
        val finalProfile = gameDao.updateProfile { prof ->
            oldSpecialWorldTier = prof.specialWorldTier
            val result = MissionManager.claimMission(prof, tier, missionId)
            if (result != null) {
                val (updatedProfile, rewardGems) = result
                rewardGemsResult = rewardGems
                claimed = true
                val newTier = MissionManager.checkSpecialWorldQualification(updatedProfile)
                updatedProfile.copy(
                    gems = updatedProfile.gems + rewardGems,
                    specialWorldTier = newTier
                )
            } else {
                prof
            }
        }
        if (claimed) {
            soundEngine.playUnlockSkin()
            if (finalProfile.specialWorldTier > oldSpecialWorldTier) {
                soundEngine.playPersonalBestBroken() // reuse as a celebratory "world unlocked" sound
            }
            AnalyticsManager.logScoreMilestone(rewardGemsResult)
        }
    }
}
fun rerollMissions(tier: MissionTier) {
    viewModelScope.launch {
        val cost = when (tier) {
            MissionTier.DAILY -> 10
            MissionTier.WEEKLY -> 20
            MissionTier.MONTHLY -> 30
        }
        var didReroll = false
        gameDao.updateProfile { prof ->
            if (prof.gems >= cost) {
                didReroll = true
                when (tier) {
                    MissionTier.DAILY -> prof.copy(
                        gems = prof.gems - cost,
                        dailyRerollCount = prof.dailyRerollCount + 1,
                        dailyMissionProgressCsv = "",
                        dailyMissionsClaimedCsv = ""
                    )
                    MissionTier.WEEKLY -> prof.copy(
                        gems = prof.gems - cost,
                        weeklyRerollCount = prof.weeklyRerollCount + 1,
                        weeklyMissionProgressCsv = "",
                        weeklyMissionsClaimedCsv = ""
                    )
                    MissionTier.MONTHLY -> prof.copy(
                        gems = prof.gems - cost,
                        monthlyRerollCount = prof.monthlyRerollCount + 1,
                        monthlyMissionProgressCsv = "",
                        monthlyMissionsClaimedCsv = ""
                    )
                }
            } else {
                prof
            }
        }
        if (didReroll) {
            soundEngine.playUnlockSkin()
        }
    }
}
    fun equipPilotSkin(skinId: String) {
        viewModelScope.launch {
            var didEquip = false
            gameDao.updateProfile { prof ->
                val unlocked = prof.unlockedPilotSkinsCsv.split(",").toSet()
                if (unlocked.contains(skinId)) {
                    didEquip = true
                    prof.copy(activePilotSkinId = skinId)
                } else {
                    prof
                }
            }
            if (didEquip) {
                soundEngine.playTone(400f, 100, "sine")
            }
        }
    }

    fun unlockPilotSkinFromStory(skinId: String) {
        viewModelScope.launch {
            val prof = gameDao.getProfileDirect() ?: GameProfile()
            val unlocked = prof.unlockedPilotSkinsCsv.split(",").toMutableList()
            if (!unlocked.contains(skinId)) {
                unlocked.add(skinId)
                val updated = prof.copy(unlockedPilotSkinsCsv = unlocked.joinToString(","))
                gameDao.saveProfile(updated)
                soundEngine.playUnlockSkin()
            }
        }
    }
fun doubleGemsForRun() {
    viewModelScope.launch {
        val prof = gameDao.getProfileDirect() ?: GameProfile()
        val bonus = _simState.value.gemsEarnedLastRun
        val updated = prof.copy(gems = prof.gems + bonus)
        gameDao.saveProfile(updated)
        soundEngine.playUnlockSkin()
    }
}
fun purchasePilotSkin(activity: Activity, skinId: String, productId: String) {
    RevenueCatManager.purchasePilotSuit(activity, productId) { success ->
        if (success) {
            unlockPilotSkinFromStory(skinId)
            equipPilotSkin(skinId)
        } else {
            _purchaseErrorEvent.tryEmit("Purchase failed. Please try again.")
        }
    }
}
fun reviveCostForCurrentRun(): Int {
    return when (_simState.value.reviveCount) {
        0 -> 20
        1 -> 25
        else -> 30
    }
}

fun reviveWithGems() {
    viewModelScope.launch {
        val prof = gameDao.getProfileDirect() ?: GameProfile()
        val cost = reviveCostForCurrentRun()
        if (_simState.value.reviveCount < 2 && prof.gems >= cost) {
            val updated = prof.copy(gems = prof.gems - cost)
            gameDao.saveProfile(updated)
            reviveSimulation()
        }
    }
}
fun fuelRefillCostForCurrentRun(): Int {
    return when (_simState.value.fuelRefillCount) {
        0 -> 20
        1 -> 25
        else -> 30
    }
}
fun onFuelTierChanged(tier: String) {
    when (tier) {
        "warning" -> soundEngine.playTone(440f, 150, "sine")
        "critical" -> soundEngine.playTone(880f, 200, "sawtooth")
        else -> {}
    }
}

fun refuelWithGems(isPro: Boolean) {
    viewModelScope.launch {
        val current = _simState.value
        if (!isPro && current.fuelRefillCount >= 3) return@launch
        val prof = gameDao.getProfileDirect() ?: GameProfile()
        val cost = fuelRefillCostForCurrentRun()
        if (prof.gems >= cost) {
            val updated = prof.copy(gems = prof.gems - cost)
            gameDao.saveProfile(updated)
            _simState.value = current.copy(
                fuelLevelPercent = 100,
                fuelRefillCount = current.fuelRefillCount + 1
            )
            soundEngine.playUnlockSkin()
        }
    }
}
fun buyExtraAttempt() {
    viewModelScope.launch {
        val prof = gameDao.getProfileDirect() ?: GameProfile()
        val cost = 25
        if (prof.gems >= cost) {
            val updated = prof.copy(
                gems = prof.gems - cost,
                dailyAttemptsToday = (prof.dailyAttemptsToday - 1).coerceAtLeast(0)
            )
            gameDao.saveProfile(updated)
            soundEngine.playUnlockSkin()
        }
    }
}
    private val _streakRewardEvent = MutableSharedFlow<StreakReward>(extraBufferCapacity = 2)
    val streakRewardEvent: SharedFlow<StreakReward> = _streakRewardEvent.asSharedFlow()
    private val _purchaseErrorEvent = MutableSharedFlow<String>(extraBufferCapacity = 2)
    val purchaseErrorEvent: SharedFlow<String> = _purchaseErrorEvent.asSharedFlow()

    fun checkDailyStreak() {
    viewModelScope.launch {
        val prof = gameDao.getProfileDirect() ?: GameProfile()
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val today = fmt.format(Date())
        if (prof.lastStreakLoginDate == today) {
            return@launch
        }
        val newStreak = if (prof.lastStreakLoginDate.isEmpty()) {
            1
        } else {
            val lastDate = fmt.parse(prof.lastStreakLoginDate)
            val todayDate = fmt.parse(today)
            val diffDays = ((todayDate.time - lastDate.time) / (1000 * 60 * 60 * 24)).toInt()
            if (diffDays == 1) prof.currentStreak + 1 else 1
        }
        val reward = StreakRewards.rewardForDay(newStreak)
        val updated = prof.copy(
            currentStreak = newStreak,
            lastStreakLoginDate = today,
            gems = prof.gems + reward.gems
        )
        gameDao.saveProfile(updated)
        soundEngine.playUnlockSkin()
        _streakRewardEvent.tryEmit(reward)
    }
}

fun streakDaysMissed(prof: GameProfile): Int {
    if (prof.lastStreakLoginDate.isEmpty()) return 0
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val today = fmt.format(Date())
    if (prof.lastStreakLoginDate == today) return 0
    val lastDate = fmt.parse(prof.lastStreakLoginDate)
    val todayDate = fmt.parse(today)
    return ((todayDate.time - lastDate.time) / (1000 * 60 * 60 * 24)).toInt()
}
suspend fun isStreakFreezeEligible(): Boolean {
    val prof = gameDao.getProfileDirect() ?: GameProfile()
    return streakDaysMissed(prof) == 2 && prof.currentStreak > 0
}
fun freezeStreak() {
    viewModelScope.launch {
        val prof = gameDao.getProfileDirect() ?: GameProfile()
        val cost = 25
        val missed = streakDaysMissed(prof)
        if (missed == 2 && prof.gems >= cost) {
            val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -1)
            val yesterday = fmt.format(cal.time)
            val updated = prof.copy(gems = prof.gems - cost, lastStreakLoginDate = yesterday)
            gameDao.saveProfile(updated)
            soundEngine.playUnlockSkin()
            checkDailyStreak()
        }
    }
}


    init {
        loadSocialComments()
        prepopulateSampleGhostChallenges()
        viewModelScope.launch {
            FirebaseLeaderboardManager.fetchTopScores()
        }
    }

    private fun prepopulateSampleGhostChallenges() {
        viewModelScope.launch {
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
        viewModelScope.launch {
            FirebaseLeaderboardManager.fetchTopScores()
        }
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
            loadDefaultLeaderboard()
        }
    }

    fun purchaseSkin(skinId: String, cost: Int) {
        viewModelScope.launch {
            val prof = gameDao.getProfileDirect() ?: GameProfile()
            val unlockedSkins = prof.unlockedSkinsCsv.split(",").toMutableList()
            if (unlockedSkins.contains(skinId)) {
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

    var avgZoneReached: Float = 10f
    var frustrationIndex: Float = 0.5f

    enum class DifficultyTier(val speedMultiplier: Float, val spacingMultiplier: Float, val label: String) {
        EASY(0.75f, 1.35f, "EASY"),
        MEDIUM(1.0f, 1.0f, "MEDIUM"),
        HARD(1.2f, 0.8f, "HARD"),
        LEGENDARY(1.4f, 0.65f, "LEGENDARY")
    }

    var selectedDifficulty: DifficultyTier = DifficultyTier.MEDIUM

    fun setDifficulty(tier: DifficultyTier) {
        selectedDifficulty = tier
    }

    var deathTimes: MutableList<Long> = mutableListOf()
    val deathHeatmap: MutableMap<String, Int> = mutableMapOf()
    var hitObstaclesHistory: MutableList<String> = mutableListOf()
    var totalPlayedSeconds: Float = 0f
    var lastPlayTime: Long = System.currentTimeMillis()

    private fun chooseProceduralPowerupType(rand: kotlin.random.Random, isSunday: Boolean): String {
        val roll = rand.nextInt(100)
        val legendaryThreshold = if (isSunday) 25 else 5
        val rareThreshold = if (isSunday) 55 else 25
        return when {
            roll < legendaryThreshold -> if (rand.nextBoolean()) "PU11" else "PU12"
            roll < legendaryThreshold + rareThreshold -> "PU" + rand.nextInt(6, 11)
            else -> "PU" + rand.nextInt(1, 6)
        }
    }

    private fun spawnObstacleForSet(obstacleSetId: Int, tick: Int, rand: kotlin.random.Random, ghostY: Int): List<VisualTrackElement> {
        val elements = mutableListOf<VisualTrackElement>()
        val baseId = "obs_${tick}_"
        when (obstacleSetId) {
            1 -> {
                val gapSize = rand.nextInt(20, 30)
                val gapCenter = (ghostY + rand.nextInt(-10, 10)).coerceIn(35, 65)
                elements.add(VisualTrackElement("${baseId}p1", 1.2f, gapCenter + gapSize/2, "obstacle", "PILLAR_BOTTOM"))
                elements.add(VisualTrackElement("${baseId}p2", 1.2f, gapCenter - gapSize/2, "obstacle", "PILLAR_TOP"))
            }
            2 -> {
                elements.add(VisualTrackElement("${baseId}l1", 1.2f, rand.nextInt(20, 80), "obstacle", "LASER"))
                if (rand.nextBoolean()) {
                    elements.add(VisualTrackElement("${baseId}l2", 1.25f, rand.nextInt(20, 80), "obstacle", "LASER"))
                }
            }
            3 -> elements.add(VisualTrackElement("${baseId}b1", 1.3f, ghostY, "obstacle", "BLADE"))
            4 -> elements.add(VisualTrackElement("${baseId}st1", 1.2f, rand.nextInt(15, 45), "obstacle", "STALACTITE"))
            5 -> elements.add(VisualTrackElement("${baseId}sm1", 1.2f, rand.nextInt(55, 85), "obstacle", "STALAGMITE"))
            6 -> {
                elements.add(VisualTrackElement("${baseId}sa1", 1.2f, 20, "obstacle", "STALACTITE"))
                elements.add(VisualTrackElement("${baseId}sa2", 1.2f, 80, "obstacle", "STALAGMITE"))
            }
            7, 11 -> {
                elements.add(VisualTrackElement("${baseId}m1", 1.2f, ghostY - 12, "obstacle", "BARRIER"))
                elements.add(VisualTrackElement("${baseId}m2", 1.2f, ghostY + 12, "obstacle", "BARRIER"))
            }
            15 -> elements.add(VisualTrackElement("${baseId}z1", 1.2f, rand.nextInt(10, 90), "obstacle", "ZAP_FIELD"))
            17 -> elements.add(VisualTrackElement("${baseId}f1", 1.2f, ghostY + rand.nextInt(-10, 10).coerceIn(15, 85), "obstacle", "PHANTOM"))
            18 -> {
                elements.add(VisualTrackElement("${baseId}s1", 1.2f, ghostY - 6, "obstacle", "SPLITTER"))
                elements.add(VisualTrackElement("${baseId}s2", 1.2f, ghostY + 6, "obstacle", "SPLITTER"))
            }
            22 -> {
                elements.add(VisualTrackElement("${baseId}tu1", 1.2f, ghostY - 20, "obstacle", "TUNNEL_TOP"))
                elements.add(VisualTrackElement("${baseId}tu2", 1.2f, ghostY + 20, "obstacle", "TUNNEL_BOTTOM"))
            }
            else -> elements.add(VisualTrackElement("${baseId}st", 1.2f, ghostY + rand.nextInt(-12, 12).coerceIn(15, 85), "obstacle", "STANDARD"))
        }
        return elements
    }

    fun triggerTranscendence() {
        viewModelScope.launch {
            val prof = gameDao.getProfileDirect() ?: GameProfile()
            if (prof.bestScore >= 5000 || prof.transcendenceCount > 0) {
                val nextPrestige = prof.transcendenceCount + 1
                val currentUnlocked = prof.unlockedSkinsCsv.split(",").filter { it.isNotEmpty() }.toMutableList()
                val exclusiveSkin = when (nextPrestige) {
                    1 -> "gold_transcendence"
                    2 -> "matrix_grid"
                    else -> "elite_nebula"
                }
                if (!currentUnlocked.contains(exclusiveSkin)) {
                    currentUnlocked.add(exclusiveSkin)
                }
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

    fun runDailyRushChallenge(onComplete: (Boolean, Int) -> Unit) {
        viewModelScope.launch {
            val prof = gameDao.getProfileDirect() ?: GameProfile()
            val todayDate = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
            val attempts = if (prof.lastDailyRushDate == todayDate) prof.dailyAttemptsToday else 0
            if (attempts >= 3) {
                onComplete(false, 0)
                return@launch
            }
            val updated = prof.copy(
                dailyAttemptsToday = attempts + 1,
                lastDailyRushDate = todayDate
            )
            gameDao.saveProfile(updated)
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

    fun overrideEnvironment(dna: ZoneDNA, envId: Int): ZoneDNA {
    val (name, emoji, color) = ZoneGenerator.ENVIRONMENTS[envId]
    return dna.copy(environmentId = envId, environmentName = name, environmentEmoji = emoji, environmentColor = color)
}

fun startSpecialModeRun(ghost: GhostChallengeEntity) {
    viewModelScope.launch {
        val prof = gameDao.getProfileDirect() ?: GameProfile()
        val world = Worlds.specialWorldForTier(prof.specialWorldTier)
        val envId = world?.environmentIds?.firstOrNull()
        if (envId != null) {
            startRacingSimulation(ghost, specialWorldId = envId)
        }
    }
}

fun startRacingSimulation(ghost: GhostChallengeEntity, specialWorldId: Int? = null) {
    simJob?.cancel()
    var startupDna = ZoneGenerator.generateZone(1, 42)
    if (specialWorldId != null) startupDna = overrideEnvironment(startupDna, specialWorldId)
    val todayMutation = DailyMutations.getActiveMutation()
    val isFirstLucky = (System.currentTimeMillis() - lastPlayTime) > 3 * 24 * 3600 * 1000L
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
        currentMutationName = todayMutation.title,
        specialWorldId = specialWorldId
    )
        soundEngine.setHomeScreenActiveState(false)
        soundEngine.playThrusterCharge()
        AnalyticsManager.logGameStart()
        simJob = viewModelScope.launch {
            val milestonesTriggered = mutableSetOf<Int>()
            val prof = gameDao.getProfileDirect() ?: GameProfile()
            var tick = 0
            val random = kotlin.random.Random(System.currentTimeMillis())
            var runStartTime = System.currentTimeMillis()
            val currentFrustration = frustrationIndex
            val spacingBias = if (currentFrustration > 3.0f) 0.85f else if (avgZoneReached > 15f) 1.10f else 1.0f
            val isLuckyActive = isFirstLucky
            var liveDifficultyMultiplier = 1.0f
            var ticksSinceLastHit = 0
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
                if (hasSlowTime || isLuckyActive) {
                    speedInPx *= 0.50f
                }
                val hasHyperdrive = state.zoneDNA.mechanicIds.contains(1)
                if (hasHyperdrive) {
                    speedInPx *= 2.0f
                }
                speedInPx *= liveDifficultyMultiplier * selectedDifficulty.speedMultiplier
                val tickDistanceOffset = speedInPx * 3.6f
                val nextDistance = state.distanceMeters + tickDistanceOffset
                val nextZoneNumber = (nextDistance / 500f).toInt() + 1
                var activeDna = ZoneGenerator.generateZone(nextZoneNumber, random.nextLong())
                if (state.specialWorldId != null) activeDna = overrideEnvironment(activeDna, state.specialWorldId)
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
                if (tick % (10 / spacingBias).coerceIn(4f, 25f).toInt() == 0 && !activeDna.mechanicIds.contains(19)) {
                    val gridX = 1.2f
                    val routeTargetY = state.ghostYPath.getOrNull(tick % state.ghostYPath.size.coerceAtLeast(1)) ?: 50
                    if (random.nextInt(100) < 9) {
                        updatedElements.add(VisualTrackElement("gem_${tick}", gridX, routeTargetY + random.nextInt(-8, 8), "gem"))
                    } else if (random.nextInt(100) < 15) {
                        updatedElements.add(VisualTrackElement("fuel_${tick}", gridX, routeTargetY + random.nextInt(-5, 5), "fuel"))
                    }
                }
                val spacingVal = (activeDna.obstacleSpacingAndDensity / (12f * spacingBias * liveDifficultyMultiplier * selectedDifficulty.spacingMultiplier)).coerceAtLeast(4f).toInt()
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
                    bossYState += (trackingYBias * 0.05f).toInt()
                    if (tick % 9 == 0) {
                        updatedElements.add(VisualTrackElement("bullet_${tick}", 1.15f, bossYState + random.nextInt(-18, 18), "bullet"))
                    }
                    bossHealthState -= 0.04f
                    if (bossHealthState <= 0f) {
                        soundEngine.playTone(990f, 400, "sine")
                        updatedMsg = "BOSS DEFEATED! Prestige Reward Cells gathered!"
                        val bossReward = nextZoneNumber * 12
                        val currentGems = prof.gems + bossReward
                        gameDao.saveProfile(prof.copy(gems = currentGems))
                    }
                }
                var fuelLevelState = state.fuelLevelPercent
                var gemsGathered = state.collectedGemsCount
                val nextDurationsMap = state.activePowerupDurations.toMutableMap()
                val finalElements = mutableListOf<VisualTrackElement>()
                val activeParticles = state.particles.toMutableList()
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
                var hitThisTick = false
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
                                    repeat(6) { i ->
                                        activeParticles.add(
                                            Particle(
                                                id = "sp_${tick}_$i",
                                                x = elem.xOffsetFraction,
                                                y = elem.yMatchPos.toFloat(),
                                                vx = random.nextFloat() * 0.02f - 0.01f,
                                                vy = random.nextFloat() * 6f - 3f,
                                                maxAge = 12,
                                                colorArgb = 0xFF00E5FFL,
                                                kind = "sparkle"
                                            )
                                        )
                                    }
                                }
                                "fuel" -> {
                                    soundEngine.playTone(523f, 80, "sine")
                                    fuelLevelState = (fuelLevelState + 18).coerceAtMost(100)
                                    repeat(6) { i ->
                                        activeParticles.add(
                                            Particle(
                                                id = "sp_${tick}_$i",
                                                x = elem.xOffsetFraction,
                                                y = elem.yMatchPos.toFloat(),
                                                vx = random.nextFloat() * 0.02f - 0.01f,
                                                vy = random.nextFloat() * 6f - 3f,
                                                maxAge = 12,
                                                colorArgb = 0xFF00FF88L,
                                                kind = "sparkle"
                                            )
                                        )
                                    }
                                }
                                "powerup" -> {
                                    soundEngine.playShieldPowerup()
                                    val puId = elem.subType
                                    if (puId == "PU10") {
                                        val perfectY = state.ghostYPath.getOrNull(tick % state.ghostYPath.size.coerceAtLeast(1)) ?: 50
                                        _simState.value = _simState.value.copy(userYPos = perfectY)
                                        updatedMsg = "LANE WARP COMPLETE! DRIFT SAFE LINE SECURED!"
                                    } else if (puId == "PU11") {
                                        val mToSkip = 500f - (nextDistance % 500f) + 10f
                                        _simState.value = state.copy(distanceMeters = nextDistance + mToSkip, currentZoneNumber = nextZoneNumber + 1)
                                        updatedMsg = "QUANTUM ZONE LEAP ENGAGED!"
                                    } else if (puId == "PU12") {
                                        for (i in 1..9) {
                                            nextDurationsMap["PU$i"] = 40
                                        }
                                        updatedMsg = "LEGENDARY PRESTIGE MATRIX SYNC ACTIVE!"
                                    } else {
                                        nextDurationsMap[puId] = 80
                                    }
                                    repeat(8) { i ->
                                        activeParticles.add(
                                            Particle(
                                                id = "sp_${tick}_$i",
                                                x = elem.xOffsetFraction,
                                                y = elem.yMatchPos.toFloat(),
                                                vx = random.nextFloat() * 0.02f - 0.01f,
                                                vy = random.nextFloat() * 6f - 3f,
                                                maxAge = 14,
                                                colorArgb = 0xFFFF00FFL,
                                                kind = "sparkle"
                                            )
                                        )
                                    }
                                }
                                "obstacle", "bullet" -> {
    val hasReviveShield = tick < state.shieldUntilTick
    if (hasInvincibility || hasGhostMode || hasReviveShield) {
                                    } else if (hasShield) {
                                        soundEngine.playShieldBreak()
                                        nextDurationsMap.remove("PU1")
                                        hasShield = false
                                        updatedMsg = "DRIFT DEFLECTED: SHIELD BARRIER OVERLOADED"
                                    } else {
                                        soundEngine.playCollision()
                                        fuelLevelState = (fuelLevelState - 20).coerceAtLeast(0)
                                        updatedMsg = "WARNING: IMPACT DETECTED! HULL INTEGRITY LOST"
                                        hitObstaclesHistory.add(elem.subType)
                                        hitThisTick = true
                                        repeat(10) { i ->
                                            activeParticles.add(
                                                Particle(
                                                    id = "ex_${tick}_$i",
                                                    x = elem.xOffsetFraction,
                                                    y = elem.yMatchPos.toFloat(),
                                                    vx = random.nextFloat() * 0.04f - 0.02f,
                                                    vy = random.nextFloat() * 10f - 5f,
                                                    maxAge = 16,
                                                    colorArgb = 0xFFFF5500L,
                                                    kind = "explosion"
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                            continue
                        }
                    }
                    finalElements.add(elem)
                }
                val agedParticles = activeParticles.mapNotNull { p ->
                    val newAge = p.age + 1
                    if (newAge >= p.maxAge) null
                    else p.copy(age = newAge, x = p.x + p.vx, y = (p.y + p.vy).coerceIn(0f, 100f))
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
                    particles = agedParticles,
                    isTranscendenceUnlocked = nextZoneNumber >= 50 || prof.transcendenceCount > 0
                )
            }
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
        val isNewPB = finalState.score > prof.bestScore
        val todayMutation = DailyMutations.getActiveMutation()
        val isMonday = todayMutation == MutationDay.MONDAY
        val isFriday = todayMutation == MutationDay.FRIDAY
        val valMultiplier = if (isMonday) 2 else 1
        val FridayBonus = if (isFriday) 25 else 0
        var bonusGems = 0
        if (finalState.activeGhost?.challengeId == "daily_hard_ghost" && finalState.score >= dailyChallengeGoal) {
            bonusGems = 55
            soundEngine.playUnlockSkin()
        }
        val GEM_ECONOMY_RATE = (1f / 3f)
val gemsEarnedThisRun = (((finalState.collectedGemsCount + bonusGems + FridayBonus) * valMultiplier) * GEM_ECONOMY_RATE).toInt()
        android.util.Log.d("GEM_DEBUG", "collected=${finalState.collectedGemsCount} bonus=$bonusGems friday=$FridayBonus mult=$valMultiplier earned=$gemsEarnedThisRun profGemsBefore=${prof.gems}")

val newTotalRuns = prof.totalRuns + 1
val newAverageScore = ((prof.averageScore * prof.totalRuns) + finalState.score) / newTotalRuns

val bestZoneLifetime = maxOf(prof.bestZoneReached, finalState.currentZoneNumber)
val updated = prof.copy(
    bestScore = if (isNewPB) finalState.score else prof.bestScore,
    gems = prof.gems + gemsEarnedThisRun,
    totalRuns = newTotalRuns,
    averageScore = newAverageScore,
    totalGemsEarned = prof.totalGemsEarned + gemsEarnedThisRun,
    bestZoneReached = bestZoneLifetime
)
_simState.value = _simState.value.copy(gemsEarnedLastRun = gemsEarnedThisRun)
val missionUpdated = MissionManager.recordRunResult(
    updated,
    zoneReached = finalState.currentZoneNumber,
    score = finalState.score,
    gemsThisRun = gemsEarnedThisRun,
    bestZoneLifetime = bestZoneLifetime
)
gameDao.saveProfile(missionUpdated)
AnalyticsManager.logGameOver(
    score = finalState.score,
    isNewPB = isNewPB,
    zoneReached = finalState.currentZoneNumber
)
if (!updated.adsRemoved) {
    AdMobManager.incrementGameOver()
}
        if (isNewPB) {
            soundEngine.playPersonalBestBroken()
            FirebaseLeaderboardManager.submitScore(prof.username, finalState.score, prof.activeSkinId)
        } else {
            soundEngine.playCollision()
        }
    }

    fun reviveSimulation() {
    val currentState = _simState.value
    if (currentState.reviveCount >= 2) return
    val currentTick = currentState.tickIndex
    _simState.value = currentState.copy(
        isCompleted = false,
        fuelLevelPercent = 100,
        feedbackMessage = "Revive code accepted! Launching drone boosters...",
        reviveCount = currentState.reviveCount + 1,
        shieldUntilTick = currentTick + 25, // ~3 seconds of invulnerability at 120ms/tick
        fuelRefillCount = 0 // fresh life = fresh set of 3 refuels
    )
    simJob?.cancel()
    soundEngine.playRevive()
        simJob = viewModelScope.launch {
            val milestonesTriggered = mutableSetOf<Int>()
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
                val activeParticles = state.particles.toMutableList()
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
                                    repeat(6) { i ->
                                        activeParticles.add(
                                            Particle(
                                                id = "sp_${tick}_$i",
                                                x = elem.xOffsetFraction,
                                                y = elem.yMatchPos.toFloat(),
                                                vx = random.nextFloat() * 0.02f - 0.01f,
                                                vy = random.nextFloat() * 6f - 3f,
                                                maxAge = 12,
                                                colorArgb = 0xFF00E5FFL,
                                                kind = "sparkle"
                                            )
                                        )
                                    }
                                }
                                "fuel" -> {
                                    soundEngine.playTone(523f, 80, "sine")
                                    fuelLevelState = (fuelLevelState + 18).coerceAtMost(100)
                                    repeat(6) { i ->
                                        activeParticles.add(
                                            Particle(
                                                id = "sp_${tick}_$i",
                                                x = elem.xOffsetFraction,
                                                y = elem.yMatchPos.toFloat(),
                                                vx = random.nextFloat() * 0.02f - 0.01f,
                                                vy = random.nextFloat() * 6f - 3f,
                                                maxAge = 12,
                                                colorArgb = 0xFF00FF88L,
                                                kind = "sparkle"
                                            )
                                        )
                                    }
                                }
                                "powerup" -> {
                                    soundEngine.playShieldPowerup()
                                    val puId = elem.subType
                                    repeat(8) { i ->
                                        activeParticles.add(
                                            Particle(
                                                id = "sp_${tick}_$i",
                                                x = elem.xOffsetFraction,
                                                y = elem.yMatchPos.toFloat(),
                                                vx = random.nextFloat() * 0.02f - 0.01f,
                                                vy = random.nextFloat() * 6f - 3f,
                                                maxAge = 14,
                                                colorArgb = 0xFFFF00FFL,
                                                kind = "sparkle"
                                            )
                                        )
                                    }
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
                                    } else if (hasShield) {
                                        soundEngine.playShieldBreak()
                                        nextDurationsMap.remove("PU1")
                                        hasShield = false
                                        updatedMsg = "SHIELD BROKEN!"
                                    } else {
                                        soundEngine.playCollision()
                                        fuelLevelState = (fuelLevelState - 20).coerceAtLeast(0)
                                        updatedMsg = "IMPACT IMPACT!"
                                        repeat(10) { i ->
                                            activeParticles.add(
                                                Particle(
                                                    id = "ex_${tick}_$i",
                                                    x = elem.xOffsetFraction,
                                                    y = elem.yMatchPos.toFloat(),
                                                    vx = random.nextFloat() * 0.04f - 0.02f,
                                                    vy = random.nextFloat() * 10f - 5f,
                                                    maxAge = 16,
                                                    colorArgb = 0xFFFF5500L,
                                                    kind = "explosion"
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                            continue
                        }
                    }
                    finalElements.add(elem)
                }
                val agedParticles = activeParticles.mapNotNull { p ->
                    val newAge = p.age + 1
                    if (newAge >= p.maxAge) null
                    else p.copy(age = newAge, x = p.x + p.vx, y = (p.y + p.vy).coerceIn(0f, 100f))
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
                        AnalyticsManager.logScoreMilestone(ms)
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

    fun adjustUserY(delta: Int) {
        val activeMechs = _simState.value.zoneDNA.mechanicIds
        val relativeDelta = if (activeMechs.contains(2)) -delta else delta
        val nextY = (_simState.value.userYPos + relativeDelta).coerceIn(10, 90)
        _simState.value = _simState.value.copy(userYPos = nextY)
    }

    fun setGhostModeTier(mode: Int) {
        viewModelScope.launch {
            val prof = gameDao.getProfileDirect() ?: GameProfile()
            val simulatedScore = when (mode) {
                1 -> prof.bestScore.coerceAtLeast(100)
                2 -> 400
                3 -> 1200
                4 -> 2200
                else -> 5000
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

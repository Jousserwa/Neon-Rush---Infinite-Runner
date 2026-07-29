package com.neonrush.game.ui

import android.app.Activity
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.PI
import kotlin.math.pow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import com.neonrush.game.R
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.delay 
import com.neonrush.game.StreakReward

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neonrush.game.AdMobManager
import com.neonrush.game.DailyMutations
import com.neonrush.game.MutationDay
import com.neonrush.game.NeonRushViewModel
import com.neonrush.game.RevenueCatManager
import com.neonrush.game.SimulationState
import com.neonrush.game.ZoneGenerator
import com.neonrush.game.StoryBannerHost
import com.neonrush.game.Skins
import com.neonrush.game.db.GameProfile
import com.neonrush.game.ui.theme.*
import com.neonrush.game.MissionTier
import com.neonrush.game.MissionManager
import com.neonrush.game.MissionTemplate
@Composable
fun NeonRushApp(viewModel: NeonRushViewModel) {
    val activeProfile by viewModel.profile.collectAsState(initial = GameProfile())
    val currentProfile = activeProfile ?: GameProfile()

    var activeTab by remember { mutableStateOf("arcade") }
    var showGhostSelection by remember { mutableStateOf(false) }
    val simState by viewModel.simState.collectAsState()
    
    val isPro by RevenueCatManager.isPro.collectAsState()
    var showPaywall by remember { mutableStateOf(false) }
    var paywallReason by remember { mutableStateOf("generic") }
    
    var showStreakFreezeOffer by remember { mutableStateOf(false) }
LaunchedEffect(Unit) {
    if (viewModel.isStreakFreezeEligible()) {
        showStreakFreezeOffer = true
    } else {
        viewModel.checkDailyStreak()
    }
}

if (showStreakFreezeOffer) {
    AlertDialog(
        onDismissRequest = { },
        title = { Text("🔥 Streak at risk!", fontFamily = FontFamily.Monospace) },
        text = { Text("You missed a day. Spend 15 gems to freeze your streak and keep it going?") },
        confirmButton = {
            Button(onClick = {
                viewModel.freezeStreak()
                showStreakFreezeOffer = false
            }) {
                Text("💎 FREEZE (15 GEMS)")
            }
        },
        dismissButton = {
            Button(onClick = {
                viewModel.checkDailyStreak()
                showStreakFreezeOffer = false
            }) {
                Text("No thanks")
            }
        }
    )
}

    var streakBannerReward by remember { mutableStateOf<StreakReward?>(null) }
    LaunchedEffect(Unit) {
        viewModel.streakRewardEvent.collect { reward ->
            streakBannerReward = reward
            delay(4000)
            streakBannerReward = null
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (!simState.isStarted || simState.isCompleted) {
                Column {
                    // Show home screen banner if user is NOT PRO
                    if (!isPro) {
                        AdMobBannerView(
                            adUnitId = "ca-app-pub-3841327492203214/6533049489",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    NavigationBar(
                        containerColor = CyberSurface,
                        modifier = Modifier
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .border(1.dp, CyberPrimary.copy(alpha = 0.15f), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    ) {
                        NavigationBarItem(
                            selected = activeTab == "arcade",
                            onClick = { 
                                activeTab = "arcade" 
                                showGhostSelection = false
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = CyberPrimary,
                                unselectedIconColor = CyberOnSurface.copy(alpha = 0.5f),
                                indicatorColor = CyberPrimary.copy(alpha = 0.1f)
                            ),
                            icon = { Icon(Icons.Filled.PlayArrow, contentDescription = "Arcade") },
                            label = { Text("Arcade", fontFamily = FontFamily.Monospace, fontSize = 11.sp) }
                        )
                        NavigationBarItem(
                            selected = activeTab == "rankings",
                            onClick = { activeTab = "rankings" },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = CyberPrimary,
                                unselectedIconColor = CyberOnSurface.copy(alpha = 0.5f),
                                indicatorColor = CyberPrimary.copy(alpha = 0.1f)
                            ),
                            icon = { Icon(Icons.Filled.List, contentDescription = "Rankings") },
                            label = { Text("Rankings", fontFamily = FontFamily.Monospace, fontSize = 11.sp) }
                        )
                        NavigationBarItem(
                            selected = activeTab == "social",
                            onClick = { activeTab = "social" },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = CyberPrimary,
                                unselectedIconColor = CyberOnSurface.copy(alpha = 0.5f),
                                indicatorColor = CyberPrimary.copy(alpha = 0.1f)
                            ),
                            icon = { Icon(Icons.Filled.Star, contentDescription = "Social") },
                            label = { Text("Social", fontFamily = FontFamily.Monospace, fontSize = 11.sp) }
                        )
                        NavigationBarItem(
                            selected = activeTab == "skins",
                            onClick = { activeTab = "skins" },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = CyberPrimary,
                                unselectedIconColor = CyberOnSurface.copy(alpha = 0.5f),
                                indicatorColor = CyberPrimary.copy(alpha = 0.1f)
                            ),
                            icon = {
                                BadgedBox(badge = { Badge(containerColor = CyberSecondary) }) {
                                    Icon(Icons.Filled.ShoppingCart, contentDescription = "Skins")
                                }
                            },
                            label = { Text("Skins", fontFamily = FontFamily.Monospace, fontSize = 11.sp) }
                        )
                        NavigationBarItem(
                            selected = activeTab == "profile",
                            onClick = { activeTab = "profile" },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = CyberPrimary,
                                unselectedIconColor = CyberOnSurface.copy(alpha = 0.5f),
                                indicatorColor = CyberPrimary.copy(alpha = 0.1f)
                            ),
                            icon = { Icon(Icons.Filled.Person, contentDescription = "Profile") },
                            label = { Text("Profile", fontFamily = FontFamily.Monospace, fontSize = 11.sp) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(CyberBackground, Color(0xFF030206))
                    )
                )
                .padding(innerPadding)
        ) {
            if (simState.isStarted && !simState.isCompleted) {
                RacingSimulatorScreen(
    simState = simState,
    viewModel = viewModel,
    isPro = isPro,
    profile = currentProfile,
    onShowPaywall = { showPaywall = true; paywallReason = "world4" }
)
            } else if (simState.isStarted && simState.isCompleted) {
                GameOverOverlayScreen(
                    simState = simState,
                    viewModel = viewModel,
                    isPro = isPro,
                    profile = currentProfile,
                    onShowPaywall = { showPaywall = true }
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    AnimatedContent(
                        targetState = activeTab,
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        },
                        label = "MainTabsAnim"
                    ) { tab ->
                        when (tab) {
                            "arcade" -> {
                                if (showGhostSelection) {
                                    GhostRacerTab(viewModel = viewModel, onBack = { showGhostSelection = false })
                                } else {
                                    ArcadeHomeView(
                                        profile = currentProfile,
                                        viewModel = viewModel,
                                        onStartRush = {
                                            val defaultGhost = com.neonrush.game.db.GhostChallengeEntity(
                                                "ghost_cyberrunner", 
                                                "CyberRunner", 
                                                850, 
                                                4, 
                                                ZoneGenerator.generateTelemetryCsv(850, 111)
                                            )
                                            viewModel.startRacingSimulation(defaultGhost)
                                        },
                                        onShowGhostSelection = { showGhostSelection = true },
                                        onNavigateToGlobal = { activeTab = "rankings" },
                                        onNavigateToSkins = { activeTab = "skins" },
                                        onNavigateToSocial = { activeTab = "social" },
                                        isPro = isPro
                                    
                                
                                    )
                                }
                            }
                            "rankings" -> LeaderboardsTab(viewModel = viewModel, playerProfile = currentProfile)
                            "social" -> DailyChallengeTab(viewModel = viewModel, profile = currentProfile)
                            "skins" -> SkinsDeckTab(viewModel = viewModel, profile = currentProfile)
                            "profile" -> ProfileTab(profile = currentProfile, viewModel = viewModel)
                        }
                    }
                }
            }

            streakBannerReward?.let { reward ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(top = 24.dp, start = 20.dp, end = 20.dp)
                        .background(Color(0xE6120324), RoundedCornerShape(12.dp))
                        .border(1.dp, CyberPrimary.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "🔥 ${reward.label}",
                            color = CyberPrimary,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "+${reward.gems} GEMS",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 16.sp
                        )
                    }
                }
            }   
        }
    }

    if (showPaywall) {
        PaywallDialog(onDismiss = { showPaywall = false }, reason = paywallReason)
    }
}

@Composable
fun HeaderProfileDeck(profile: GameProfile, viewModel: NeonRushViewModel) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                Brush.horizontalGradient(listOf(CyberPrimary, CyberSecondary)),
                RoundedCornerShape(12.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "🚀",
                        fontSize = 24.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Column {
                        Text(
                            text = profile.username,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            fontFamily = FontFamily.Monospace,
                            color = CyberPrimary
                        )
                        Text(
                            text = "TRANSCENDENCE LEVEL: ${profile.transcendenceCount}",
                            fontSize = 11.sp,
                            color = CyberSecondary,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.padding(end = 12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "💎",
                            fontSize = 16.sp,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text(
                            text = "${profile.gems} GEMS",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        )
                    }
                    Text(
                        text = "BEST: ${profile.bestScore} PTS",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = CyberPrimary.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
fun LeaderboardsTab(viewModel: NeonRushViewModel, playerProfile: GameProfile) {
    val rankingList by viewModel.leaderboard.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "⚡ NEURAL LEADERBOARD RUSH",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = CyberPrimary,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Follow and challenge ghost trails of active pilots sync'd from the host.",
            color = CyberOnSurface.copy(alpha = 0.7f),
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(rankingList) { pilot ->
                val isSelf = pilot.name == playerProfile.username
                val borderBrush = if (isSelf) {
                    Brush.horizontalGradient(listOf(CyberPrimary, CyberSecondary))
                } else {
                    Brush.horizontalGradient(listOf(CyberSurface, CyberSurface))
                }

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelf) CyberSurface.copy(alpha = 0.8f) else CyberSurface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, borderBrush, RoundedCornerShape(8.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val rankColor = when (pilot.rank) {
                                1 -> GoldAccent
                                2 -> SilverAccent
                                3 -> BronzeAccent
                                else -> CyberOnSurface.copy(alpha = 0.5f)
                            }
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(rankColor.copy(alpha = 0.2f))
                            ) {
                                Text(
                                    text = "#${pilot.rank}",
                                    color = rankColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = pilot.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = if (isSelf) CyberPrimary else Color.White
                                    )
                                    if (pilot.isFollowed) {
                                        Text(
                                            text = " ✓ Followed",
                                            fontSize = 9.sp,
                                            color = CyberSecondary,
                                            modifier = Modifier.padding(start = 6.dp),
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                                Text(
                                    text = "Active Zone: ${pilot.activeZone}",
                                    fontSize = 11.sp,
                                    color = CyberOnSurface.copy(alpha = 0.6f)
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(
                                horizontalAlignment = Alignment.End,
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Text(
                                    text = "${pilot.bestScore}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace,
                                    color = CyberPrimary
                                )
                                Text(
                                    text = "PTS",
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = CyberOnSurface.copy(alpha = 0.4f)
                                )
                            }

                            if (!isSelf) {
                                IconButton(
                                    onClick = { viewModel.toggleFollowUser(pilot.name) },
                                    modifier = Modifier.testTag("follow_pilot_button")
                                ) {
                                    Icon(
                                        imageVector = if (pilot.isFollowed) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                        contentDescription = "Follow Pilot",
                                        tint = if (pilot.isFollowed) CyberSecondary else CyberOnSurface.copy(alpha = 0.4f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GhostRacerTab(viewModel: NeonRushViewModel, onBack: () -> Unit) {
    var selectedPlayerId by remember { mutableStateOf("ghost_cyberrunner") }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back to home dashboard",
                    tint = CyberPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "👾 INVICIBLE RACE",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = CyberPrimary,
                fontFamily = FontFamily.Monospace
            )
        }

        Text(
            text = "Race against the exact flight paths recorded by top pilots. Slide to match their offsets in the light stream.",
            color = CyberOnSurface.copy(alpha = 0.7f),
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = "Select Competitor Ghost:",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        val competitors = listOf(
            Pair("ghost_retro", "RetroWave (240 pts)"),
            Pair("ghost_zeroglitch", "ZeroGlitch (480 pts)"),
            Pair("ghost_cyberrunner", "CyberRunner (850 pts)")
        )

        competitors.forEach { comp ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selectedPlayerId == comp.first) CyberPrimary.copy(alpha = 0.15f) else CyberSurface)
                    .border(
                        1.dp,
                        if (selectedPlayerId == comp.first) CyberPrimary else Color.Transparent,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { selectedPlayerId = comp.first }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedPlayerId == comp.first,
                    onClick = { selectedPlayerId = comp.first },
                    colors = RadioButtonDefaults.colors(selectedColor = CyberPrimary)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = comp.second,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Click to select neural race path telemetry data stream.",
                        fontSize = 10.sp,
                        color = CyberOnSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                val selectedGhost = when (selectedPlayerId) {
                    "ghost_retro" -> com.neonrush.game.db.GhostChallengeEntity("ghost_retro", "RetroWave", 240, 2, ZoneGenerator.generateTelemetryCsv(240, 42))
                    "ghost_zeroglitch" -> com.neonrush.game.db.GhostChallengeEntity("ghost_zeroglitch", "ZeroGlitch", 480, 3, ZoneGenerator.generateTelemetryCsv(480, 84))
                    else -> com.neonrush.game.db.GhostChallengeEntity("ghost_cyberrunner", "CyberRunner", 850, 4, ZoneGenerator.generateTelemetryCsv(850, 111))
                }
                viewModel.startRacingSimulation(selectedGhost)
            },
            colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("sync_ghosts_button")
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "Launch", tint = CyberBackground)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "▶ START RUSH",
                    color = CyberBackground,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ArcadeHomeView(
    profile: GameProfile,
    viewModel: NeonRushViewModel,
    onStartRush: () -> Unit,
    onShowGhostSelection: () -> Unit,
    onNavigateToGlobal: () -> Unit,
    onNavigateToSkins: () -> Unit,
    onNavigateToSocial: () -> Unit,
    isPro: Boolean
) {
    Column(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.SpaceBetween
) {
    PromoPopup(onNavigate = { tab ->
        if (tab == "skins") onNavigateToSkins() else onNavigateToSocial()
    })

    Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "BEST SCORE: ${profile.bestScore}",
                color = CyberSecondary,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "💎 ${profile.gems} GEMS",
                color = CyberPrimary,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        val activeMutation = DailyMutations.getActiveMutation()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .border(1.dp, CyberPrimary, RoundedCornerShape(8.dp))
                .background(CyberSurface.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = activeMutation.emoji,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Column(verticalArrangement = Arrangement.Center) {
                    Text(
                        text = "ACTIVE DAILY MODIFIER: ${activeMutation.title}",
                        color = CyberPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = activeMutation.description,
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 9.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "NEON ",
                    color = Color(0xFFEC4899),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    style = LocalTextStyle.current.copy(
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = Color(0xFFEC4899).copy(alpha = 0.9f),
                            offset = androidx.compose.ui.geometry.Offset(0f, 0f),
                            blurRadius = 14f
                        )
                    )
                )
                Text(
                    text = "RUSH",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    style = LocalTextStyle.current.copy(
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = Color.White.copy(alpha = 0.9f),
                            offset = androidx.compose.ui.geometry.Offset(0f, 0f),
                            blurRadius = 14f
                        )
                    )
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "How far can you go?",
                color = Color(0xFF06B6D4),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center,
                style = LocalTextStyle.current.copy(
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = Color(0xFF06B6D4).copy(alpha = 0.9f),
                        offset = androidx.compose.ui.geometry.Offset(0f, 0f),
                        blurRadius = 8f
                    )
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.45f)
        ) {
            NeonPilotScreensaver()
        }

        if (profile.transcendenceCount > 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .border(1.dp, Color(0xFFD4AF37), RoundedCornerShape(8.dp))
                    .background(Color(0xFFD4AF37).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "👑 TRANSCENDENCE RANK ACTIVE: LVL ${profile.transcendenceCount} 👑",
                    color = Color(0xFFD4AF37),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .border(1.dp, CyberSecondary, RoundedCornerShape(8.dp))
                    .background(CyberSecondary.copy(alpha = 0.10f), RoundedCornerShape(8.dp))
                    .clickable { onShowGhostSelection() }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("👻", fontSize = 16.sp, modifier = Modifier.padding(end = 8.dp))
                        Column(verticalArrangement = Arrangement.Center) {
                            Text(
                                text = "INVICIBLE RACE",
                                color = CyberSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Race against pilot ghost telemetry trails",
                                color = CyberOnSurface.copy(alpha = 0.65f),
                                fontSize = 9.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowRight,
                        contentDescription = "Forward arrow",
                        tint = CyberSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf(
                NeonRushViewModel.DifficultyTier.EASY,
                NeonRushViewModel.DifficultyTier.MEDIUM,
                NeonRushViewModel.DifficultyTier.HARD,
                NeonRushViewModel.DifficultyTier.LEGENDARY
            ).forEach { tier ->
                val isLocked = tier == NeonRushViewModel.DifficultyTier.LEGENDARY && !isPro
                val isSelected = viewModel.selectedDifficulty == tier
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) CyberPrimary else CyberSurface)
                        .clickable {
                            if (!isLocked) {
                                viewModel.setDifficulty(tier)
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (isLocked) "\uD83D\uDD12 ${tier.label}" else tier.label,
                        color = if (isSelected) Color.Black else Color.White.copy(alpha = if (isLocked) 0.4f else 1f),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Button(
            onClick = onStartRush,
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .background(
                    Brush.horizontalGradient(listOf(Color(0xFFEC4899), Color(0xFFFF0055))),
                    RoundedCornerShape(12.dp)
                )
                .testTag("start_rush_button")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Start Rush",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "START RUSH",
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onNavigateToGlobal,
                colors = ButtonDefaults.buttonColors(containerColor = CyberSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .border(1.dp, CyberPrimary.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .testTag("quick_global_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Filled.List, contentDescription = "Global", tint = CyberPrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("GLOBAL", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Button(
                onClick = onNavigateToSkins,
                colors = ButtonDefaults.buttonColors(containerColor = CyberSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .border(1.dp, CyberSecondary.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .testTag("quick_skins_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Filled.ShoppingCart, contentDescription = "Skins", tint = CyberSecondary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("SKINS", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
@Composable
fun MissionsSection(viewModel: NeonRushViewModel, profile: GameProfile) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        MissionTierBlock("📅 DAILY MISSIONS", MissionTier.DAILY, MissionManager.currentDailyMissions(profile.dailyRerollCount), profile.dailyMissionProgressCsv, profile.dailyMissionsClaimedCsv, viewModel, profile)
        Spacer(modifier = Modifier.height(12.dp))
        MissionTierBlock("🗓️ WEEKLY MISSIONS", MissionTier.WEEKLY, MissionManager.currentWeeklyMissions(profile.weeklyRerollCount), profile.weeklyMissionProgressCsv, profile.weeklyMissionsClaimedCsv, viewModel, profile)
        Spacer(modifier = Modifier.height(12.dp))
        MissionTierBlock("🏆 MONTHLY MISSIONS", MissionTier.MONTHLY, MissionManager.currentMonthlyMissions(profile.monthlyRerollCount), profile.monthlyMissionProgressCsv, profile.monthlyMissionsClaimedCsv, viewModel, profile)
    }
}

@Composable
fun MissionTierBlock(
    title: String,
    tier: MissionTier,
    missions: List<MissionTemplate>,
    progressCsv: String,
    claimedCsv: String,
    viewModel: NeonRushViewModel,
    profile: GameProfile
) {
    val progress = remember(progressCsv) { MissionManager.parseProgressCsv(progressCsv) }
    val claimed = remember(claimedCsv) { MissionManager.parseClaimedCsv(claimedCsv) }
    val rerollCost = when (tier) {
        MissionTier.DAILY -> 10
        MissionTier.WEEKLY -> 20
        MissionTier.MONTHLY -> 30
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = CyberPrimary,
            fontFamily = FontFamily.Monospace
        )
        Button(
            onClick = { viewModel.rerollMissions(tier) },
            enabled = profile.gems >= rerollCost,
            colors = ButtonDefaults.buttonColors(containerColor = CyberSurface),
            shape = RoundedCornerShape(4.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(text = "🔄 $rerollCost💎", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
        }
    }
    Spacer(modifier = Modifier.height(6.dp))
    missions.forEach { mission ->
        val current = (progress[mission.id] ?: 0).coerceAtMost(mission.target)
        val isClaimed = claimed.contains(mission.id)
        val isComplete = current >= mission.target

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(CyberSurface)
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = mission.description, color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = current.toFloat() / mission.target.toFloat(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (isComplete) Color(0xFF4CAF50) else CyberPrimary,
                    trackColor = Color.White.copy(alpha = 0.1f)
                )
                Text(
                    text = "$current / ${mission.target}  •  💎${mission.rewardGems}",
                    color = CyberOnSurface.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            if (isClaimed) {
                Text(text = "✓", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            } else if (isComplete) {
                Button(
                    onClick = { viewModel.claimMission(tier, mission.id) },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberSecondary),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(text = "CLAIM", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                }
            }
        }
    }
}
@Composable
fun DailyChallengeTab(viewModel: NeonRushViewModel, profile: GameProfile) {
    val comments by viewModel.socialComments.collectAsState()
    var alertMsg by remember { mutableStateOf("") }

    Column(
    modifier = Modifier
        .fillMaxSize()
        .verticalScroll(androidx.compose.foundation.rememberScrollState())
) {
    MissionsSection(viewModel = viewModel, profile = profile)

    Card(
            colors = CardDefaults.cardColors(containerColor = CyberSurface),
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, CyberSecondary, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(CyberSecondary, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "ACTIVE TODAY",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Text(
                        text = "Attempts Used: ${profile.dailyAttemptsToday}/3",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = CyberPrimary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = viewModel.dailyChallengeTitle,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = viewModel.dailyChallengeDesc,
                    fontSize = 13.sp,
                    color = CyberOnSurface.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
    onClick = {
        viewModel.runDailyRushChallenge { success, attempt ->
            if (!success) {
                alertMsg = "Maximum of 3 Daily Rush attempts reached. Buy an extra attempt below, or wait till tomorrow!"
            }
        }
    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberSecondary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("daily_rush_button")
                ) {
                    Text(
                        text = "LAUNCH DAILY STORM ATTEMPT",
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (alertMsg.isNotEmpty()) {
    Text(
        text = alertMsg,
        color = CyberSecondary,
        fontSize = 11.sp,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.padding(top = 8.dp)
    )
}

if (profile.dailyAttemptsToday >= 3) {
    Spacer(modifier = Modifier.height(8.dp))
    Button(
        onClick = { viewModel.buyExtraAttempt() },
        enabled = profile.gems >= 25,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9C27B0)),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "💎 BUY EXTRA ATTEMPT (25 GEMS)",
            color = Color.White,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "💬 COMPANION PILOT CHAT",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = CyberPrimary,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = CyberSurface.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                comments.forEach { chat ->
                    Column(modifier = Modifier.padding(vertical = 6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "@" + chat.username,
                                fontWeight = FontWeight.Bold,
                                color = CyberPrimary,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = chat.timeAgo,
                                color = CyberOnSurface.copy(alpha = 0.4f),
                                fontSize = 10.sp
                            )
                        }
                        Text(
                            text = chat.comment,
                            fontSize = 12.sp,
                            color = Color.White,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                        Divider(
                            color = CyberOnSurface.copy(alpha = 0.08f),
                            thickness = 1.dp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SkinsDeckTab(viewModel: NeonRushViewModel, profile: GameProfile) {
    val unlockedSkins = remember(profile.unlockedSkinsCsv) {
        profile.unlockedSkinsCsv.split(",").toSet()
    }
    val activity = LocalContext.current as? Activity
    var selectedTab by remember { mutableStateOf("pilots") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(androidx.compose.foundation.rememberScrollState())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(CyberSurface),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(
                text = "PILOT SUITS",
                color = if (selectedTab == "pilots") CyberBackground else CyberPrimary,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .background(if (selectedTab == "pilots") CyberPrimary else Color.Transparent)
                    .clickable { selectedTab = "pilots" }
                    .padding(vertical = 10.dp)
            )
            Text(
                text = "SHIP HULLS",
                color = if (selectedTab == "ships") CyberBackground else CyberPrimary,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .background(if (selectedTab == "ships") CyberPrimary else Color.Transparent)
                    .clickable { selectedTab = "ships" }
                    .padding(vertical = 10.dp)
            )
        }
        if (selectedTab == "pilots") {
            val unlockedPilotSkins = remember(profile.unlockedPilotSkinsCsv) {
                profile.unlockedPilotSkinsCsv.split(",").toSet()
            }

            Text(
                text = "🧑‍🚀 PILOT SUITS",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = CyberPrimary,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            Text(
                text = "Suit up your pilot. Some are earned by completing Worlds, others can be bought outright.",
                color = CyberOnSurface.copy(alpha = 0.7f),
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Skins.ALL.forEach { skin ->
                val isUnlocked = unlockedPilotSkins.contains(skin.id)
                val isActive = profile.activePilotSkinId == skin.id
                val isStorySkin = skin.unlockWorldId != null

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isActive) CyberPrimary.copy(alpha = 0.15f) else CyberSurface)
                        .border(
                            1.dp,
                            if (isActive) CyberPrimary else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = skin.name,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = skin.description,
                            fontSize = 11.sp,
                            color = CyberPrimary.copy(alpha = 0.7f)
                        )
                    }

                    if (isActive) {
                        Box(
                            modifier = Modifier
                                .background(CyberPrimary, RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "ACTIVE",
                                color = CyberBackground,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    } else if (isUnlocked) {
                        Button(
                            onClick = { viewModel.equipPilotSkin(skin.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberSurface.copy(alpha = 0.8f)),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.border(1.dp, CyberPrimary, RoundedCornerShape(4.dp))
                        ) {
                            Text(text = "EQUIP", color = CyberPrimary, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        }
                    } else if (isStorySkin) {
                        Text(
                            text = "🔒 World ${skin.unlockWorldId}",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.5f),
                            fontFamily = FontFamily.Monospace
                        )
                        } else if (!skin.purchasable) {
    Text(
        text = "🏆 Mission Reward",
        fontSize = 11.sp,
        color = Color(0xFFD4AF37),
        fontFamily = FontFamily.Monospace
    )
                    } else {
                        Button(
                            onClick = {
                                activity?.let {
                                    viewModel.purchasePilotSkin(it, skin.id, "neonrush_skin_${skin.id}")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberSecondary),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(text = "BUY ${skin.priceUsd}", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        if (selectedTab == "ships") {
        Text(
            text = "🎨 SHIP CUSTOMIZATION DECK",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = CyberPrimary,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Text(
            text = "Exchange hard-earned gems for advanced cyberpunk ship hulls. Higher level metrics unlock legendary cosmetics.",
            color = CyberOnSurface.copy(alpha = 0.7f),
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .background(CyberSurface, RoundedCornerShape(8.dp))
                .border(1.dp, CyberPrimary.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "AVAILABLE BALANCE",
                color = Color.White.copy(alpha = 0.7f),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            
        Text(
                text = "💎 ${profile.gems}",
                color = CyberPrimary,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black
            )
        }

        viewModel.shopSkins.forEach { (id, name, cost) ->
            val isUnlocked = unlockedSkins.contains(id)
            val isActive = profile.activeSkinId == id

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isActive) CyberPrimary.copy(alpha = 0.15f) else CyberSurface)
                    .border(
                        1.dp,
                        if (isActive) CyberPrimary else Color.Transparent,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable {
                        if (isUnlocked) {
                            viewModel.purchaseSkin(id, 0)
                        }
                    }
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = when (id) {
                                "cyan_diamond" -> "💠"
                                "purple_square" -> "🔮"
                                "green_triangle" -> "🔺"
                                "magenta_pulse" -> "⚡"
                                "gold_transcendence" -> "🏆"
                                else -> "🪐"
                            },
                            fontSize = 18.sp,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Text(
                            text = name,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Text(
                        text = if (cost == 0) "Free Starter Hull" else "$cost GEMS Cost",
                        fontSize = 11.sp,
                        color = CyberPrimary.copy(alpha = 0.7f)
                    )
                }

                if (isActive) {
                    Box(
                        modifier = Modifier
                            .background(CyberPrimary, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "ACTIVE",
                            color = CyberBackground,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                } else if (isUnlocked) {
                    Button(
                        onClick = { viewModel.purchaseSkin(id, 0) },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberSurface.copy(alpha = 0.8f)),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.border(1.dp, CyberPrimary, RoundedCornerShape(4.dp))
                    ) {
                        Text(text = "EQUIP", color = CyberPrimary, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    }
                } else {
                    Button(
                        onClick = { viewModel.purchaseSkin(id, cost) },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberSecondary),
                        shape = RoundedCornerShape(4.dp),
                        enabled = profile.gems >= cost,
                        modifier = Modifier.testTag("buy_skin_button")
                    ) {
                        Text(text = "BUY 💎$cost", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    }
                }
            }
        }
        }

        Spacer(modifier = Modifier.height(20.dp))
        
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberSurface.copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CyberTertiary, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "🌌 TRANSCENDENCE PROTOCOL",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyberTertiary,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Text(
                    text = "Exchange 100+ points of highscore data for instant Transcendence level ranks and a +150 bonus gem payout. Relive the cyber alley!",
                    fontSize = 11.sp,
                    color = CyberOnSurface.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { viewModel.triggerTranscendence() },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberTertiary),
                    shape = RoundedCornerShape(6.dp),
                    enabled = profile.bestScore >= 100,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("transcendence_button")
                ) {
                    Text(
                        text = if (profile.bestScore >= 100) "INITIATE TRANSCENDENCE" else "REQUIRES 100+ SCORE",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    

        
Spacer(modifier = Modifier.height(20.dp))
        if (!profile.adsRemoved) {
    Text(
        text = "🚫 REMOVE ADS",
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = CyberPrimary,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.padding(bottom = 6.dp)
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(CyberSurface)
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Remove all ads",
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "One-time purchase, forever",
                color = CyberOnSurface.copy(alpha = 0.65f),
                fontSize = 11.sp
            )
        }
        Button(
            onClick = {
                activity?.let { viewModel.purchaseRemoveAds(it) }
            },
            colors = ButtonDefaults.buttonColors(containerColor = CyberSecondary),
            shape = RoundedCornerShape(4.dp)
        ) {
            Text(text = "BUY ${RevenueCatManager.REMOVE_ADS_PRICE_USD}", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
}

Text(
    text = "⚡ STARTER PACK",
    fontSize = 16.sp,
    fontWeight = FontWeight.Bold,
    color = CyberPrimary,
    fontFamily = FontFamily.Monospace,
    modifier = Modifier.padding(bottom = 6.dp)
)
Row(
    modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(CyberSurface)
        .padding(14.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
) {
    Column {
        Text(
            text = "💎 ${RevenueCatManager.STARTER_PACK_GEMS_AMOUNT} Gems",
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "Limited-time offer for new pilots",
            color = CyberOnSurface.copy(alpha = 0.65f),
            fontSize = 11.sp
        )
    }
    Button(
        onClick = {
            activity?.let { viewModel.purchaseStarterPack(it) }
        },
        colors = ButtonDefaults.buttonColors(containerColor = CyberSecondary),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(text = "BUY ${RevenueCatManager.STARTER_PACK_PRICE_USD}", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
    }
}
Spacer(modifier = Modifier.height(16.dp))

Text(
    text = "💎 GET MORE GEMS",
    fontSize = 16.sp,    fontWeight = FontWeight.Bold,
    color = CyberPrimary,
    fontFamily = FontFamily.Monospace,
    modifier = Modifier.padding(bottom = 6.dp)
)

        

        Text(
            text = "Skip the grind — top up your gem balance directly.",
            color = CyberOnSurface.copy(alpha = 0.7f),
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        listOf(
            Triple(RevenueCatManager.PRODUCT_ID_GEMS_SMALL, RevenueCatManager.GEMS_SMALL_AMOUNT, RevenueCatManager.GEMS_SMALL_PRICE_USD),
            Triple(RevenueCatManager.PRODUCT_ID_GEMS_MEDIUM, RevenueCatManager.GEMS_MEDIUM_AMOUNT, RevenueCatManager.GEMS_MEDIUM_PRICE_USD),
            Triple(RevenueCatManager.PRODUCT_ID_GEMS_LARGE, RevenueCatManager.GEMS_LARGE_AMOUNT, RevenueCatManager.GEMS_LARGE_PRICE_USD)
        ).forEach { (productId, amount, price) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(CyberSurface)
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "💎 $amount Gems",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Button(
                    onClick = {
                        activity?.let { viewModel.purchaseGemPack(it, productId, amount) }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberSecondary),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(text = "BUY $price", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun RacingSimulatorScreen(
    simState: SimulationState,
    viewModel: NeonRushViewModel,
    isPro: Boolean,
    profile: GameProfile,
    onShowPaywall: () -> Unit
) {
    var controlOffset by remember { mutableStateOf(50f) }
    var previousUserYPos by remember { mutableStateOf(simState.userYPos) }
    val tiltAngle = (simState.userYPos - previousUserYPos).toFloat().coerceIn(-10f, 10f) * 1.8f
    SideEffect { previousUserYPos = simState.userYPos }

    // Default running frames (used when the equipped skin has no custom frames yet)
    val pf1 = ImageBitmap.imageResource(id = R.drawable.pilot_run_1)
    val pf2 = ImageBitmap.imageResource(id = R.drawable.pilot_run_2)
    val pf3 = ImageBitmap.imageResource(id = R.drawable.pilot_run_3)
    val pf4 = ImageBitmap.imageResource(id = R.drawable.pilot_run_4)
    val pf5 = ImageBitmap.imageResource(id = R.drawable.pilot_run_5)
    val pf6 = ImageBitmap.imageResource(id = R.drawable.pilot_run_6)
    val defaultFrameIds = listOf(pf1, pf2, pf3, pf4, pf5, pf6)

    // Look up the equipped skin's custom running frames, if it has any
    val equippedSkin = remember(profile.activePilotSkinId) {
        Skins.ALL.find { it.id == profile.activePilotSkinId }
    }
    val overrideIds = equippedSkin?.pilotFrameOverrides
    val pilotFrames = if (overrideIds != null && overrideIds.size == 6) {
        overrideIds.map { resId -> ImageBitmap.imageResource(id = resId) }
    } else {
        defaultFrameIds
    }
        
    val gemImg = ImageBitmap.imageResource(id = R.drawable.gem)
    val coinImg = ImageBitmap.imageResource(id = R.drawable.coin)
    val spikesImg = ImageBitmap.imageResource(id = R.drawable.spikes)
    val laserImg = ImageBitmap.imageResource(id = R.drawable.laser1)
    val sawbladeImg = ImageBitmap.imageResource(id = R.drawable.sawblade)
    val droneImg = ImageBitmap.imageResource(id = R.drawable.drone)
    val spikesFlippedImg = ImageBitmap.imageResource(id = R.drawable.spikes_flipped)
    val barrierImg = ImageBitmap.imageResource(id = R.drawable.obstacle_barrier)
    val zapFieldImg = ImageBitmap.imageResource(id = R.drawable.obstacle_zap_field)
    val phantomImg = ImageBitmap.imageResource(id = R.drawable.obstacle_phantom)
    val splitterImg = ImageBitmap.imageResource(id = R.drawable.obstacle_splitter_v2)
    val tunnelTopImg = ImageBitmap.imageResource(id = R.drawable.obstacle_tunnel_top)
    val tunnelBottomImg = ImageBitmap.imageResource(id = R.drawable.obstacle_tunnel_bottom)
    val standardImg = ImageBitmap.imageResource(id = R.drawable.obstacle_standard)
    
    val textMeasurer = rememberTextMeasurer()
    var previousScoreForPopups by remember { mutableStateOf(simState.score) }
    val scorePopups = remember { mutableStateListOf<Triple<Int, Int,Int>>() }
    val scoreDelta = simState.score - previousScoreForPopups
    if (scoreDelta > 0) {
        scorePopups.add(Triple(scoreDelta, simState.tickIndex, simState.userYPos))
    }
    previousScoreForPopups = simState.score
    scorePopups.removeAll { (_, spawnTick, _) -> simState.tickIndex - spawnTick > 25 }

    var previousShakeMagnitude by remember { mutableStateOf(0f) }
    var flashStartTick by remember { mutableStateOf(-100) }
    val currentShakeMagnitude = kotlin.math.abs(simState.screenShakeX) + kotlin.math.abs(simState.screenShakeY)
    if (currentShakeMagnitude > 3f && previousShakeMagnitude <= 3f) {
        flashStartTick = simState.tickIndex
    }
    previousShakeMagnitude = currentShakeMagnitude
    
    Box(modifier = Modifier.fillMaxSize()) {
        val currentWorld by viewModel.currentWorld.collectAsState()
        LaunchedEffect(currentWorld.id) {
            if (currentWorld.requiresPro && !isPro) {
                viewModel.triggerPaywallTeaser(currentWorld)
                delay(2500)
                onShowPaywall()
            }
        }
        when (currentWorld.id) {
            1 -> Image(
                painter = painterResource(id = R.drawable.bg_world1_blackout_front),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            2 -> Image(
                painter = painterResource(id = R.drawable.bg_world2_derelict_signal),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            3 -> Image(
                painter = painterResource(id = R.drawable.bg_world3_cell_block_zero),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            4 -> Image(
                painter = painterResource(id = R.drawable.bg_world4_green_hell),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            5 -> Image(
                painter = painterResource(id = R.drawable.bg_world5_red_protocol),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            else -> {}
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Telemetry top panel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CyberSurface, RoundedCornerShape(8.dp))
                    .border(1.dp, CyberPrimary.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "STORM ZONE: " + simState.currentZoneName,
                        color = CyberPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "${simState.distanceMeters.toInt()}m",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${simState.speedKmh} KM/H",
                        color = CyberSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "${simState.score} PTS",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF020104).copy(alpha = 0.25f))
                    .border(2.dp, CyberPrimary, RoundedCornerShape(12.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val deltaPercent = (dragAmount.y / size.height) * 100f
                                viewModel.adjustUserY(deltaPercent.toInt())
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cw = size.width
                        val ch = size.height

                        drawContext.canvas.translate(simState.screenShakeX, simState.screenShakeY)

                        val gridLines = 7
                        for (i in 1..gridLines) {
                            val x = cw * i / (gridLines + 1)
                            drawLine(
                                color = CyberPrimary.copy(alpha = 0.08f),
                                start = Offset(x, 0f),
                                end = Offset(x, ch),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        val path = Path().apply {
                            moveTo(0f, ch * 0.5f)
                            val ticksSize = simState.ghostYPath.size
                            for (i in 0 until ticksSize) {
                                val posX = cw * i / (ticksSize - 1).coerceAtLeast(1)
                                val posY = ch * (simState.ghostYPath[i] / 100f)
                                lineTo(posX, posY)
                            }
                        }
                        drawPath(
                            path = path,
                            color = CyberPrimary.copy(alpha = 0.15f),
                            style = Stroke(width = 2.dp.toPx())
                        )

                        for (elem in simState.activeTrackElements) {
                            val x = cw * elem.xOffsetFraction
                            val y = ch * (elem.yMatchPos / 100f)
                            
                            when (elem.type) {
                                "gem" -> {
                                    val pulse = 1f + 0.15f * sin(simState.tickIndex * 0.3f)
                                    val baseSize = ch * 0.06f
                                    val w = baseSize * (gemImg.width.toFloat() / gemImg.height.toFloat()) * pulse
                                    val h = baseSize * pulse
                                    drawImage(
                                        image = gemImg,
                                        dstOffset = IntOffset((x - w / 2f).roundToInt(), (y - h / 2f).roundToInt()),
                                        dstSize = IntSize(w.roundToInt(), h.roundToInt())
                                    )
                                }
                                "fuel" -> {
                                    val angle = (simState.tickIndex * 6f) % 360f
                                    val baseSize = ch * 0.055f
                                    val w = baseSize * (coinImg.width.toFloat() / coinImg.height.toFloat())
                                    rotate(degrees = angle, pivot = Offset(x, y)) {
                                        drawImage(
                                            image = coinImg,
                                            dstOffset = IntOffset((x - w / 2f).roundToInt(), (y - baseSize / 2f).roundToInt()),
                                            dstSize = IntSize(w.roundToInt(), baseSize.roundToInt())
                                        )
                                    }
                                }
                                "powerup" -> {
                                    drawCircle(color = Color(0xFF8338EC), radius = 9.dp.toPx(), center = Offset(x, y))
                                    drawCircle(color = Color.White, radius = 7.dp.toPx(), center = Offset(x, y), style = Stroke(1.dp.toPx()))
                                }
                                "obstacle" -> {
                                    val obsColor = Color(0xFFFF0055)
                                    when (elem.subType) {
                                        "PILLAR_TOP" -> {
                                            drawRect(
                                                color = obsColor,
                                                topLeft = Offset(x - cw * 0.025f, 0f),
                                                size = Size(cw * 0.05f, y)
                                            )
                                            drawRect(
                                                color = Color.White.copy(alpha = 0.4f),
                                                topLeft = Offset(x - cw * 0.025f, 0f),
                                                size = Size(cw * 0.05f, y),
                                                style = Stroke(1.dp.toPx())
                                            )
                                        }
                                        "PILLAR_BOTTOM" -> {
                                            drawRect(
                                                color = obsColor,
                                                topLeft = Offset(x - cw * 0.025f, y),
                                                size = Size(cw * 0.05f, ch - y)
                                            )
                                            drawRect(
                                                color = Color.White.copy(alpha = 0.4f),
                                                topLeft = Offset(x - cw * 0.025f, y),
                                                size = Size(cw * 0.05f, ch - y),
                                                style = Stroke(1.dp.toPx())
                                            )
                                        }
                                        "STALACTITE" -> {
                                            val baseSize = ch * 0.09f
                                            val w = baseSize * (spikesFlippedImg.width.toFloat() / spikesFlippedImg.height.toFloat())
                                            val glowPulse = 0.7f + 0.3f * sin(simState.tickIndex * 0.4f)
                                            drawImage(
                                                image = spikesFlippedImg,
                                                dstOffset = IntOffset((x - w / 2f).roundToInt(), (y - baseSize / 2f).roundToInt()),
                                                dstSize = IntSize(w.roundToInt(), baseSize.roundToInt()),
                                                alpha = glowPulse
                                            )
                                        }
                                        "STALAGMITE" -> {
                                            val baseSize = ch * 0.09f
                                            val w = baseSize * (spikesImg.width.toFloat() / spikesImg.height.toFloat())
                                            val glowPulse = 0.7f + 0.3f * sin(simState.tickIndex * 0.4f)
                                            drawImage(
                                                image = spikesImg,
                                                dstOffset = IntOffset((x - w / 2f).roundToInt(), (y - baseSize / 2f).roundToInt()),
                                                dstSize = IntSize(w.roundToInt(), baseSize.roundToInt()),
                                                alpha = glowPulse
                                            )
                                        }
                                        "LASER" -> {
                                            val glowPulse = 0.6f + 0.4f * sin(simState.tickIndex * 0.5f)
                                            val h = ch * 0.22f
                                            val w = h * (laserImg.width.toFloat() / laserImg.height.toFloat())
                                            drawImage(
                                                image = laserImg,
                                                dstOffset = IntOffset((x - w / 2f).roundToInt(), (y - h / 2f).roundToInt()),
                                                dstSize = IntSize(w.roundToInt(), h.roundToInt()),
                                                alpha = glowPulse
                                            )
                                        }
                                        "BLADE" -> {
                                            val angle = (simState.tickIndex * 12f) % 360f
                                            val size = ch * 0.08f
                                            rotate(degrees = angle, pivot = Offset(x, y)) {
                                                drawImage(
                                                    image = sawbladeImg,
                                                    dstOffset = IntOffset((x - size / 2f).roundToInt(), (y - size / 2f).roundToInt()),
                                                    dstSize = IntSize(size.roundToInt(), size.roundToInt())
                                                )
                                            }
                                        }
                                        "BARRIER" -> {
                                            val baseSize = ch * 0.16f
                                            val w = baseSize * (barrierImg.width.toFloat() / barrierImg.height.toFloat())
                                            drawImage(
                                                image = barrierImg,
                                                dstOffset = IntOffset((x - w / 2f).roundToInt(), (y - baseSize / 2f).roundToInt()),
                                                dstSize = IntSize(w.roundToInt(), baseSize.roundToInt())
                                            )
                                        }
                                        "ZAP_FIELD" -> {
                                            val glowPulse = 0.6f + 0.4f * sin(simState.tickIndex * 0.5f)
                                            val baseSize = ch * 0.15f
                                            val w = baseSize * (zapFieldImg.width.toFloat() / zapFieldImg.height.toFloat())
                                            drawImage(
                                                image = zapFieldImg,
                                                dstOffset = IntOffset((x - w / 2f).roundToInt(), (y - baseSize / 2f).roundToInt()),
                                                dstSize = IntSize(w.roundToInt(), baseSize.roundToInt()),
                                                alpha = glowPulse
                                            )
                                        }
                                        "PHANTOM" -> {
                                            val bob = sin(simState.tickIndex * 0.2f) * ch * 0.015f
                                            val flicker = 0.5f + 0.5f * sin(simState.tickIndex * 0.3f)
                                            val baseSize = ch * 0.16f
                                            val w = baseSize * (phantomImg.width.toFloat() / phantomImg.height.toFloat())
                                            drawImage(
                                                image = phantomImg,
                                                dstOffset = IntOffset((x - w / 2f).roundToInt(), (y - baseSize / 2f + bob).roundToInt()),
                                                dstSize = IntSize(w.roundToInt(), baseSize.roundToInt()),
                                                alpha = flicker
                                            )
                                        }
                                        "SPLITTER" -> {
                                            val angle = (simState.tickIndex * 8f) % 360f
                                            val baseSize = ch * 0.13f
                                            val w = baseSize * (splitterImg.width.toFloat() / splitterImg.height.toFloat())
                                            rotate(degrees = angle, pivot = Offset(x, y)) {
                                                drawImage(
                                                    image = splitterImg,
                                                    dstOffset = IntOffset((x - w / 2f).roundToInt(), (y - baseSize / 2f).roundToInt()),
                                                    dstSize = IntSize(w.roundToInt(), baseSize.roundToInt())
                                                )
                                            }
                                        }
                                        "TUNNEL_TOP" -> {
                                            val baseSize = ch * 0.18f
                                            val w = baseSize * (tunnelTopImg.width.toFloat() / tunnelTopImg.height.toFloat())
                                            drawImage(
                                                image = tunnelTopImg,
                                                dstOffset = IntOffset((x - w / 2f).roundToInt(), 0),
                                                dstSize = IntSize(w.roundToInt(), baseSize.roundToInt())
                                            )
                                        }
                                        "TUNNEL_BOTTOM" -> {
                                            val baseSize = ch * 0.18f
                                            val w = baseSize * (tunnelBottomImg.width.toFloat() / tunnelBottomImg.height.toFloat())
                                            drawImage(
                                                image = tunnelBottomImg,
                                                dstOffset = IntOffset((x - w / 2f).roundToInt(), (ch - baseSize).roundToInt()),
                                                dstSize = IntSize(w.roundToInt(), baseSize.roundToInt())
                                            )
                                        }
                                        else -> {
                                            val bob = sin(simState.tickIndex * 0.2f) * ch * 0.015f
                                            val baseSize = ch * 0.14f
                                            val w = baseSize * (standardImg.width.toFloat() / standardImg.height.toFloat())
                                            drawImage(
                                                image = standardImg,
                                                dstOffset = IntOffset((x - w / 2f).roundToInt(), (y - baseSize / 2f + bob).roundToInt()),
                                                dstSize = IntSize(w.roundToInt(), baseSize.roundToInt())
                                            )
                                        }
                                    }
                                }
                                "bullet" -> {
                                    drawCircle(color = Color(0xFFFF00FF), radius = 3.dp.toPx(), center = Offset(x, y))
                                }
                            }
                        }

                        for (p in simState.particles) {
                            val px = cw * p.x
                            val py = ch * (p.y / 100f)
                            val lifeFrac = (1f - (p.age.toFloat() / p.maxAge.toFloat())).coerceIn(0f, 1f)
                            val particleColor = Color(p.colorArgb).copy(alpha = lifeFrac)
                            val radius = if (p.kind == "explosion") (3f + 4f * lifeFrac).dp.toPx() else (2f + 2f * lifeFrac).dp.toPx()
                            drawCircle(
                                color = particleColor,
                                radius = radius,
                                center = Offset(px, py)
                            )
                        }

                        val ticksCount = simState.ghostYPath.size
                        val userX = cw * 0.2f
                        
                        if (ticksCount > 0) {
                            val ghostY = ch * (simState.ghostYPos / 100f)
                            val ghostX = cw * 0.2f

                            drawCircle(
                                color = CyberSecondary.copy(alpha = 0.5f),
                                radius = 8.dp.toPx(),
                                center = Offset(ghostX, ghostY)
                            )
                        }

                        val userY = ch * (simState.userYPos / 100f)

                        if (simState.activePowerupDurations.containsKey("PU1")) {
                            drawCircle(
                                color = Color(0xFF3A86FF),
                                radius = 26.dp.toPx(),
                                center = Offset(userX, userY),
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                        if (simState.activePowerupDurations.containsKey("PU7")) {
                            drawCircle(
                                color = Color(0xFFF72585),
                                radius = 28.dp.toPx(),
                                center = Offset(userX, userY),
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }

                        val frameIdx = (simState.tickIndex / 3) % pilotFrames.size
                        val currentFrameImg = pilotFrames[frameIdx]

                        val displayHeight = ch * 0.20f
                        val aspect = currentFrameImg.width.toFloat() / currentFrameImg.height.toFloat()
                        val displayWidth = displayHeight * aspect

                        rotate(degrees = tiltAngle, pivot = Offset(userX, userY)) {
                            drawImage(
                                image = currentFrameImg,
                                dstOffset = IntOffset((userX - displayWidth / 2f).roundToInt(), (userY - displayHeight / 2f).roundToInt()),
                                dstSize = IntSize(displayWidth.roundToInt(), displayHeight.roundToInt())
                            )
                        }

                        for ((scoreDelta, spawnTick, spawnY) in scorePopups) {
                            val popupAge = simState.tickIndex - spawnTick
                            val popupAlpha = 1f - (popupAge / 25f)
                            val popupY = ch * (spawnY / 100f) - (popupAge * 2f)
                            val textLayout = textMeasurer.measure(
                                "+$scoreDelta",
                                style = TextStyle(
                                    color = CyberPrimary.copy(alpha = popupAlpha),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            drawText(
                                textLayoutResult = textLayout,
                                topLeft = Offset(userX + 20f, popupY)
                            )
                        }

                        if (simState.tickIndex - flashStartTick < 5) {
                            drawRect(
                                color = Color.White.copy(alpha = 0.3f),
                                size = Size(cw, ch)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (!isPro) {
                AdMobBannerView(
                    adUnitId = "ca-app-pub-3841327492203214/6533049489",
                    modifier = Modifier.fillMaxWidth()
                )
            }

                        Spacer(modifier = Modifier.height(8.dp))

            Row(
    modifier = Modifier
        .fillMaxWidth()
        .height(48.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
) {
    Button(
        onClick = { viewModel.resetSimulation() },
        colors = ButtonDefaults.buttonColors(containerColor = CyberSurface),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .weight(0.3f)
            .fillMaxHeight()
            .border(1.dp, CyberPrimary.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
    ) {
        Text("QUIT", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }

    FuelBar(
    fuelPercent = simState.fuelLevelPercent,
    refillCount = simState.fuelRefillCount,
    isPro = isPro,
    cost = viewModel.fuelRefillCostForCurrentRun(),
    onRefuel = { viewModel.refuelWithGems(isPro) },
    onFuelTierChanged = { tier -> viewModel.onFuelTierChanged(tier) },
    modifier = Modifier
        .weight(0.7f)
        .fillMaxHeight()
)
}
}
}
}
 @Composable
fun GameOverOverlayScreen(
    simState: SimulationState,
    viewModel: NeonRushViewModel,
    isPro: Boolean,
    profile: GameProfile,
    onShowPaywall: () -> Unit
) {
    val activity = LocalContext.current as? Activity
    val revivesExhausted = simState.reviveCount >= 2
    var showSummary by remember(simState.reviveCount) { mutableStateOf(revivesExhausted) }
    var secondsLeft by remember(simState.reviveCount) { mutableStateOf(5) }

    LaunchedEffect(simState.reviveCount, revivesExhausted) {
        if (!revivesExhausted) {
            secondsLeft = 5
            while (secondsLeft > 0) {
                delay(1000)
                secondsLeft--
            }
            showSummary = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE6000000))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        if (!showSummary) {
            // ---------- SCREEN 1: Crash / Revive ----------
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "SYSTEM FAILURE",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = CyberPrimary,
                    fontFamily = FontFamily.Monospace
                )

                Text(
                    text = "SCORE: ${simState.score}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace
                )

                Box(
    modifier = Modifier.size(56.dp),
    contentAlignment = Alignment.Center
) {
    CircularProgressIndicator(
        progress = secondsLeft / 5f,
        modifier = Modifier.fillMaxSize(),
        color = CyberPrimary,
        trackColor = CyberSurface,
        strokeWidth = 4.dp
    )
    Text(
        text = "$secondsLeft",
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        color = CyberPrimary,
        fontFamily = FontFamily.Monospace
    )
}

                if (!isPro) {
    Button(
        onClick = {
            activity?.let {
                AdMobManager.showRewardedIfReady(it) {
                    viewModel.reviveSimulation()
                    viewModel.recordAdWatched()
                }
            }
        },
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "🎬 WATCH AD TO REVIVE",
            color = Color.White,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}

                Button(
                    onClick = { viewModel.reviveWithGems() },
                    enabled = profile.gems >= viewModel.reviveCostForCurrentRun(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9C27B0)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "💎 REVIVE FOR ${viewModel.reviveCostForCurrentRun()} GEMS",
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = { showSummary = true },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberSurface),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CyberPrimary.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                ) {
                    Text(
                        text = "QUIT",
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            // ---------- SCREEN 2: Final Summary ----------
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "GAME OVER",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = CyberPrimary,
                    fontFamily = FontFamily.Monospace
                )

                Text(
                    text = "FINAL SCORE: ${simState.score}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace
                )

                Text(
                    text = "DISTANCE: ${simState.distanceMeters.toInt()}m",
                    fontSize = 14.sp,
                    color = CyberSecondary,
                    fontFamily = FontFamily.Monospace
                )

                Text(
                    text = "ZONE REACHED: ${simState.currentZoneName}",
                    fontSize = 14.sp,
                    color = CyberSecondary,
                    fontFamily = FontFamily.Monospace
                )

                if (!isPro) {
    Button(
        onClick = {
            activity?.let {
                AdMobManager.showRewardedIfReady(it) {
                    viewModel.doubleGemsForRun()
                    viewModel.recordAdWatched()
                }
            }
        },
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC107)),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "🎬 DOUBLE GEMS (${simState.collectedGemsCount * 2} 💎)",
            color = Color.Black,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}

                if (!isPro) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CyberSurface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, CyberPrimary, RoundedCornerShape(12.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "⚡ GO PRO",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberPrimary,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Remove ads, unlock Legendary difficulty, and access all Worlds!",
                                fontSize = 12.sp,
                                color = CyberOnSurface.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = onShowPaywall,
                                colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "UPGRADE TO PRO",
                                    color = CyberBackground,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        if (!profile.adsRemoved && AdMobManager.isInterstitialDue()) {
                            activity?.let {
                                AdMobManager.showInterstitialIfReady(it) {
                                    viewModel.resetSimulation()
                                }
                            }
                        } else {
                            viewModel.resetSimulation()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberSurface),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CyberPrimary.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                ) {
                    Text(
                        text = "BACK TO MENU",
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
    }



@Composable
fun ProfileTab(profile: GameProfile, viewModel: NeonRushViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        HeaderProfileDeck(profile = profile, viewModel = viewModel)

        Card(
            colors = CardDefaults.cardColors(containerColor = CyberSurface),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CyberPrimary.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "📊 STATISTICS",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyberPrimary,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                StatRow("Total Runs", "${profile.totalRuns}")
                StatRow("Average Score", "${profile.averageScore}")
                StatRow("Total Gems Earned", "${profile.totalGemsEarned}")
                StatRow("Favorite Skin", profile.activeSkinId.replace("_", " ").capitalize())
                StatRow("Transcendence Level", "${profile.transcendenceCount}")
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = CyberSurface),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CyberSecondary.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "⚙️ SETTINGS",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyberSecondary,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                var soundEnabled by remember { mutableStateOf(true) }
                var musicEnabled by remember { mutableStateOf(true) }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Sound Effects", color = Color.White, fontFamily = FontFamily.Monospace)
                    Switch(
                        checked = soundEnabled,
                        onCheckedChange = { soundEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyberPrimary,
                            checkedTrackColor = CyberPrimary.copy(alpha = 0.5f)
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Music", color = Color.White, fontFamily = FontFamily.Monospace)
                    Switch(
                        checked = musicEnabled,
                        onCheckedChange = { musicEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyberPrimary,
                            checkedTrackColor = CyberPrimary.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        }

        Button(
            onClick = { RevenueCatManager.restorePurchases {} },
            colors = ButtonDefaults.buttonColors(containerColor = CyberSurface),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CyberPrimary.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
        ) {
            Text(
                text = "RESTORE PURCHASES",
                color = CyberPrimary,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = CyberOnSurface.copy(alpha = 0.7f),
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
        )
        Text(
            text = value,
            color = Color.White,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
    }
}

@Composable
fun PaywallDialog(onDismiss: () -> Unit, reason: String) {
    val activity = LocalContext.current as? Activity

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "⚡ NEON RUSH PRO",
                color = CyberPrimary,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = when (reason) {
                        "world4" -> "World 4: Green Hell requires Pro subscription. Unlock all Worlds, remove ads, and get Legendary difficulty!"
                        else -> "Upgrade to Pro to unlock all features: remove ads, access all Worlds, Legendary difficulty, and exclusive skins!"
                    },
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Monthly: ${RevenueCatManager.SUBSCRIPTION_PRICE_MONTHLY_USD}",
                    color = CyberSecondary,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Annual: ${RevenueCatManager.SUBSCRIPTION_PRICE_ANNUAL_USD} (Save 50%)",
                    color = CyberSecondary,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    activity?.let {
                        RevenueCatManager.purchaseProSubscription(it) { success ->
                            if (success) onDismiss()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary)
            ) {
                Text("SUBSCRIBE", color = CyberBackground, fontFamily = FontFamily.Monospace)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("MAYBE LATER", color = CyberOnSurface, fontFamily = FontFamily.Monospace)
            }
        },
        containerColor = CyberSurface,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun NeonPilotScreensaver() {
    val infiniteTransition = rememberInfiniteTransition(label = "screensaver")
    val offsetX by infiniteTransition.animateFloat(
        initialValue = -50f,
        targetValue = 50f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetX"
    )
    val offsetY by infiniteTransition.animateFloat(
        initialValue = -20f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetY"
    )
    // Load the same running-pilot animation frames used in the real game
val pf1 = ImageBitmap.imageResource(id = R.drawable.pilot_run_1)
val pf2 = ImageBitmap.imageResource(id = R.drawable.pilot_run_2)
val pf3 = ImageBitmap.imageResource(id = R.drawable.pilot_run_3)
val pf4 = ImageBitmap.imageResource(id = R.drawable.pilot_run_4)
val pf5 = ImageBitmap.imageResource(id = R.drawable.pilot_run_5)
val pf6 = ImageBitmap.imageResource(id = R.drawable.pilot_run_6)
val pilotFrames = remember(pf1, pf2, pf3, pf4, pf5, pf6) {
    listOf(pf1, pf2, pf3, pf4, pf5, pf6)
}

// Drives the run-cycle animation, independent of offsetX/offsetY drifting
var frameTick by remember { mutableStateOf(0) }
LaunchedEffect(Unit) {
    while (true) {
        delay(80)
        frameTick++
    }
}
val frameIdx = frameTick % pilotFrames.size
val currentFrameImg = pilotFrames[frameIdx]
   // Continuously scrolling background — sells the sense of forward motion
val scrollX by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 1000f,
    animationSpec = infiniteRepeatable(
        animation = tween(6000, easing = LinearEasing),
        repeatMode = RepeatMode.Restart
    ),
    label = "scrollX"
) 


    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cw = size.width
            val ch = size.height

           // Draw scrolling stars, two layers for a parallax depth effect
for (i in 0..50) {
    val baseX = (i * 73 % 100) / 100f * cw
    val y = (i * 37 % 100) / 100f * ch
    val alpha = 0.3f + 0.7f * ((i * 13 % 10) / 10f)
    val speed = 0.4f + (i % 3) * 0.3f // varying speeds = parallax depth
    val x = (baseX - scrollX * speed).mod(cw)
    drawCircle(
        color = Color.White.copy(alpha = alpha),
        radius = (1f + (i % 3)).dp.toPx(),
        center = Offset(x, y)
    )
}

// Scrolling speed-lines to sell velocity
for (i in 0..8) {
    val baseX = (i * 137 % 100) / 100f * cw
    val y = ch * 0.15f + (i * 91 % 100) / 100f * ch * 0.7f
    val lineSpeed = 1.6f
    val x = (baseX - scrollX * lineSpeed).mod(cw)
    drawLine(
        color = CyberPrimary.copy(alpha = 0.25f),
        start = Offset(x, y),
        end = Offset(x + 24f, y),
        strokeWidth = 2.dp.toPx()
    )
} 

            // Draw pilot silhouette
            val pilotX = cw / 2f + offsetX
            val pilotY = ch / 2f + offsetY

            drawCircle(
                color = CyberPrimary.copy(alpha = 0.3f),
                radius = 40.dp.toPx(),
                center = Offset(pilotX, pilotY)
            )

            // Draw trail
            val trailPath = Path().apply {
                moveTo(pilotX - 60, pilotY)
                for (i in 1..5) {
                    val tx = pilotX - 60 - i * 30
                    val ty = pilotY + kotlin.math.sin(i * 0.5f) * 10
                    lineTo(tx, ty)
                }
            }
            drawPath(
                path = trailPath,
                color = CyberPrimary.copy(alpha = 0.5f),
                style = Stroke(width = 3.dp.toPx())
            )
        }

        val displayHeight = 90.dp
        val aspect = currentFrameImg.width.toFloat() / currentFrameImg.height.toFloat()
        val displayWidth = displayHeight * aspect

        Image(
            bitmap = currentFrameImg,
            contentDescription = "Pilot running",
            modifier = Modifier
                .size(width = displayWidth, height = displayHeight)
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
        )
    }
}

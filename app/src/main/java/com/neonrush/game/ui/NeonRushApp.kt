package com.neonrush.game.ui

import android.app.Activity
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.neonrush.game.AdMobBannerView
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
LaunchedEffect(Unit) {
        viewModel.checkDailyStreak()
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
                        AdMobBannerView()
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
                // Interactive Space Racetrack Simulator Screen!
                RacingSimulatorScreen(
    simState = simState,
    viewModel = viewModel,
    isPro = isPro,
    onShowPaywall = { showPaywall = true; paywallReason = "world4" }
)
            } else if (simState.isStarted && simState.isCompleted) {
                // Dynamic Game Over overlay screen with reward, revive, subscription access point!
                GameOverOverlayScreen(
                    simState = simState,
                    viewModel = viewModel,
                    isPro = isPro,
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

    // Display billing Subscription Paywall when requested
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
            text = "Follow and challenge ghost trails of active pilots sync\'d from the host.",
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
                            // Gold, Silver, Bronze Badge colors
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
                // Find ghost by selected ID or default to CyberRunner
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
    isPro: Boolean
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Row 1 - Header bar: height 48dp
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

        // Row 2 - Daily Mutation banner: height 44dp, cyan border, visible
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

        // Row 3 - NEON RUSH title: height 56dp, stylized glow with tagline below
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

        // Row 4 - SCREENSAVER CANVAS: fillMaxWidth, height 45% of screen
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.45f)
        ) {
            NeonPilotScreensaver()
        }

        // Row 5 - TRANSCENDENCE banner (if available): height 44dp, gold
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
        
        // Difficulty selector row
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

        // Row 6 — START RUSH button: height 52dp, full width, pink gradient
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

        // Row 7 — GLOBAL + SKINS quick buttons: height 44dp, side by side
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
fun DailyChallengeTab(viewModel: NeonRushViewModel, profile: GameProfile) {
    val comments by viewModel.socialComments.collectAsState()
    var alertMsg by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(androidx.compose.foundation.rememberScrollState())
    ) {
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
                                alertMsg = "Maximum of 3 Daily Rush attempts reached. Play regular Ghost trails or wait till tomorrow!"
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
                            viewModel.purchaseSkin(id, 0) // Equips skin
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
        // Transcendence Prestige Container
        
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

        Text(
            text = "💎 GET MORE GEMS",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
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
    onShowPaywall: () -> Unit
) {  var controlOffset by remember { mutableStateOf(50f) }
    var previousUserYPos by remember { mutableStateOf(simState.userYPos) }
    val tiltAngle = (simState.userYPos - previousUserYPos).toFloat().coerceIn(-10f, 10f) * 1.8f
    SideEffect { previousUserYPos = simState.userYPos }

    val pf1 = ImageBitmap.imageResource(id = R.drawable.pilot_run_1)
    val pf2 = ImageBitmap.imageResource(id = R.drawable.pilot_run_2)
    val pf3 = ImageBitmap.imageResource(id = R.drawable.pilot_run_3)
    val pf4 = ImageBitmap.imageResource(id = R.drawable.pilot_run_4)
    val pf5 = ImageBitmap.imageResource(id = R.drawable.pilot_run_5)
    val pf6 = ImageBitmap.imageResource(id = R.drawable.pilot_run_6)
    val pilotFrames = remember(pf1, pf2, pf3, pf4, pf5, pf6) {
        listOf(pf1, pf2, pf3, pf4, pf5, pf6)
    }  
        
    val gemImg = ImageBitmap.imageResource(id = R.drawable.gem)
    val coinImg = ImageBitmap.imageResource(id = R.drawable.coin)
    val spikesImg = ImageBitmap.imageResource(id = R.drawable.spikes)
    val laserImg = ImageBitmap.imageResource(id = R.drawable.laser1)
    val sawbladeImg = ImageBitmap.imageResource(id = R.drawable.sawblade)
    val droneImg = ImageBitmap.imageResource(id = R.drawable.drone)
    val spikesFlippedImg = ImageBitmap.imageResource(id = R.drawable.spikes_flipped)
    
  
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
        else -> {} // no art yet for this World — existing gradient background shows through
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
                    text = String.format("Telemetry: %.1fm", simState.distanceMeters),
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

        // Live Graphic Space Canvas
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF020104).copy(alpha = 0.25f))
                .border(2.dp, CyberPrimary, RoundedCornerShape(12.dp))
        ) {
            // Render active powerup durations or boss labels (Levels 1 - 10)
            Box(modifier = Modifier.fillMaxSize()) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cw = size.width
                    val ch = size.height

                    // Screen Shake translate (Level 5 / 9)
                    drawContext.canvas.translate(simState.screenShakeX, simState.screenShakeY)

                    // Grid cyber line backgrounds
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

                    // Draw Racetrack path bounds
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

                    // Render Procedural activeTrackElements (Gems, Powerups, Spikes, Pillars, Blades)
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
                                        run {
                                            drawImage(
                                                image = spikesFlippedImg,
                                                dstOffset = IntOffset((x - w / 2f).roundToInt(), (y - baseSize / 2f).roundToInt()),
                                                dstSize = IntSize(w.roundToInt(), baseSize.roundToInt()),
                                                alpha = glowPulse
                                            )
                                        }
                                    }
                                    "STALAGMITE"  -> {
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
                                    else -> {
                                        val bob = sin(simState.tickIndex * 0.2f) * ch * 0.015f
                                        val size = ch * 0.07f
                                        val w = size * (droneImg.width.toFloat() / droneImg.height.toFloat())
                                        drawImage(
                                            image = droneImg,
                                            dstOffset = IntOffset((x - w / 2f).roundToInt(), (y - size / 2f + bob).roundToInt()),
                                            dstSize = IntSize(w.roundToInt(), size.roundToInt())
                                        )
                                    
                                        
                                    }
                                }
                            }
                            "bullet" -> {
                                drawCircle(color = Color(0xFFFF00FF), radius = 3.dp.toPx(), center = Offset(x, y))
                            }
                        }
                    }

                    // Ghost vehicle dot (represent pink spaceship)
                    val ticksCount = simState.ghostYPath.size
                    val userX = cw * 0.2f // Lock user ship coordinates to 20% width for running feel
                    
                    if (ticksCount > 0) {
                        val ghostY = ch * (simState.ghostYPos / 100f)
                        val ghostX = cw * 0.2f

                        drawCircle(
                            color = CyberSecondary.copy(alpha = 0.5f),
                            radius = 8.dp.toPx(),
                            center = Offset(ghostX, ghostY)
                        )
                    }
// Draw User Pilot Character using real sprite art
                    val userY = ch * (simState.userYPos / 100f)

                    // Draw protective shields or active powerup halo visual hints
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
                    val topLeftX = userX - displayWidth * 0.42f
                    val topLeftY = userY - displayHeight * 0.55f

                    rotate(degrees = tiltAngle, pivot = Offset(userX, userY)) {
                        drawImage(
                            image = currentFrameImg,
                            dstOffset = IntOffset(topLeftX.roundToInt(), topLeftY.roundToInt()),
                            dstSize = IntSize(displayWidth.roundToInt(), displayHeight.roundToInt())
                       )
            }  
                    scorePopups.forEach { (value, spawnTick, yFractionAtSpawn) ->
                        val age = simState.tickIndex - spawnTick
                        val ageFrac = (age / 25f).coerceIn(0f, 1f)
                        val popupAlpha = 1f - ageFrac
                        val riseOffset = ageFrac * 60f
                        val popupY = ch * (yFractionAtSpawn / 100f) - riseOffset - 40f
                        drawText(
                            textMeasurer = textMeasurer,
                            text = "+$value",
                            topLeft = Offset(userX - 20f, popupY),
                            style = TextStyle(
                                color = Color(0xFFFFD700).copy(alpha = popupAlpha),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                 // Clean screen shake translation reset
                    drawContext.canvas.translate(-simState.screenShakeX, -simState.screenShakeY)
                    val flashAge = simState.tickIndex - flashStartTick
                    if (flashAge in 0..6) {
                        val flashAlpha = (1f - flashAge / 6f) * 0.35f
                        drawRect(color = Color.Red.copy(alpha = flashAlpha), size = Size(cw, ch))
                        
                       }
                   }
      

                // Render Active Buff Pills Overlay (Level 4)
                if (simState.activePowerupDurations.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        simState.activePowerupDurations.forEach { (puId, remaining) ->
                            val label = when (puId) {
                                "PU1" -> "🛡️ SHIELD"
                                "PU2" -> "🧲 MAGNET"
                                "PU3" -> "⏳ SLOW"
                                "PU4" -> "👻 GHOST"
                                "PU5" -> "⚡ x2"
                                "PU6" -> "🔥 x5"
                                "PU7" -> "🌟 INVINC"
                                "PU8" -> "💣 TRAIL"
                                "PU9" -> "🔍 SHRINK"
                                else -> puId
                            }
                            Text(
                                text = "$label (${remaining / 8}s)",
                                color = CyberPrimary,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier
                                    .background(CyberSurface, RoundedCornerShape(2.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Render Boss Battle Warning HUD and Health Meter (Level 5)
                if (simState.bossActive) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "ZONE ADVANCED COMMAND BOSS",
                            color = Color(0xFFFF0055),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(CyberSurface)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(simState.bossHealth)
                                    .fillMaxHeight()
                                    .background(Color(0xFFFF0055))
                            )
                        }
                    }
                }
            }

            // Sync alert banners shifting values
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = simState.feedbackMessage,
                    color = if (simState.feedbackMessage.contains("CRITICAL")) CyberSecondary else CyberPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Dynamic coordinate sliding panel input
        Text(
            text = "🛰️ SHIP POSITION CALIBRATION CONTROLLER",
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.6f),
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.adjustUserY(-5) },
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(CyberSurface)
                    .size(48.dp)
            ) {
                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Climb", tint = CyberPrimary)
            }

            // Central joystick offset coordinate slider for high precision
            Slider(
                value = simState.userYPos.toFloat(),
                onValueChange = {
                    viewModel.adjustUserY((it - simState.userYPos).toInt())
                },
                valueRange = 10f..90f,
                colors = SliderDefaults.colors(
                    thumbColor = CyberPrimary,
                    activeTrackColor = CyberPrimary,
                    inactiveTrackColor = CyberSurface
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
                    .testTag("slider_coor_input")
            )

            IconButton(
                onClick = { viewModel.adjustUserY(5) },
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(CyberSurface)
                    .size(48.dp)
            ) {
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Descend", tint = CyberPrimary)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Battery / Fuel state
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "FUEL CELLS: ",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(end = 4.dp)
            )
            LinearProgressIndicator(
                progress = simState.fuelLevelPercent / 100f,
                color = if (simState.fuelLevelPercent < 25) CyberSecondary else CyberPrimary,
                trackColor = CyberSurface,
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
            )
            Text(
                text = " ${simState.fuelLevelPercent}%",
                color = Color.White,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
    StoryBannerHost(
        storyEvent = viewModel.storyEvent,
        modifier = Modifier.align(Alignment.BottomCenter)
    )
    }
}


@Composable
fun NeonPilotScreensaver() {
    val infiniteTransition = rememberInfiniteTransition(label = "screensaver")
    
    // Animate X position of the pilot
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    // Animate pilot's legs coordinate for a running motion!
    val frameProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "frameProgress"
    )
    val currentFrame = frameProgress.toInt() % 4

    val densityValue = LocalDensity.current.density
    val pilotHeight = (80 * densityValue).toInt()
    val pilotWidth = (40 * densityValue).toInt()

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.45f)
            .clip(RectangleShape)
            .background(Color(0xFF020104))
    ) {
        val cw = size.width
        val ch = size.height

        // 1. Draw 3D Perspective Grid filling bottom half
        val horizonY = ch * 0.5f
        val gridColor = Color(0xFF06B6D4).copy(alpha = 0.20f)
        val gridWidth = 1.5f

        // 12 horizontal lines
        for (i in 0..12) {
            val ratio = (i.toFloat() / 12f).pow(2f)
            val lineY = horizonY + (ch - horizonY) * ratio
            drawLine(
                color = gridColor,
                start = Offset(0f, lineY),
                end = Offset(cw, lineY),
                strokeWidth = gridWidth
            )
        }

        // 16 vertical lines converging to vanishing point
        val vpX = cw * 0.5f
        val vpY = horizonY
        for (i in 0..16) {
            val bottomX = (cw / 16) * i
            val shift = (progress * (cw / 16))
            val adjustedBottomX = ((bottomX - shift) % cw + cw) % cw
            drawLine(
                color = gridColor,
                start = Offset(vpX, vpY),
                end = Offset(adjustedBottomX, ch),
                strokeWidth = gridWidth
            )
        }

        // 2. Base running metrics
        val feetY = ch * 0.75f
        val startX = -pilotWidth.toFloat()
        val endX = cw + pilotWidth.toFloat()
        val currentX = startX + (endX - startX) * progress

        // 3. Draw Rainbow Trail with 10 ghost copies
        val opacities = listOf(0.85f, 0.72f, 0.60f, 0.50f, 0.40f, 0.32f, 0.24f, 0.16f, 0.10f, 0.05f)
        val ghostColors = listOf(
            Color(0xFF06B6D4), // Cyan
            Color(0xFFEC4899), // Pink
            Color(0xFF8B5CF6), // Purple
            Color(0xFFF59E0B)  // Yellow
        )

        val bootWidth = 0.12f * pilotHeight
        val bootHeight = 0.05f * pilotHeight

        for (g in 1..10) {
            val opacity = opacities[g - 1]
            val baseColor = ghostColors[(g - 1) % ghostColors.size]
            val ghostColor = baseColor.copy(alpha = opacity)
            val ghostX = currentX - g * 18f

            if (ghostX > -pilotWidth && ghostX < cw + pilotWidth) {
                val ghostFrame = (currentFrame + 4 - (g % 4)) % 4
                val gHeadBob = when (ghostFrame) {
                    0 -> -0.02f * pilotHeight
                    1 -> 0.02f * pilotHeight
                    2 -> -0.02f * pilotHeight
                    else -> 0.02f * pilotHeight
                }
                val gHeadY = (ch * 0.45f) + gHeadBob
                val gHead = Offset(ghostX, gHeadY)

                val gTorsoTopY = gHeadY + 0.25f * pilotHeight
                val gTorsoBottomY = gHeadY + 0.55f * pilotHeight
                val gTorsoTop = Offset(ghostX, gTorsoTopY)
                val gTorsoBottom = Offset(ghostX, gTorsoBottomY)

                var gLeg1End = Offset(ghostX, feetY)
                var gLeg2End = Offset(ghostX, feetY)
                var gKnee1 = Offset(ghostX, (gTorsoBottomY + feetY) / 2)
                var gKnee2 = Offset(ghostX, (gTorsoBottomY + feetY) / 2)

                val gChestY = gTorsoTopY + 0.1f * pilotHeight
                val gShoulder = Offset(ghostX, gChestY)
                var gArm1End = Offset(ghostX, gChestY + 0.25f * pilotHeight)
                var gArm2End = Offset(ghostX, gChestY + 0.25f * pilotHeight)

                when (ghostFrame) {
                    0 -> {
                        gLeg1End = Offset(ghostX + 0.25f * pilotWidth, feetY)
                        gKnee1 = Offset(ghostX + 0.15f * pilotWidth, gTorsoBottomY + 0.2f * pilotHeight)
                        gLeg2End = Offset(ghostX - 0.25f * pilotWidth, feetY - 0.05f * pilotHeight)
                        gKnee2 = Offset(ghostX - 0.1f * pilotWidth, gTorsoBottomY + 0.15f * pilotHeight)
                        gArm1End = Offset(ghostX - 0.2f * pilotWidth, gChestY + 0.15f * pilotHeight)
                        gArm2End = Offset(ghostX + 0.2f * pilotWidth, gChestY + 0.15f * pilotHeight)
                    }
                    1 -> {
                        gLeg1End = Offset(ghostX + 0.05f * pilotWidth, feetY)
                        gKnee1 = Offset(ghostX + 0.05f * pilotWidth, gTorsoBottomY + 0.22f * pilotHeight)
                        gLeg2End = Offset(ghostX - 0.05f * pilotWidth, feetY)
                        gKnee2 = Offset(ghostX - 0.05f * pilotWidth, gTorsoBottomY + 0.22f * pilotHeight)
                        gArm1End = Offset(ghostX - 0.02f * pilotWidth, gChestY + 0.25f * pilotHeight)
                        gArm2End = Offset(ghostX + 0.02f * pilotWidth, gChestY + 0.25f * pilotHeight)
                    }
                    2 -> {
                        gLeg1End = Offset(ghostX - 0.25f * pilotWidth, feetY - 0.05f * pilotHeight)
                        gKnee1 = Offset(ghostX - 0.1f * pilotWidth, gTorsoBottomY + 0.15f * pilotHeight)
                        gLeg2End = Offset(ghostX + 0.25f * pilotWidth, feetY)
                        gKnee2 = Offset(ghostX + 0.15f * pilotWidth, gTorsoBottomY + 0.2f * pilotHeight)
                        gArm1End = Offset(ghostX + 0.2f * pilotWidth, gChestY + 0.15f * pilotHeight)
                        gArm2End = Offset(ghostX - 0.2f * pilotWidth, gChestY + 0.15f * pilotHeight)
                    }
                    3 -> {
                        gLeg1End = Offset(ghostX - 0.05f * pilotWidth, feetY)
                        gKnee1 = Offset(ghostX - 0.05f * pilotWidth, gTorsoBottomY + 0.22f * pilotHeight)
                        gLeg2End = Offset(ghostX + 0.05f * pilotWidth, feetY)
                        gKnee2 = Offset(ghostX + 0.05f * pilotWidth, gTorsoBottomY + 0.22f * pilotHeight)
                        gArm1End = Offset(ghostX + 0.02f * pilotWidth, gChestY + 0.25f * pilotHeight)
                        gArm2End = Offset(ghostX - 0.02f * pilotWidth, gChestY + 0.25f * pilotHeight)
                    }
                }

                // Ghost head
                drawCircle(color = ghostColor, radius = 0.1f * pilotHeight, center = gHead)

                // Ghost visor
                drawRect(
                    color = Color.White.copy(alpha = opacity),
                    topLeft = Offset(ghostX + (0.1f * pilotHeight) * 0.1f, gHeadY - (0.1f * pilotHeight) * 0.4f),
                    size = Size((0.1f * pilotHeight) * 0.7f, (0.1f * pilotHeight) * 0.5f)
                )

                // Ghost torso
                drawLine(color = ghostColor, start = gTorsoTop, end = gTorsoBottom, strokeWidth = 0.15f * pilotHeight)

                // Ghost legs
                drawLine(color = ghostColor, start = gTorsoBottom, end = gKnee1, strokeWidth = 0.08f * pilotHeight)
                drawLine(color = ghostColor, start = gKnee1, end = gLeg1End, strokeWidth = 0.06f * pilotHeight)
                drawLine(color = ghostColor, start = gTorsoBottom, end = gKnee2, strokeWidth = 0.08f * pilotHeight)
                drawLine(color = ghostColor, start = gKnee2, end = gLeg2End, strokeWidth = 0.06f * pilotHeight)

                // Ghost boots
                drawRect(
                    color = ghostColor,
                    topLeft = Offset(gLeg1End.x - bootWidth / 2, gLeg1End.y),
                    size = Size(bootWidth, bootHeight)
                )
                drawRect(
                    color = ghostColor,
                    topLeft = Offset(gLeg2End.x - bootWidth / 2, gLeg2End.y),
                    size = Size(bootWidth, bootHeight)
                )

                // Ghost arms
                drawLine(color = ghostColor, start = gShoulder, end = gArm1End, strokeWidth = 0.05f * pilotHeight)
                drawLine(color = ghostColor, start = gShoulder, end = gArm2End, strokeWidth = 0.05f * pilotHeight)
            }
        }

        // 4. Draw Active Lead Running Pilot
        if (currentX > -pilotWidth && currentX < cw + pilotWidth) {
            val bobOffset = when (currentFrame) {
                0 -> -0.02f * pilotHeight
                1 -> 0.02f * pilotHeight
                2 -> -0.02f * pilotHeight
                else -> 0.02f * pilotHeight
            }
            val headY = (ch * 0.45f) + bobOffset
            val headCenter = Offset(currentX, headY)
            val helmetRadius = 0.1f * pilotHeight

            val torsoTopY = headY + 0.25f * pilotHeight
            val torsoBottomY = headY + 0.55f * pilotHeight
            val torsoTop = Offset(currentX, torsoTopY)
            val torsoBottom = Offset(currentX, torsoBottomY)

            var leg1End = Offset(currentX, feetY)
            var leg2End = Offset(currentX, feetY)
            var knee1 = Offset(currentX, (torsoBottomY + feetY) / 2)
            var knee2 = Offset(currentX, (torsoBottomY + feetY) / 2)

            val chestY = torsoTopY + 0.1f * pilotHeight
            val shoulder = Offset(currentX, chestY)
            var arm1End = Offset(currentX, chestY + 0.25f * pilotHeight)
            var arm2End = Offset(currentX, chestY + 0.25f * pilotHeight)

            when (currentFrame) {
                0 -> {
                    leg1End = Offset(currentX + 0.25f * pilotWidth, feetY)
                    knee1 = Offset(currentX + 0.15f * pilotWidth, torsoBottomY + 0.2f * pilotHeight)
                    leg2End = Offset(currentX - 0.25f * pilotWidth, feetY - 0.05f * pilotHeight)
                    knee2 = Offset(currentX - 0.1f * pilotWidth, torsoBottomY + 0.15f * pilotHeight)
                    arm1End = Offset(currentX - 0.2f * pilotWidth, chestY + 0.15f * pilotHeight)
                    arm2End = Offset(currentX + 0.2f * pilotWidth, chestY + 0.15f * pilotHeight)
                }
                1 -> {
                    leg1End = Offset(currentX + 0.05f * pilotWidth, feetY)
                    knee1 = Offset(currentX + 0.05f * pilotWidth, torsoBottomY + 0.22f * pilotHeight)
                    leg2End = Offset(currentX - 0.05f * pilotWidth, feetY)
                    knee2 = Offset(currentX - 0.05f * pilotWidth, torsoBottomY + 0.22f * pilotHeight)
                    arm1End = Offset(currentX - 0.02f * pilotWidth, chestY + 0.25f * pilotHeight)
                    arm2End = Offset(currentX + 0.02f * pilotWidth, chestY + 0.25f * pilotHeight)
                }
                2 -> {
                    leg1End = Offset(currentX - 0.25f * pilotWidth, feetY - 0.05f * pilotHeight)
                    knee1 = Offset(currentX - 0.1f * pilotWidth, torsoBottomY + 0.15f * pilotHeight)
                    leg2End = Offset(currentX + 0.25f * pilotWidth, feetY)
                    knee2 = Offset(currentX + 0.15f * pilotWidth, torsoBottomY + 0.2f * pilotHeight)
                    arm1End = Offset(currentX + 0.2f * pilotWidth, chestY + 0.15f * pilotHeight)
                    arm2End = Offset(currentX - 0.2f * pilotWidth, chestY + 0.15f * pilotHeight)
                }
                3 -> {
                    leg1End = Offset(currentX - 0.05f * pilotWidth, feetY)
                    knee1 = Offset(currentX - 0.05f * pilotWidth, torsoBottomY + 0.22f * pilotHeight)
                    leg2End = Offset(currentX + 0.05f * pilotWidth, feetY)
                    knee2 = Offset(currentX + 0.05f * pilotWidth, torsoBottomY + 0.22f * pilotHeight)
                    arm1End = Offset(currentX + 0.02f * pilotWidth, chestY + 0.25f * pilotHeight)
                    arm2End = Offset(currentX - 0.02f * pilotWidth, chestY + 0.25f * pilotHeight)
                }
            }

            // Helmet (Head)
            drawCircle(color = Color(0xFFEC4899), radius = helmetRadius, center = headCenter)
            drawCircle(color = Color.White, radius = helmetRadius * 0.7f, center = headCenter, style = Stroke(width = 2.dp.toPx()))

            // Visor
            drawRect(
                color = Color(0xFF06B6D4),
                topLeft = Offset(currentX + helmetRadius * 0.1f, headY - helmetRadius * 0.4f),
                size = Size(helmetRadius * 0.7f, helmetRadius * 0.5f)
            )

            // Torso
            drawLine(color = Color(0xFF8B5CF6), start = torsoTop, end = torsoBottom, strokeWidth = 0.15f * pilotHeight)
            drawLine(color = Color.White, start = torsoTop, end = torsoBottom, strokeWidth = 2.dp.toPx())

            // Legs
            drawLine(color = Color(0xFF06B6D4), start = torsoBottom, end = knee1, strokeWidth = 0.08f * pilotHeight)
            drawLine(color = Color(0xFF06B6D4), start = knee1, end = leg1End, strokeWidth = 0.06f * pilotHeight)
            drawLine(color = Color(0xFFEC4899), start = torsoBottom, end = knee2, strokeWidth = 0.08f * pilotHeight)
            drawLine(color = Color(0xFFEC4899), start = knee2, end = leg2End, strokeWidth = 0.06f * pilotHeight)

            // Boots
            drawRect(color = Color(0xFFF59E0B), topLeft = Offset(leg1End.x - bootWidth / 2, leg1End.y), size = Size(bootWidth, bootHeight))
            drawRect(color = Color(0xFFF59E0B), topLeft = Offset(leg2End.x - bootWidth / 2, leg2End.y), size = Size(bootWidth, bootHeight))

            // Arms
            drawLine(color = Color(0xFF8B5CF6), start = shoulder, end = arm1End, strokeWidth = 0.05f * pilotHeight)
            drawLine(color = Color(0xFFEC4899), start = shoulder, end = arm2End, strokeWidth = 0.05f * pilotHeight)
        }
    }
}

@Composable
fun ProfileTab(profile: GameProfile, viewModel: NeonRushViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "👤 PILOT REGISTRY & DATA PROFILE",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = CyberPrimary,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Text(
            text = "Access hardware systems diagnostics, sync coordinates, and verify pilot credentials.",
            color = CyberOnSurface.copy(alpha = 0.7f),
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = CyberSurface),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CyberPrimary.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "SYSTEM METRICS",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = CyberPrimary,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                ProfileMetricRow("PILOT ID", profile.username)
                ProfileMetricRow("HIGH SCORE", "${profile.bestScore} PTS")
                ProfileMetricRow("GOLD PRESTIGE", "${profile.transcendenceCount} Lvl")
                ProfileMetricRow("POWER GEMS", "💎 ${profile.gems}")
                ProfileMetricRow("EQUIPPED MODEL", profile.activeSkinId.replace("_", " ").uppercase())
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = CyberSurface.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "SYSTEM STATUS",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = CyberSecondary,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "• COLD LAUNCH: OK\n• NEUROLINK TELEMETRY: GREEN\n• PROPULSION FUEL CELL: CALIBRATED\n• GHOST GRID: 3 RUNS SYNCED",
                    fontSize = 12.sp,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 20.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = CyberSurface),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CyberSecondary.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "🔊 AUDIO & HAPTICS HARDWARE CALIBRATION",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = CyberSecondary,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                var sfxOn by remember { mutableStateOf(com.neonrush.game.NeonSoundEngine.getSoundEffectsEnabled()) }
                var ambientOn by remember { mutableStateOf(com.neonrush.game.NeonSoundEngine.getAmbientEnabled()) }
                var hapticsOn by remember { mutableStateOf(com.neonrush.game.NeonSoundEngine.getHapticsEnabled()) }
                var volumePercent by remember { mutableStateOf(com.neonrush.game.NeonSoundEngine.getMasterVolumePercent().toFloat()) }

                // Sound Effects Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SOUND EFFECTS",
                        color = CyberOnSurface.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Switch(
                        checked = sfxOn,
                        onCheckedChange = {
                            sfxOn = it
                            com.neonrush.game.NeonSoundEngine.setSoundEffectsEnabled(it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyberSecondary,
                            checkedTrackColor = CyberSecondary.copy(alpha = 0.3f),
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color.DarkGray
                        ),
                        modifier = Modifier.testTag("sfx_toggle")
                    )
                }

                // Ambient / Music Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MUSIC & AMBIENT DRONE",
                        color = CyberOnSurface.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Switch(
                        checked = ambientOn,
                        onCheckedChange = {
                            ambientOn = it
                            com.neonrush.game.NeonSoundEngine.setAmbientEnabled(it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyberSecondary,
                            checkedTrackColor = CyberSecondary.copy(alpha = 0.3f),
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color.DarkGray
                        ),
                        modifier = Modifier.testTag("ambient_toggle")
                    )
                }

                // Haptics Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "NEURAL HAPTIC FEEDBACK",
                        color = CyberOnSurface.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Switch(
                        checked = hapticsOn,
                        onCheckedChange = {
                            hapticsOn = it
                            com.neonrush.game.NeonSoundEngine.setHapticsEnabled(it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyberSecondary,
                            checkedTrackColor = CyberSecondary.copy(alpha = 0.3f),
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color.DarkGray
                        ),
                        modifier = Modifier.testTag("haptic_toggle")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Master Volume Slider
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "MASTER AUDIO VOLUME",
                            color = CyberOnSurface.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "${volumePercent.toInt()}%",
                            color = CyberSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Slider(
                        value = volumePercent,
                        onValueChange = {
                            volumePercent = it
                            com.neonrush.game.NeonSoundEngine.setMasterVolumePercent(it.toInt())
                        },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = CyberSecondary,
                            activeTrackColor = CyberSecondary,
                            inactiveTrackColor = Color.DarkGray
                        ),
                        modifier = Modifier
                            .testTag("volume_slider")
                            .padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Test Category Sound Buttons (Obstacle, Laser, Shield, etc.)
                Text(
                    text = "HARDWARE DIAGNOSTIC TEST DRIVES",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = CyberOnSurface.copy(alpha = 0.5f),
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                val engine = remember { com.neonrush.game.NeonSoundEngine() }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { engine.playObstaclePass() },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary.copy(alpha = 0.15f)),
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .testTag("test_sound_obstacle"),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("OBSTACLE", color = CyberPrimary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                        Button(
                            onClick = { engine.playNearMiss() },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberSecondary.copy(alpha = 0.15f)),
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .testTag("test_sound_near_miss"),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("NEAR MISS", color = CyberSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                        Button(
                            onClick = { engine.playSpeedMilestone(200) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.12f)),
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .testTag("test_sound_milestone"),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("MILESTONE", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { engine.playLaserWarning() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Magenta.copy(alpha = 0.15f)),
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .testTag("test_sound_laser"),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("LASER BEAM", color = Color.Magenta, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                        Button(
                            onClick = { engine.playCollision() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.15f)),
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .testTag("test_sound_collision"),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("COLLISION", color = Color.Red, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                        Button(
                            onClick = { engine.playPersonalBestBroken() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Yellow.copy(alpha = 0.15f)),
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .testTag("test_sound_pbest"),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("NEW RECORD", color = Color.Yellow, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { engine.playShieldPowerup() },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberSecondary.copy(alpha = 0.12f)),
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .testTag("test_sound_shield"),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("SHIELD UP", color = CyberSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                        Button(
                            onClick = { engine.playGemCollect() },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary.copy(alpha = 0.12f)),
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .testTag("test_sound_gem"),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("GEM COLLECT", color = CyberPrimary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                        Button(
                            onClick = { engine.playGhostOvertake() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Cyan.copy(alpha = 0.15f)),
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .testTag("test_sound_overtake"),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("OVERTAKE", color = Color.Cyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileMetricRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = CyberOnSurface.copy(alpha = 0.6f), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        Text(text = value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun DailyMutationActiveBanner() {
    val activeMutation = DailyMutations.getActiveMutation()
    val parsedColor = remember(activeMutation.colorHex) {
        Color(android.graphics.Color.parseColor(activeMutation.colorHex))
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = CyberSurface.copy(alpha = 0.85f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                Brush.horizontalGradient(listOf(parsedColor, CyberSecondary)),
                RoundedCornerShape(12.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = activeMutation.emoji,
                fontSize = 24.sp,
                modifier = Modifier.padding(end = 12.dp)
            )
            Column {
                Text(
                    text = "ACTIVE DAILY MODIFIER: ${activeMutation.title}",
                    color = parsedColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = activeMutation.description,
                    color = Color.White.copy(alpha = 0.82f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
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
    onShowPaywall: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "⚡ NEURAL SYNC DISRUPTED ⚡",
            color = CyberSecondary,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 20.sp,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "FLIGHT RUN COMPLETED",
            color = Color.White.copy(alpha = 0.6f),
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = CyberSurface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, CyberPrimary, RoundedCornerShape(16.dp))
                .padding(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "FINAL SCORE",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "${simState.score} PTS",
                    color = CyberPrimary,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Divider(color = CyberPrimary.copy(alpha = 0.15f), modifier = Modifier.padding(vertical = 12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "DISTANCE", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        Text(text = String.format("%.1fm", simState.distanceMeters), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "TOP SPEED", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        Text(text = "${simState.speedKmh} KM/H", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "SECTOR", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        Text(text = simState.currentZoneName, color = CyberSecondary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (isPro) {
                    viewModel.reviveSimulation()
                } else if (activity != null) {
                    AdMobManager.showRewardedIfReady(activity) {
                        viewModel.reviveSimulation()
                    }
                } else {
                    viewModel.reviveSimulation()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = CyberSecondary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("revive_button")
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "❤️ REVIVE FLIGHT ", color = Color.White, fontWeight = FontWeight.Bold)
                if (!isPro) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            text = "AD",
                            color = CyberSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } else {
                    Text(text = " (PRO INSTANT)", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.Normal)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                if (!isPro) {
                    AdMobManager.incrementGameOver()
                    if (AdMobManager.isPaywallDue()) {
                        onShowPaywall()
                    } else if (AdMobManager.isInterstitialDue() && activity != null) {
                        AdMobManager.showInterstitialIfReady(activity) {
                            viewModel.resetSimulation()
                        }
                    } else {
                        viewModel.resetSimulation()
                    }
                } else {
                    viewModel.resetSimulation()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = CyberSurface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .border(1.dp, CyberPrimary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .testTag("exit_run_button")
        ) {
            Text(text = "◀ RETURN TO HQ", color = CyberPrimary, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (!isPro) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberSurface.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onShowPaywall() }
                    .border(1.dp, CyberPrimary.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "👑 GET NEON RUSH PRO",
                            color = CyberPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Remove all ads, get unlimited daily challenges, and earn the exclusive PRO badge!",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    }
                    Icon(Icons.Filled.KeyboardArrowRight, contentDescription = "Buy", tint = CyberPrimary)
                }
            }
        }
    }
}

@Composable
fun PaywallDialog(onDismiss: () -> Unit, reason: String = "generic") {
    val context = LocalContext.current
    val activity = context as? Activity
    var selectedTier by remember { mutableStateOf("monthly") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF120324),
        modifier = Modifier.border(2.dp, CyberPrimary, RoundedCornerShape(16.dp)),
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (reason == "world4") "🌴 GREEN HELL AWAITS" else "🏆 NEON RUSH PRO",
                    color = CyberPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Subscription Portal",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (reason == "world4")
                        "Your chopper went down days ago. Something in the reserve has been pacing you ever since — and it's not done. Continue PRO to find out what's hunting you."
                    else
                        "Gain supreme access to unrestricted flight telemetry. Fuel and drift systems optimization package:",
                    color = Color.White,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Row(
                    modifier = Modifier.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "🚫", fontSize = 18.sp, modifier = Modifier.padding(end = 12.dp))
                    Text(text = "No ads (removes home standard banner, interstitials, and revives)", color = Color.White, fontSize = 12.sp)
                }

                Row(
                    modifier = Modifier.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "👑", fontSize = 18.sp, modifier = Modifier.padding(end = 12.dp))
                    Text(text = "PRO Badge enabled for Global Rankings and pilot registry", color = Color.White, fontSize = 12.sp)
                }

                Row(
                    modifier = Modifier.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "🔥", fontSize = 18.sp, modifier = Modifier.padding(end = 12.dp))
                    Text(text = "Unlimited daily challenge sync attempts", color = Color.White, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedTier = "monthly" }
                        .background(
                            if (selectedTier == "monthly") CyberPrimary.copy(alpha = 0.15f) else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                        .border(
                            1.dp,
                            if (selectedTier == "monthly") CyberPrimary else Color.White.copy(alpha = 0.2f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedTier == "monthly",
                        onClick = { selectedTier = "monthly" },
                        colors = RadioButtonDefaults.colors(selectedColor = CyberPrimary)
                    )
                    Text(
                        text = "Monthly — ${RevenueCatManager.SUBSCRIPTION_PRICE_MONTHLY_USD}",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedTier = "annual" }
                        .background(
                            if (selectedTier == "annual") CyberPrimary.copy(alpha = 0.15f) else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                        .border(
                            1.dp,
                            if (selectedTier == "annual") CyberPrimary else Color.White.copy(alpha = 0.2f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedTier == "annual",
                        onClick = { selectedTier = "annual" },
                        colors = RadioButtonDefaults.colors(selectedColor = CyberPrimary)
                    )
                    Column {
                        Text(
                            text = "Annual — ${RevenueCatManager.SUBSCRIPTION_PRICE_ANNUAL_USD}",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "BEST VALUE — save ~50%",
                            color = CyberSecondary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (activity != null) {
                        val onResult: (Boolean) -> Unit = { success ->
                            if (success) {
                                onDismiss()
                            }
                        }
                        if (selectedTier == "annual") {
                            RevenueCatManager.purchaseProSubscriptionAnnual(activity, onResult)
                        } else {
                            RevenueCatManager.purchaseProSubscription(activity, onResult)
                        }
                    } else {
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary)
            ) {
                Text(text = "SUBSCRIBE", color = CyberBackground, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "DISMISS", color = Color.White.copy(alpha = 0.5f))
            }
        }
    )
}

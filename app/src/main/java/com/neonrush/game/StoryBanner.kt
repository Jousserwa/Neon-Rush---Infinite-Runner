// StoryBanner.kt
package com.neonrush.game

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// Rough neon accent per world — adjust to match your actual art palette per-World
private fun accentColorFor(worldId: Int): Color = when (worldId) {
    1 -> Color(0xFFFF3D9A) // Blackout Front — pink/magenta
    2 -> Color(0xFF00E5FF) // Derelict Signal — cyan
    3 -> Color(0xFFB388FF) // Cell Block Zero — violet
    4 -> Color(0xFF69F0AE) // Green Hell — acid green
    else -> Color(0xFF00E5FF)
}

@Composable
fun StoryBannerHost(
    storyEvent: kotlinx.coroutines.flow.SharedFlow<StoryEvent>,
    modifier: Modifier = Modifier
) {
    var activeEvent by remember { mutableStateOf<StoryEvent?>(null) }
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        storyEvent.collect { event ->
            activeEvent = event
            visible = true
            delay(4200) // hold time before fade starts
            visible = false
            delay(400) // let fade-out finish before clearing text underneath
            activeEvent = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 96.dp, start = 20.dp, end = 20.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(350)),
            exit = fadeOut(tween(400))
        ) {
            activeEvent?.let { event ->
                val accent = accentColorFor(event.world.id)
                Column(
                    modifier = Modifier
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xCC0A0A12), Color(0xE60A0A12))
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .border(1.dp, accent.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 18.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = event.world.title,
                        color = accent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        letterSpacing = 2.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = event.text,
                        color = Color.White,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Start
                    )
                }
            }
        }
    }
}

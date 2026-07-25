package com.neonrush.game.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

data class PromoMessage(
    val icon: String,
    val text: String,
    val targetTab: String // "social" (missions) or "skins" (suits/remove ads)
)

object PromoMessages {
    val ALL = listOf(
        PromoMessage("🎯", "Daily missions are waiting — claim your gems!", "social"),
        PromoMessage("🗓️", "Weekly missions in progress. Check your rewards!", "social"),
        PromoMessage("🏆", "Monthly missions unlock an exclusive suit!", "social"),
        PromoMessage("👕", "New pilot suits are in the shop!", "skins"),
        PromoMessage("🚫", "Tired of ads? Remove them forever.", "skins")
    )
}

@Composable
fun PromoPopup(
    onNavigate: (String) -> Unit,
    intervalMillis: Long = 25000L,
    visibleMillis: Long = 2500L
) {
    var currentPromo by remember { mutableStateOf<PromoMessage?>(null) }
    var promoIndex by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(intervalMillis)
            currentPromo = PromoMessages.ALL[promoIndex % PromoMessages.ALL.size]
            promoIndex++
            delay(visibleMillis)
            currentPromo = null
        }
    }

    AnimatedVisibility(
        visible = currentPromo != null,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = Modifier.fillMaxWidth()
    ) {
        currentPromo?.let { promo ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1A0B2E))
                    .border(1.dp, Color(0xFFFF2E92).copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    .clickable {
                        onNavigate(promo.targetTab)
                        currentPromo = null
                    }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = promo.icon, fontSize = 20.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = promo.text,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

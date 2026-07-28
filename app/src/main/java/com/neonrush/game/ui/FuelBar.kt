@Composable
fun FuelBar(
    fuelPercent: Int,
    refillCount: Int,
    isPro: Boolean,
    cost: Int,
    onRefuel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val capReached = !isPro && refillCount >= 3

    // Base color by fuel level
    val baseColor = when {
        fuelPercent > 50 -> Color(0xFF00E5FF) // calm blue/cyan
        fuelPercent > 30 -> Color(0xFFFF9800) // orange warning
        else -> Color(0xFFFF1744)             // urgent red
    }

    // Pulse speed/intensity scales with urgency
    val infiniteTransition = rememberInfiniteTransition(label = "fuelPulse")
    val pulseDurationMs = when {
        fuelPercent > 50 -> 100000 // effectively static
        fuelPercent > 30 -> 900
        else -> 450
    }
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = if (fuelPercent > 50) 1f else 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(pulseDurationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fuelPulseAlpha"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF14141C))
            .border(1.dp, baseColor.copy(alpha = pulseAlpha), RoundedCornerShape(8.dp))
            .clickable(enabled = !capReached) { onRefuel() }
    ) {
        // Fill proportional to fuel level
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction = (fuelPercent / 100f).coerceIn(0f, 1f))
                .background(baseColor.copy(alpha = pulseAlpha * 0.35f))
        )

        Text(
            text = if (capReached) "FUEL ${fuelPercent}%" else "⛽ ${fuelPercent}%  •  TAP: $cost💎",
            color = Color.White,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

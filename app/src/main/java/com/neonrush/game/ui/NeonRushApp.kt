package com.neonrush.game.ui

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neonrush.game.NeonRushViewModel
import com.neonrush.game.RevenueCatManager

@Composable
fun NeonRushApp(viewModel: NeonRushViewModel) {
    var showPaywall by remember { mutableStateOf(false) }

    // Main Game UI Container
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Your main game screens render here

        // Paywall Dialog Overlay
        if (showPaywall) {
            PaywallDialog(
                onDismiss = { showPaywall = false },
                onSuccess = {
                    showPaywall = false
                    // Update view model state if needed
                }
            )
        }
    }
}

@Composable
fun PaywallDialog(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    var isMonthlySelected by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .background(Color(0xFF0F172A), RoundedCornerShape(16.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "NEON RUSH PRO",
                color = Color(0xFF00E5FF),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Unlock all Worlds, remove ads, access Legendary difficulty, and get exclusive player skins!",
                color = Color.White,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Monthly vs Annual Toggle Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = { isMonthlySelected = true }) {
                    Text(
                        text = "Monthly: $2.99",
                        color = if (isMonthlySelected) Color(0xFFFF007F) else Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                }

                TextButton(onClick = { isMonthlySelected = false }) {
                    Text(
                        text = "Annual: $24.00",
                        color = if (!isMonthlySelected) Color(0xFFFF007F) else Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("MAYBE LATER", color = Color.Gray)
                }

                Button(
                    onClick = {
                        if (activity != null) {
                            // Target exact RevenueCat identifiers for base plans
                            val targetPackage = if (isMonthlySelected) "neon_rush_pro:monthly" else "neon_rush_pro:annual"
                            
                            RevenueCatManager.purchasePackage(
                                activity = activity,
                                packageIdentifier = targetPackage,
                                onSuccess = { customerInfo ->
                                    if (RevenueCatManager.isProActive(customerInfo)) {
                                        Toast.makeText(context, "Welcome to Neon Rush Pro!", Toast.LENGTH_SHORT).show()
                                        onSuccess()
                                    }
                                },
                                onError = { error ->
                                    Toast.makeText(context, "Purchase Failed: $error", Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                ) {
                    Text("SUBSCRIBE", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Helper to trigger In-App Purchases for Gems or Skins anywhere in Compose UI
fun triggerStorePurchase(
    activity: Activity,
    packageIdentifier: String,
    onSuccessGranted: () -> Unit
) {
    RevenueCatManager.purchasePackage(
        activity = activity,
        packageIdentifier = packageIdentifier,
        onSuccess = {
            Toast.makeText(activity, "Purchase Completed!", Toast.LENGTH_SHORT).show()
            onSuccessGranted()
        },
        onError = { error ->
            Toast.makeText(activity, "Purchase Failed: $error", Toast.LENGTH_LONG).show()
        }
    )
}

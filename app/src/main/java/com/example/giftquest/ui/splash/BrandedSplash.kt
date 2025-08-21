package com.example.giftquest.ui.splash

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.giftquest.R
import kotlinx.coroutines.delay

/**
 * Branded splash (icon version). Shows blue BG + white vector icon + "GiftQuest".
 * Use this if you want to rely on the vector (crisp on all DPIs).
 */
@Composable
fun BrandedSplash(
    onFinish: () -> Unit,
    durationMs: Long = 1800,
    bgColor: Color = Color(0xFF3B82F6), // GiftQuest blue
    iconTint: Color = Color.White,
    titleColor: Color = Color.White
) {
    // hold for durationMs then finish
    LaunchedEffect(Unit) {
        delay(durationMs)
        onFinish()
    }

    // subtle fade-in
    val alpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 400),
        label = "splashFadeIn"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .alpha(alpha),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = painterResource(id = R.drawable.ic_splash), // <- white vector in res/drawable
                contentDescription = "GiftQuest",
                modifier = Modifier.size(120.dp),
                tint = iconTint
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "GiftQuest",
                color = titleColor,
                fontSize = 28.sp,
                style = MaterialTheme.typography.headlineSmall
            )
        }
    }
}
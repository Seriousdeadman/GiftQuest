package com.example.giftquest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.giftquest.ui.splash.BrandedSplash

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Install OS splash and add a short fade-out
        val splash: SplashScreen = installSplashScreen()
        splash.setOnExitAnimationListener { provider ->
            provider.view
                .animate()
                .alpha(0f)
                .setDuration(200)         // quick fade of OS splash
                .withEndAction { provider.remove() }
                .start()
        }

        super.onCreate(savedInstanceState)
        setContent {
            var showBranded by remember { mutableStateOf(true) }

            Crossfade(
                targetState = showBranded,
                animationSpec = tween(500) // fade branded → app
            ) { isSplash ->
                if (isSplash) {
                    BrandedSplash(
                        durationMs = 1800,            // a bit longer
                        onFinish = { showBranded = false }
                    )
                } else {
                    GiftQuestApp()
                }
            }
        }
    }
}

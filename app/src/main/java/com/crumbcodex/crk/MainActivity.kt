package com.crumbcodex.crk

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crumbcodex.crk.ui.navigation.CrkApp
import com.crumbcodex.crk.ui.navigation.LocalThemeController
import com.crumbcodex.crk.ui.navigation.ThemeController
import com.crumbcodex.crk.ui.theme.CrkTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as CrkApplication
        val prefs = app.container.themePreferences
        setContent {
            val themeMode by prefs.mode.collectAsStateWithLifecycle()
            val seedState by app.seedState.collectAsStateWithLifecycle()
            CompositionLocalProvider(LocalThemeController provides ThemeController(themeMode, prefs::set)) {
                CrkTheme(themeMode) {
                    CrkApp(seedState = seedState, onRetrySeed = app::reseed)
                }
            }
        }
    }
}

package com.crumbcodex.crk

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crumbcodex.crk.ui.navigation.CrkApp
import com.crumbcodex.crk.ui.navigation.LocalThemeController
import com.crumbcodex.crk.ui.navigation.ThemeController
import com.crumbcodex.crk.ui.theme.CrkTheme

class MainActivity : ComponentActivity() {
    /** Set when launched from a "new coupon codes" notification. */
    private val openCoupons = mutableStateOf(false)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getStringExtra(EXTRA_OPEN) == OPEN_COUPONS) openCoupons.value = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null && intent?.getStringExtra(EXTRA_OPEN) == OPEN_COUPONS) openCoupons.value = true
        enableEdgeToEdge()
        val app = application as CrkApplication
        val prefs = app.container.themePreferences
        setContent {
            val themeMode by prefs.mode.collectAsStateWithLifecycle()
            val seedState by app.seedState.collectAsStateWithLifecycle()
            CompositionLocalProvider(LocalThemeController provides ThemeController(themeMode, prefs::set)) {
                CrkTheme(themeMode) {
                    CrkApp(
                        seedState = seedState,
                        onRetrySeed = app::reseed,
                        openCoupons = openCoupons.value,
                        onOpenCouponsHandled = { openCoupons.value = false },
                    )
                }
            }
        }
    }

    companion object {
        const val EXTRA_OPEN = "open"
        const val OPEN_COUPONS = "coupons"
    }
}

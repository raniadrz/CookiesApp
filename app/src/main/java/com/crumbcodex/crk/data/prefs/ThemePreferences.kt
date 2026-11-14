package com.crumbcodex.crk.data.prefs

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode(val label: String) { SYSTEM("System"), LIGHT("Light"), DARK("Dark") }

/** Tiny SharedPreferences-backed store for the theme toggle. */
class ThemePreferences(context: Context) {
    private val prefs = context.getSharedPreferences("crumb_codex_prefs", Context.MODE_PRIVATE)

    private val _mode = MutableStateFlow(
        runCatching { ThemeMode.valueOf(prefs.getString(KEY, ThemeMode.SYSTEM.name)!!) }.getOrDefault(ThemeMode.SYSTEM),
    )
    val mode: StateFlow<ThemeMode> = _mode.asStateFlow()

    fun set(mode: ThemeMode) {
        prefs.edit().putString(KEY, mode.name).apply()
        _mode.value = mode
    }

    private companion object { const val KEY = "theme_mode" }
}

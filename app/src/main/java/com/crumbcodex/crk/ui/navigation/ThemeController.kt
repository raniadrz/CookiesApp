package com.crumbcodex.crk.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.crumbcodex.crk.data.prefs.ThemeMode

class ThemeController(val mode: ThemeMode, val onChange: (ThemeMode) -> Unit)

val LocalThemeController = staticCompositionLocalOf { ThemeController(ThemeMode.SYSTEM) {} }

@Composable
fun ThemeMenuButton() {
    val controller = LocalThemeController.current
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) { Icon(Icons.Rounded.Brightness6, contentDescription = "Theme") }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            ThemeMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(mode.label) },
                    onClick = { controller.onChange(mode); open = false },
                    trailingIcon = { if (mode == controller.mode) Icon(Icons.Rounded.Check, null) },
                )
            }
        }
    }
}

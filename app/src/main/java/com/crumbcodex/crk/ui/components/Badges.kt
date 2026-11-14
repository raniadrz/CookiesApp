package com.crumbcodex.crk.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.crumbcodex.crk.domain.model.CookieClass
import com.crumbcodex.crk.domain.model.Element
import com.crumbcodex.crk.domain.model.Position
import com.crumbcodex.crk.domain.model.Rarity
import com.crumbcodex.crk.ui.theme.LocalIsDark
import com.crumbcodex.crk.ui.util.color
import com.crumbcodex.crk.ui.util.icon

@Composable
fun TagPill(text: String, color: Color, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    val textColor = if (LocalIsDark.current) lerp(color, Color.White, 0.55f) else lerp(color, Color.Black, 0.45f)
    GlassPill(modifier, color = color) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (icon != null) Icon(icon, null, tint = textColor, modifier = Modifier.size(14.dp))
            Text(text, style = MaterialTheme.typography.labelMedium, color = textColor, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

@Composable
fun RarityBadge(rarity: Rarity, modifier: Modifier = Modifier) = TagPill(rarity.label, rarity.color, modifier)

@Composable
fun ElementBadge(element: Element, modifier: Modifier = Modifier) = TagPill(element.label, element.color, modifier, element.icon)

@Composable
fun ClassBadge(cookieClass: CookieClass, modifier: Modifier = Modifier) =
    TagPill(cookieClass.label, cookieClass.color, modifier, cookieClass.icon)

@Composable
fun PositionBadge(position: Position, modifier: Modifier = Modifier) = TagPill(position.label, position.color, modifier)

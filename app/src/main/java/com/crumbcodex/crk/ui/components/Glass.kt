package com.crumbcodex.crk.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.crumbcodex.crk.ui.theme.LocalIsDark
import com.crumbcodex.crk.ui.theme.Lavender
import com.crumbcodex.crk.ui.theme.Mint
import com.crumbcodex.crk.ui.theme.RosePink
import com.crumbcodex.crk.ui.theme.Violet

/** Soft pastel "aurora" backdrop that gives glass cards something to frost over. */
@Composable
fun AppBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val dark = LocalIsDark.current
    val base = MaterialTheme.colorScheme.background
    val a = if (dark) 0.28f else 0.45f
    Box(
        modifier
            .fillMaxSize()
            .background(base)
            .drawBehind {
                val w = size.width
                val h = size.height
                fun blob(c: Color, x: Float, y: Float, r: Float) = drawCircle(
                    Brush.radialGradient(listOf(c.copy(alpha = a), Color.Transparent), center = Offset(x, y), radius = r),
                    radius = r, center = Offset(x, y),
                )
                blob(Lavender, w * 0.1f, h * 0.05f, w * 0.75f)
                blob(RosePink, w * 0.95f, h * 0.22f, w * 0.65f)
                blob(Violet, w * 0.2f, h * 0.75f, w * 0.7f)
                blob(Mint, w * 0.9f, h * 0.95f, w * 0.5f)
            },
        content = content,
    )
}

@Composable
fun glassColor(): Color {
    val dark = LocalIsDark.current
    return if (dark) Color(0xFF231C33).copy(alpha = 0.62f) else Color.White.copy(alpha = 0.62f)
}

@Composable
fun glassBorder(): BorderStroke {
    val dark = LocalIsDark.current
    return BorderStroke(
        1.dp,
        Brush.linearGradient(
            if (dark) listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.04f))
            else listOf(Color.White.copy(alpha = 0.95f), Lavender.copy(alpha = 0.35f)),
        ),
    )
}

/** Frosted-glass card: translucent fill, luminous gradient edge, large rounded corners. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(24.dp),
    contentPadding: PaddingValues = PaddingValues(16.dp),
    tint: Color? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val color = tint?.let { it.copy(alpha = if (LocalIsDark.current) 0.22f else 0.18f) } ?: glassColor()
    if (onClick != null) {
        Surface(onClick = onClick, modifier = modifier, shape = shape, color = color, border = glassBorder()) {
            Column(Modifier.padding(contentPadding), content = content)
        }
    } else {
        Surface(modifier = modifier, shape = shape, color = color, border = glassBorder()) {
            Column(Modifier.padding(contentPadding), content = content)
        }
    }
}

/** Small glass pill used for chips/badges. */
@Composable
fun GlassPill(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    corner: Dp = 50.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier
            .background(color.copy(alpha = if (LocalIsDark.current) 0.28f else 0.16f), RoundedCornerShape(corner))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        content = content,
    )
}

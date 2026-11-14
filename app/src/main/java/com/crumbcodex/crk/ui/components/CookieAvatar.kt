package com.crumbcodex.crk.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.crumbcodex.crk.domain.model.Cookie
import com.crumbcodex.crk.ui.util.color
import com.crumbcodex.crk.ui.util.icon
import com.crumbcodex.crk.ui.util.toColors

/**
 * Circular cookie portrait. Always renders the procedural mascot; if the seed JSON provides an
 * `imageUrl` (e.g. your own artwork), it is loaded on top with Coil and the mascot stays as the
 * placeholder / error fallback.
 */
@Composable
fun CookieAvatar(
    cookie: Cookie,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    showElement: Boolean = true,
    showRarityRing: Boolean = true,
) {
    val palette = remember(cookie.id) { cookie.palette.toColors() }
    val bg = cookie.elements.firstOrNull()?.color ?: MaterialTheme.colorScheme.primary
    val ring = cookie.rarity.color
    Box(modifier.size(size)) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .then(
                    if (showRarityRing) Modifier.border(
                        width = (size.value * 0.045f).dp,
                        brush = Brush.sweepGradient(listOf(ring, Color.White.copy(alpha = 0.8f), ring, ring.copy(alpha = 0.6f), ring)),
                        shape = CircleShape,
                    ) else Modifier,
                ),
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawCookieMascot(palette, cookie.rarity, bg, cookie.id.hashCode())
            }
            if (cookie.imageUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(cookie.imageUrl).crossfade(true).build(),
                    contentDescription = cookie.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        val element = cookie.elements.firstOrNull()
        if (showElement && element != null && size >= 40.dp) {
            val badge = size * 0.32f
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 2.dp, y = 2.dp)
                    .size(badge)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(element.color),
                contentAlignment = Alignment.Center,
            ) {
                Icon(element.icon, contentDescription = element.label, tint = Color.White, modifier = Modifier.size(badge * 0.62f))
            }
        }
    }
}

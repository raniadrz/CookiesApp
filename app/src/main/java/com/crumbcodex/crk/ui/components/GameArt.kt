package com.crumbcodex.crk.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crumbcodex.crk.domain.model.BeascuitGrade
import com.crumbcodex.crk.domain.model.CandyKind
import com.crumbcodex.crk.domain.model.Rarity
import com.crumbcodex.crk.domain.model.SynergyTag
import com.crumbcodex.crk.domain.model.ToppingType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/*
 * All artwork in the app is original and drawn procedurally with Compose Canvas, so the app
 * ships without any third-party game assets. Each cookie gets a unique gingerbread mascot
 * whose frosting/outfit colours come from its seed "palette" and whose accessory reflects
 * its rarity tier.
 */

private val Dough = Color(0xFFE2A66B)
private val DoughShade = Color(0xFFC4844A)
private val Icing = Color(0xFFFFFBF5)
private val Ink = Color(0xFF3B2A28)
private val Blush = Color(0xFFFF8FA8)

fun DrawScope.drawCookieMascot(
    palette: List<Color>,
    rarity: Rarity,
    background: Color,
    seed: Int,
    drawBackground: Boolean = true,
) {
    val s = size.minDimension
    val cx = size.width / 2f
    val top = (size.height - s) / 2f
    fun p(x: Float, y: Float) = Offset(cx + (x - 0.5f) * s, top + y * s)

    val frosting = palette.getOrElse(0) { Color(0xFFB9A7FF) }
    val outfit = palette.getOrElse(1) { Color(0xFFFF9EBB) }

    if (drawBackground) {
        drawCircle(
            brush = Brush.radialGradient(
                listOf(lerp(background, Color.White, 0.55f), background, lerp(background, Color.Black, 0.25f)),
                center = p(0.4f, 0.35f), radius = s * 0.75f,
            ),
            radius = s / 2f, center = Offset(cx, top + s / 2f),
        )
        // Sparkles
        val rnd = Random(seed)
        repeat(6) {
            val sp = p(0.12f + rnd.nextFloat() * 0.76f, 0.08f + rnd.nextFloat() * 0.35f)
            drawSparkle(sp, s * (0.02f + rnd.nextFloat() * 0.025f), Color.White.copy(alpha = 0.75f))
        }
    }

    val limb = s * 0.12f
    // Legs
    drawLine(DoughShade, p(0.43f, 0.74f), p(0.38f, 0.9f), strokeWidth = limb, cap = StrokeCap.Round)
    drawLine(DoughShade, p(0.57f, 0.74f), p(0.62f, 0.9f), strokeWidth = limb, cap = StrokeCap.Round)
    drawLine(Dough, p(0.43f, 0.74f), p(0.385f, 0.885f), strokeWidth = limb * 0.86f, cap = StrokeCap.Round)
    drawLine(Dough, p(0.57f, 0.74f), p(0.615f, 0.885f), strokeWidth = limb * 0.86f, cap = StrokeCap.Round)
    // Arms
    drawLine(Dough, p(0.38f, 0.58f), p(0.24f, 0.68f), strokeWidth = limb, cap = StrokeCap.Round)
    drawLine(Dough, p(0.62f, 0.58f), p(0.76f, 0.66f), strokeWidth = limb, cap = StrokeCap.Round)
    // Icing cuffs
    drawLine(Icing, p(0.27f, 0.645f), p(0.29f, 0.675f), strokeWidth = s * 0.02f, cap = StrokeCap.Round)
    drawLine(Icing, p(0.73f, 0.63f), p(0.71f, 0.66f), strokeWidth = s * 0.02f, cap = StrokeCap.Round)
    // Body (outfit)
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(lerp(outfit, Color.White, 0.2f), outfit), startY = top + 0.5f * s, endY = top + 0.8f * s),
        topLeft = p(0.35f, 0.5f), size = Size(s * 0.3f, s * 0.28f), cornerRadius = CornerRadius(s * 0.1f),
    )
    // Buttons
    drawCircle(Icing, s * 0.018f, p(0.5f, 0.6f))
    drawCircle(Icing, s * 0.018f, p(0.5f, 0.68f))
    // Head
    val headC = p(0.5f, 0.37f)
    val headR = s * 0.19f
    drawCircle(DoughShade, headR * 1.03f, headC + Offset(0f, s * 0.008f))
    drawCircle(Brush.radialGradient(listOf(lerp(Dough, Color.White, 0.25f), Dough), center = headC - Offset(headR * 0.3f, headR * 0.3f), radius = headR * 1.4f), headR, headC)
    // Frosting "hair": a wavy cap over the top of the head
    val hair = Path().apply {
        moveTo(headC.x - headR * 1.02f, headC.y - headR * 0.05f)
        cubicTo(headC.x - headR * 1.1f, headC.y - headR * 1.25f, headC.x + headR * 1.1f, headC.y - headR * 1.25f, headC.x + headR * 1.02f, headC.y - headR * 0.05f)
        val waves = 4
        for (i in waves downTo 1) {
            val x1 = headC.x - headR + (2 * headR) * (i - 0.5f) / waves
            val x2 = headC.x - headR + (2 * headR) * (i - 1f) / waves
            quadraticTo(x1, headC.y - headR * (if (i % 2 == 0) 0.2f else 0.45f), x2, headC.y - headR * 0.3f)
        }
        close()
    }
    drawPath(hair, Brush.verticalGradient(listOf(lerp(frosting, Color.White, 0.35f), frosting), startY = headC.y - headR * 1.2f, endY = headC.y))
    // Face
    val eyeY = headC.y + headR * 0.12f
    drawOval(Ink, topLeft = Offset(headC.x - headR * 0.45f, eyeY - headR * 0.14f), size = Size(headR * 0.18f, headR * 0.28f))
    drawOval(Ink, topLeft = Offset(headC.x + headR * 0.27f, eyeY - headR * 0.14f), size = Size(headR * 0.18f, headR * 0.28f))
    drawCircle(Color.White, headR * 0.045f, Offset(headC.x - headR * 0.34f, eyeY - headR * 0.06f))
    drawCircle(Color.White, headR * 0.045f, Offset(headC.x + headR * 0.38f, eyeY - headR * 0.06f))
    drawCircle(Blush.copy(alpha = 0.6f), headR * 0.13f, Offset(headC.x - headR * 0.6f, eyeY + headR * 0.28f))
    drawCircle(Blush.copy(alpha = 0.6f), headR * 0.13f, Offset(headC.x + headR * 0.6f, eyeY + headR * 0.28f))
    drawArc(
        Ink, startAngle = 20f, sweepAngle = 140f, useCenter = false,
        topLeft = Offset(headC.x - headR * 0.22f, eyeY + headR * 0.05f), size = Size(headR * 0.44f, headR * 0.36f),
        style = Stroke(width = headR * 0.08f, cap = StrokeCap.Round),
    )

    drawRarityAccessory(rarity, headC, headR, frosting, outfit)
}

private fun DrawScope.drawRarityAccessory(rarity: Rarity, headC: Offset, headR: Float, frosting: Color, outfit: Color) {
    when (rarity) {
        Rarity.BEAST -> {
            val horn = lerp(outfit, Color.Black, 0.35f)
            listOf(-1f, 1f).forEach { side ->
                val base = Offset(headC.x + side * headR * 0.55f, headC.y - headR * 0.8f)
                val path = Path().apply {
                    moveTo(base.x - side * headR * 0.22f, base.y + headR * 0.1f)
                    quadraticTo(base.x + side * headR * 0.45f, base.y - headR * 0.35f, base.x + side * headR * 0.35f, base.y - headR * 0.75f)
                    quadraticTo(base.x + side * headR * 0.1f, base.y - headR * 0.25f, base.x + side * headR * 0.2f, base.y + headR * 0.12f)
                    close()
                }
                drawPath(path, horn)
            }
        }
        Rarity.ANCIENT -> {
            drawOval(
                color = Color(0xFFFFE08A), topLeft = Offset(headC.x - headR * 0.75f, headC.y - headR * 1.55f),
                size = Size(headR * 1.5f, headR * 0.4f), style = Stroke(width = headR * 0.12f),
            )
        }
        Rarity.LEGENDARY -> drawStar(Offset(headC.x + headR * 0.75f, headC.y - headR * 0.95f), headR * 0.32f, Color(0xFFFFE08A))
        Rarity.DRAGON -> {
            val c = lerp(outfit, Color(0xFFFF6B4A), 0.5f)
            listOf(-1f, 1f).forEach { side ->
                drawLine(c, Offset(headC.x + side * headR * 0.4f, headC.y - headR * 0.85f), Offset(headC.x + side * headR * 0.75f, headC.y - headR * 1.4f), strokeWidth = headR * 0.18f, cap = StrokeCap.Round)
            }
        }
        Rarity.SUPER_EPIC -> {
            val crown = Path().apply {
                val y = headC.y - headR * 0.95f
                moveTo(headC.x - headR * 0.45f, y)
                lineTo(headC.x - headR * 0.5f, y - headR * 0.45f)
                lineTo(headC.x - headR * 0.2f, y - headR * 0.2f)
                lineTo(headC.x, y - headR * 0.55f)
                lineTo(headC.x + headR * 0.2f, y - headR * 0.2f)
                lineTo(headC.x + headR * 0.5f, y - headR * 0.45f)
                lineTo(headC.x + headR * 0.45f, y)
                close()
            }
            drawPath(crown, Color(0xFFFFD36B))
            drawCircle(Color(0xFFFF8FC7), headR * 0.07f, Offset(headC.x, headC.y - headR * 1.13f))
        }
        Rarity.EPIC -> {
            val bowC = Offset(headC.x + headR * 0.7f, headC.y - headR * 0.75f)
            val bow = lerp(frosting, outfit, 0.5f)
            drawCircle(bow, headR * 0.2f, bowC + Offset(-headR * 0.18f, 0f))
            drawCircle(bow, headR * 0.2f, bowC + Offset(headR * 0.18f, 0f))
            drawCircle(lerp(bow, Color.White, 0.4f), headR * 0.09f, bowC)
        }
        Rarity.SPECIAL -> drawSparkle(Offset(headC.x + headR * 0.8f, headC.y - headR * 0.9f), headR * 0.3f, Color(0xFF4ECDC4))
        Rarity.RARE, Rarity.COMMON -> Unit
    }
}

fun DrawScope.drawSparkle(center: Offset, r: Float, color: Color) {
    val path = Path().apply {
        moveTo(center.x, center.y - r)
        quadraticTo(center.x, center.y, center.x + r, center.y)
        quadraticTo(center.x, center.y, center.x, center.y + r)
        quadraticTo(center.x, center.y, center.x - r, center.y)
        quadraticTo(center.x, center.y, center.x, center.y - r)
        close()
    }
    drawPath(path, color)
}

fun DrawScope.drawStar(center: Offset, r: Float, color: Color) {
    val path = Path()
    for (i in 0 until 10) {
        val radius = if (i % 2 == 0) r else r * 0.45f
        val angle = -PI / 2 + i * PI / 5
        val pt = Offset(center.x + (radius * cos(angle)).toFloat(), center.y + (radius * sin(angle)).toFloat())
        if (i == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
    }
    path.close()
    drawPath(path, color)
}

/** A gem-like topping piece tinted by its topping type, with optional piece count badge. */
@Composable
fun ToppingArt(type: ToppingType, modifier: Modifier = Modifier, size: Dp = 40.dp, count: Int? = null) {
    val color = Color(type.colorArgb)
    Box(modifier.size(size)) {
        Canvas(Modifier.size(size)) {
            val s = this.size.minDimension
            val c = center
            val r = s * 0.42f
            val hex = Path().apply {
                for (i in 0 until 6) {
                    val a = PI / 6 + i * PI / 3
                    val pt = Offset(c.x + (r * cos(a)).toFloat(), c.y + (r * sin(a)).toFloat())
                    if (i == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
                }
                close()
            }
            drawPath(hex, Brush.linearGradient(listOf(lerp(color, Color.White, 0.45f), color, lerp(color, Color.Black, 0.25f)), start = Offset(0f, 0f), end = Offset(s, s)))
            drawPath(hex, Color.White.copy(alpha = 0.5f), style = Stroke(width = s * 0.04f))
            when (type) {
                ToppingType.SEARING_RASPBERRY -> repeat(6) { i ->
                    val a = i * PI / 3
                    drawCircle(lerp(color, Color.White, 0.3f), s * 0.07f, Offset(c.x + (s * 0.13f * cos(a)).toFloat(), c.y + (s * 0.13f * sin(a)).toFloat()))
                }
                ToppingType.SWIFT_CHOCOLATE -> drawRoundRect(lerp(color, Color.Black, 0.3f), Offset(c.x - s * 0.17f, c.y - s * 0.12f), Size(s * 0.34f, s * 0.24f), CornerRadius(s * 0.04f))
                ToppingType.SOLID_ALMOND -> drawOval(lerp(color, Color(0xFF8C6A4F), 0.4f), Offset(c.x - s * 0.1f, c.y - s * 0.17f), Size(s * 0.2f, s * 0.34f))
                ToppingType.JUICY_APPLE_JELLY -> drawCircle(Color.White.copy(alpha = 0.55f), s * 0.14f, c)
                ToppingType.BOUNCY_CARAMEL -> drawArc(Color.White.copy(alpha = 0.7f), 200f, 220f, false, Offset(c.x - s * 0.15f, c.y - s * 0.15f), Size(s * 0.3f, s * 0.3f), style = Stroke(s * 0.05f, cap = StrokeCap.Round))
                ToppingType.HEALTHY_PEANUT -> {
                    drawCircle(lerp(color, Color.White, 0.25f), s * 0.1f, Offset(c.x, c.y - s * 0.08f))
                    drawCircle(lerp(color, Color.White, 0.25f), s * 0.1f, Offset(c.x, c.y + s * 0.08f))
                }
                ToppingType.HARD_WALNUT -> drawCircle(lerp(color, Color.Black, 0.25f), s * 0.15f, c, style = Stroke(s * 0.05f))
            }
            drawSparkle(Offset(c.x - s * 0.18f, c.y - s * 0.2f), s * 0.07f, Color.White.copy(alpha = 0.85f))
        }
        if (count != null) {
            Box(
                Modifier.align(Alignment.BottomEnd).size(size * 0.45f).clip(CircleShape).then(Modifier),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.size(size * 0.45f)) { drawCircle(Color(0xFF2B2140)) }
                Text("$count", color = Color.White, fontSize = (size.value * 0.22f).sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** A paw-print biscuit tinted with the element colour; frame colour follows the grade. */
@Composable
fun BeascuitArt(tint: Color, grade: BeascuitGrade, modifier: Modifier = Modifier, size: Dp = 48.dp) {
    val frame = when (grade) {
        BeascuitGrade.LEGENDARY -> Color(0xFFFFD36B)
        BeascuitGrade.EPIC -> Color(0xFFC77DFF)
        BeascuitGrade.RARE -> Color(0xFF5EA8FF)
        BeascuitGrade.COMMON -> Color(0xFFB8B8C0)
    }
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val c = center
        drawRoundRect(
            brush = Brush.linearGradient(listOf(lerp(tint, Color.White, 0.6f), lerp(tint, Color.White, 0.2f))),
            cornerRadius = CornerRadius(s * 0.28f),
        )
        drawRoundRect(frame, cornerRadius = CornerRadius(s * 0.28f), style = Stroke(width = s * 0.06f))
        val biscuit = Color(0xFFE9B97A)
        val edge = Color(0xFFC4844A)
        // pad
        drawOval(edge, Offset(c.x - s * 0.22f, c.y - s * 0.02f), Size(s * 0.44f, s * 0.34f))
        drawOval(biscuit, Offset(c.x - s * 0.2f, c.y), Size(s * 0.4f, s * 0.3f))
        // toes
        listOf(-0.24f to -0.08f, -0.09f to -0.2f, 0.09f to -0.2f, 0.24f to -0.08f).forEach { (dx, dy) ->
            drawCircle(edge, s * 0.085f, Offset(c.x + dx * s, c.y + dy * s))
            drawCircle(biscuit, s * 0.072f, Offset(c.x + dx * s, c.y + dy * s))
        }
        drawCircle(tint, s * 0.06f, Offset(c.x, c.y + s * 0.15f))
        if (grade == BeascuitGrade.LEGENDARY) drawSparkle(Offset(c.x + s * 0.3f, c.y - s * 0.3f), s * 0.09f, Color.White)
    }
}

/** Treasure chest-gem art, its colour derived from the first synergy tag. */
@Composable
fun TreasureArt(tag: SynergyTag?, modifier: Modifier = Modifier, size: Dp = 48.dp) {
    val gem = when (tag) {
        SynergyTag.BURST -> Color(0xFFE5486A)
        SynergyTag.COOLDOWN -> Color(0xFF8A5A44)
        SynergyTag.SUSTAIN -> Color(0xFF3DBE8B)
        SynergyTag.TANK -> Color(0xFF6C8EA4)
        SynergyTag.CRIT -> Color(0xFFF4C542)
        SynergyTag.REVIVE -> Color(0xFFFF9EBB)
        SynergyTag.DEBUFF -> Color(0xFF7A62B0)
        SynergyTag.SHIELD -> Color(0xFF7FD6F5)
        null -> Color(0xFFB9A7FF)
    }
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val wood = Color(0xFFB0703C)
        drawRoundRect(wood, Offset(s * 0.12f, s * 0.4f), Size(s * 0.76f, s * 0.46f), CornerRadius(s * 0.08f))
        drawRoundRect(lerp(wood, Color.White, 0.2f), Offset(s * 0.1f, s * 0.28f), Size(s * 0.8f, s * 0.2f), CornerRadius(s * 0.1f))
        drawRect(Color(0xFFFFD36B), Offset(s * 0.12f, s * 0.46f), Size(s * 0.76f, s * 0.05f))
        val g = Path().apply {
            moveTo(s * 0.5f, s * 0.08f); lineTo(s * 0.68f, s * 0.26f); lineTo(s * 0.5f, s * 0.5f); lineTo(s * 0.32f, s * 0.26f); close()
        }
        drawPath(g, Brush.linearGradient(listOf(lerp(gem, Color.White, 0.5f), gem)))
        drawSparkle(Offset(s * 0.42f, s * 0.2f), s * 0.05f, Color.White)
        drawRoundRect(Color(0xFFFFD36B), Offset(s * 0.44f, s * 0.52f), Size(s * 0.12f, s * 0.14f), CornerRadius(s * 0.03f))
    }
}

/** Candy / jam jar art for Magic Candy and Crystal Jam sections. */
@Composable
fun CandyArt(kind: CandyKind, tint: Color, modifier: Modifier = Modifier, size: Dp = 48.dp) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        when (kind) {
            CandyKind.CRYSTAL_JAM -> {
                drawRoundRect(Color.White.copy(alpha = 0.6f), Offset(s * 0.22f, s * 0.25f), Size(s * 0.56f, s * 0.62f), CornerRadius(s * 0.14f))
                drawRoundRect(Brush.verticalGradient(listOf(lerp(tint, Color.White, 0.3f), tint), startY = s * 0.4f, endY = s * 0.85f), Offset(s * 0.26f, s * 0.4f), Size(s * 0.48f, s * 0.44f), CornerRadius(s * 0.1f))
                drawRoundRect(Color(0xFFFFD36B), Offset(s * 0.2f, s * 0.14f), Size(s * 0.6f, s * 0.14f), CornerRadius(s * 0.05f))
                rotate(45f, Offset(s * 0.5f, s * 0.6f)) {
                    drawRect(Color.White.copy(alpha = 0.8f), Offset(s * 0.44f, s * 0.54f), Size(s * 0.12f, s * 0.12f))
                }
            }
            else -> {
                val c = Offset(s * 0.5f, s * 0.5f)
                drawLine(tint, Offset(s * 0.12f, s * 0.3f), Offset(s * 0.3f, s * 0.42f), strokeWidth = s * 0.16f, cap = StrokeCap.Round)
                drawLine(tint, Offset(s * 0.88f, s * 0.7f), Offset(s * 0.7f, s * 0.58f), strokeWidth = s * 0.16f, cap = StrokeCap.Round)
                drawCircle(Brush.radialGradient(listOf(lerp(tint, Color.White, 0.6f), tint), center = c - Offset(s * 0.06f, s * 0.06f), radius = s * 0.3f), s * 0.24f, c)
                drawArc(Color.White.copy(alpha = 0.8f), 200f, 120f, false, Offset(s * 0.34f, s * 0.34f), Size(s * 0.32f, s * 0.32f), style = Stroke(s * 0.04f, cap = StrokeCap.Round))
                if (kind == CandyKind.NONE) drawLine(Color(0xFF9AA6B2), Offset(s * 0.15f, s * 0.85f), Offset(s * 0.85f, s * 0.15f), strokeWidth = s * 0.06f, cap = StrokeCap.Round)
            }
        }
    }
}

/** Stable small int from a string, used to seed deterministic sparkles. */
@Composable
fun rememberSeed(key: String): Int = remember(key) { key.hashCode() }

internal fun Rect.inset(f: Float) = Rect(left + f, top + f, right - f, bottom - f)

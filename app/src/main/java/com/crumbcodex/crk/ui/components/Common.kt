package com.crumbcodex.crk.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.crumbcodex.crk.ui.theme.Gold

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    subtitle: String? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Box(
                Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) }
            Spacer(Modifier.width(10.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        action?.invoke()
    }
}

@Composable
fun EmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    art: @Composable () -> Unit,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        art()
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        if (actionLabel != null && onAction != null) TextButton(onClick = onAction) { Text(actionLabel) }
    }
}

@Composable
fun StatProgress(progress: Float, color: Color, modifier: Modifier = Modifier) {
    LinearProgressIndicator(
        progress = { progress.coerceIn(0f, 1f) },
        modifier = modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
        color = color,
        trackColor = color.copy(alpha = 0.18f),
        strokeCap = StrokeCap.Round,
        gapSize = 0.dp,
        drawStopIndicator = {},
    )
}

@Composable
fun StarRow(stars: Int, max: Int, modifier: Modifier = Modifier, size: Int = 20, onChange: ((Int) -> Unit)? = null) {
    Row(modifier.semantics { contentDescription = "$stars of $max stars" }) {
        for (i in 1..max) {
            val filled = i <= stars
            Icon(
                if (filled) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                contentDescription = null,
                tint = if (filled) Gold else MaterialTheme.colorScheme.outline,
                modifier = Modifier
                    .size(size.dp)
                    .then(if (onChange != null) Modifier.clickable { onChange(if (stars == i) i - 1 else i) } else Modifier),
            )
        }
    }
}

@Composable
fun NumberStepper(
    label: String,
    value: Int,
    range: IntRange,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    step: Int = 1,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        FilledTonalIconButton(onClick = { onChange((value - step).coerceIn(range)) }, enabled = value > range.first) {
            Icon(Icons.Rounded.Remove, contentDescription = "Decrease $label")
        }
        Text(
            "$value", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center, modifier = Modifier.width(48.dp),
        )
        FilledTonalIconButton(onClick = { onChange((value + step).coerceIn(range)) }, enabled = value < range.last) {
            Icon(Icons.Rounded.Add, contentDescription = "Increase $label")
        }
    }
}

@Composable
fun KeyValueRow(key: String, value: String, modifier: Modifier = Modifier, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Row(modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(key, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = valueColor)
    }
}

@Composable
fun BulletList(items: List<String>, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items.forEach {
            Row {
                Text("•  ", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

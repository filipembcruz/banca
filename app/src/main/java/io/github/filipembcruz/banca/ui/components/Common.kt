package io.github.filipembcruz.banca.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import io.github.filipembcruz.banca.ui.theme.LocalExtras
import kotlin.math.abs

/** Favicon of the source in a circle, with a coloured initial as fallback. */
@Composable
fun SourceIcon(name: String, favicon: String?, size: Dp = 18.dp, modifier: Modifier = Modifier) {
    val color = remember(name) { avatarColor(name) }
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            name.firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "•",
            color = Color.White,
            fontSize = (size.value * 0.55f).sp,
            fontWeight = FontWeight.Bold,
        )
        if (favicon != null) {
            AsyncImage(
                model = favicon,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(Color.White),
            )
        }
    }
}

private val avatarPalette = listOf(
    Color(0xFF3B5BFD), Color(0xFF8A5CF6), Color(0xFFE5484D), Color(0xFF1FA463),
    Color(0xFFF08C00), Color(0xFF0EA5E9), Color(0xFFDB2777), Color(0xFF475569),
)

fun avatarColor(key: String): Color = avatarPalette[abs(key.lowercase().hashCode()) % avatarPalette.size]

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, action: String? = null, onAction: () -> Unit = {}) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        if (action != null) {
            TextButton(onClick = onAction) { Text(action) }
        }
    }
}

/** Animated placeholder block. */
@Composable
fun Modifier.shimmer(shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(8.dp)): Modifier {
    val dark = LocalExtras.current.dark
    val base = if (dark) Color(0xFF232836) else Color(0xFFE9ECF3)
    val hi = if (dark) Color(0xFF2E3445) else Color(0xFFF6F7FB)
    val t = rememberInfiniteTransition(label = "shimmer")
    val x by t.animateFloat(
        initialValue = -1f, targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmerX",
    )
    return this
        .clip(shape)
        .background(
            Brush.linearGradient(
                colors = listOf(base, hi, base),
                start = Offset(x * 600f, 0f),
                end = Offset(x * 600f + 400f, 200f),
            )
        )
}

@Composable
fun SkeletonRow(modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(vertical = 14.dp)) {
        Column(Modifier.weight(1f)) {
            Box(Modifier.size(110.dp, 10.dp).shimmer())
            Spacer(Modifier.height(10.dp))
            Box(Modifier.fillMaxWidth().height(14.dp).shimmer())
            Spacer(Modifier.height(6.dp))
            Box(Modifier.fillMaxWidth(0.75f).height(14.dp).shimmer())
        }
        Spacer(Modifier.size(14.dp))
        Box(Modifier.size(88.dp).shimmer(RoundedCornerShape(14.dp)))
    }
}

@Composable
fun SkeletonHero(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(220.dp).shimmer(RoundedCornerShape(24.dp)))
}

/** Small line chart; draws from left to right when [progress] animates from 0 to 1. */
@Composable
fun Sparkline(
    values: List<Double>,
    color: Color,
    modifier: Modifier = Modifier,
    progress: Float = 1f,
    fill: Boolean = false,
    strokeWidth: Dp = 1.6.dp,
) {
    if (values.size < 2) {
        Box(modifier)
        return
    }
    Canvas(modifier) {
        val min = values.min()
        val max = values.max()
        val span = (max - min).takeIf { it > 0 } ?: 1.0
        val stepX = size.width / (values.size - 1)
        val path = Path()
        val count = (values.size * progress).toInt().coerceIn(2, values.size)
        for (i in 0 until count) {
            val x = i * stepX
            val y = size.height - ((values[i] - min) / span * size.height).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        if (fill) {
            val area = Path().apply {
                addPath(path)
                lineTo((count - 1) * stepX, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(area, Brush.verticalGradient(listOf(color.copy(alpha = 0.28f), color.copy(alpha = 0f))))
        }
        drawPath(path, color, style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: () -> Unit = {},
) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp))
        }
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (action != null) {
            TextButton(onClick = onAction) { Text(action) }
        }
    }
}

package io.github.filipembcruz.banca.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import io.github.filipembcruz.banca.ui.theme.LocalExtras
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Chips that wrap onto lines with equal horizontal and vertical gaps. */
@Composable
fun ChipFlow(modifier: Modifier = Modifier, spacing: Dp = 8.dp, content: @Composable FlowRowScope.() -> Unit) {
    // Chips keep a 48dp touch box by default, which makes row gaps larger than column gaps.
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
        FlowRow(
            modifier,
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalArrangement = Arrangement.spacedBy(spacing),
            content = content,
        )
    }
}

/** Full-screen picture with pinch and double-tap zoom. A tap or Back closes it. */
@Composable
fun ImageViewer(url: String, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        val scope = rememberCoroutineScope()
        val scale = remember { Animatable(1f) }
        val offset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { if (scale.value <= 1.05f) onDismiss() },
                        onDoubleTap = { tap ->
                            scope.launch {
                                if (scale.value > 1.05f) {
                                    launch { offset.animateTo(Offset.Zero) }
                                    scale.animateTo(1f)
                                } else {
                                    val center = Offset(size.width / 2f, size.height / 2f)
                                    launch { offset.animateTo((center - tap) * 1.5f) }
                                    scale.animateTo(2.5f)
                                }
                            }
                        },
                    )
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scope.launch {
                            val s = (scale.value * zoom).coerceIn(1f, 5f)
                            scale.snapTo(s)
                            offset.snapTo(if (s > 1f) offset.value + pan else Offset.Zero)
                        }
                    }
                },
        ) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                        translationX = offset.value.x
                        translationY = offset.value.y
                    },
            )
            IconButton(
                onClick = onDismiss,
                colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.45f), contentColor = Color.White),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(12.dp),
            ) { Icon(Icons.Rounded.Close, null) }
        }
    }
}

/**
 * Animated weather backdrop drawn over the card gradient: sun rays, moon and stars,
 * drifting clouds, rain, snow, fog and lightning, depending on the WMO code.
 */
@Composable
fun WeatherScene(code: Int, day: Boolean, modifier: Modifier = Modifier, compact: Boolean = false) {
    val reduced = LocalExtras.current.reducedMotion
    val t = rememberInfiniteTransition(label = "wx")
    val clock by t.animateFloat(
        0f, 1f, infiniteRepeatable(tween(12_000, easing = LinearEasing), RepeatMode.Restart), label = "clock",
    )
    val time = if (reduced) 0.35f else clock

    // In the compact card the scene stays in the middle and softer, away from the text and temperature.
    Canvas(modifier.graphicsLayer { alpha = if (compact) 0.75f else 0.85f }) {
        // Everything is sized from a band at the top of the card, so the expanded card's
        // forecast row and the temperature stay clear.
        val unit = minOf(size.height, size.width * 0.3f)
        val sx = 0.6f
        val sy = if (compact) 0.5f else unit * 0.5f / size.height
        val rain = code in 51..67 || code in 80..82 || code in 95..99
        val snow = code in 71..77 || code in 85..86
        val clouds = code in 1..3 || rain || snow || code in 45..48
        when {
            !day -> moon(time, sx, sy)
            code == 0 || code in 1..2 -> sun(time, sx, sy)
        }
        if (clouds) clouds(time, heavy = code == 3 || rain || snow, unit = unit)
        if (code in 45..48) fog(time)
        if (rain) rain(time, heavy = code in 63..67 || code in 81..82 || code >= 95)
        if (snow) snow(time)
        if (code >= 95) lightning(time)
    }
}

private fun DrawScope.sun(t: Float, sx: Float, sy: Float) {
    val c = Offset(size.width * sx, size.height * sy)
    val r = minOf(size.height, size.width * 0.3f) * 0.16f
    val pulse = 1f + 0.06f * sin(t * 2 * PI * 3).toFloat()
    drawCircle(
        Brush.radialGradient(listOf(Color(0x66FFF3B0), Color.Transparent), c, r * 3.2f * pulse),
        radius = r * 3.2f * pulse, center = c,
    )
    rotate(t * 360f, c) {
        for (i in 0 until 12) {
            val a = i * 30.0 * PI / 180
            val start = Offset(c.x + cos(a).toFloat() * r * 1.35f, c.y + sin(a).toFloat() * r * 1.35f)
            val end = Offset(c.x + cos(a).toFloat() * r * 1.9f, c.y + sin(a).toFloat() * r * 1.9f)
            drawLine(Color(0x66FFF7D6), start, end, strokeWidth = r * 0.16f, cap = StrokeCap.Round)
        }
    }
    drawCircle(Color(0x55FFF5C2), radius = r, center = c)
}

private fun DrawScope.moon(t: Float, sx: Float, sy: Float) {
    val c = Offset(size.width * sx, size.height * sy)
    val r = minOf(size.height, size.width * 0.3f) * 0.13f
    drawCircle(Brush.radialGradient(listOf(Color(0x33FFFFFF), Color.Transparent), c, r * 3f), radius = r * 3f, center = c)
    drawCircle(Color(0x66FFFFFF), radius = r, center = c)
    drawCircle(Color(0x33000000), radius = r * 0.85f, center = Offset(c.x + r * 0.45f, c.y - r * 0.2f))
    for (i in 0 until 14) {
        val x = ((i * 73) % 100) / 100f * size.width
        val y = ((i * 37) % 60) / 100f * size.height
        val twinkle = 0.25f + 0.35f * ((sin((t * 6 + i * 0.7f) * PI).toFloat() + 1f) / 2f)
        drawCircle(Color.White.copy(alpha = twinkle), radius = 1.6f + (i % 3), center = Offset(x, y))
    }
}

private fun DrawScope.cloud(center: Offset, w: Float, alpha: Float) {
    val c = Color.White.copy(alpha = alpha)
    drawCircle(c, w * 0.22f, Offset(center.x - w * 0.22f, center.y + w * 0.04f))
    drawCircle(c, w * 0.30f, Offset(center.x, center.y - w * 0.06f))
    drawCircle(c, w * 0.22f, Offset(center.x + w * 0.24f, center.y + w * 0.05f))
    drawRoundRect(c, Offset(center.x - w * 0.44f, center.y + w * 0.02f), Size(w * 0.88f, w * 0.24f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.12f))
}

private fun DrawScope.clouds(t: Float, heavy: Boolean, unit: Float) {
    val w = unit * 0.9f
    // Whole laps per cycle so the loop restarts without a jump.
    val lap = size.width + w
    val drift = t * lap
    val alpha = if (heavy) 0.14f else 0.11f
    cloud(Offset(((size.width * 0.6f + drift) % lap) - w / 2, unit * 0.32f), w, alpha)
    cloud(Offset(((size.width * 0.1f + drift * 2f) % lap) - w / 2, unit * 0.62f), w * 0.7f, alpha * 0.8f)
    if (heavy) cloud(Offset(((size.width * 0.9f + drift) % lap) - w / 2, unit * 0.14f), w * 0.8f, alpha)
}

private fun DrawScope.rain(t: Float, heavy: Boolean) {
    val drops = if (heavy) 46 else 28
    val len = size.height * 0.08f
    for (i in 0 until drops) {
        val x = ((i * 53 + 17) % 100) / 100f * size.width
        val speed = 5f + (i % 4)
        val y = ((t * speed + i * 0.137f) % 1f) * (size.height + len) - len
        drawLine(
            Color.White.copy(alpha = 0.32f),
            Offset(x, y), Offset(x - len * 0.25f, y + len),
            strokeWidth = 2.2f, cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.snow(t: Float) {
    for (i in 0 until 34) {
        val baseX = ((i * 61 + 11) % 100) / 100f * size.width
        val speed = 1f + (i % 3)
        val y = ((t * speed + i * 0.113f) % 1f) * size.height
        val x = baseX + sin((t * 8 + i) * PI).toFloat() * 10f
        drawCircle(Color.White.copy(alpha = 0.55f), radius = 2.5f + (i % 3), center = Offset(x, y))
    }
}

private fun DrawScope.fog(t: Float) {
    for (i in 0 until 4) {
        val y = size.height * (0.3f + i * 0.17f)
        val shift = sin((t * 2 + i * 0.5f) * PI).toFloat() * size.width * 0.08f
        drawRoundRect(
            Color.White.copy(alpha = 0.12f),
            Offset(-size.width * 0.1f + shift, y), Size(size.width * 1.2f, size.height * 0.07f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height * 0.035f),
        )
    }
}

private fun DrawScope.lightning(t: Float) {
    // Two short flashes in each 12 s cycle.
    val phase = (t * 4f) % 1f
    val flash = when {
        phase in 0.00f..0.03f -> 1f - phase / 0.03f
        phase in 0.06f..0.08f -> 0.6f
        else -> 0f
    }
    if (flash <= 0f) return
    drawRect(Color.White.copy(alpha = 0.22f * flash))
    val x = size.width * 0.62f
    val bolt = Path().apply {
        moveTo(x, 0f)
        lineTo(x - size.width * 0.05f, size.height * 0.38f)
        lineTo(x + size.width * 0.01f, size.height * 0.38f)
        lineTo(x - size.width * 0.04f, size.height * 0.78f)
    }
    drawPath(bolt, Color(0xFFFFF6C2).copy(alpha = 0.85f * flash), style = Stroke(width = 4f, cap = StrokeCap.Round))
}

package io.github.filipembcruz.banca.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.filipembcruz.banca.R
import io.github.filipembcruz.banca.core.Format
import io.github.filipembcruz.banca.data.model.MarketDef
import io.github.filipembcruz.banca.data.model.Markets
import io.github.filipembcruz.banca.data.model.Place
import io.github.filipembcruz.banca.data.model.Quote
import io.github.filipembcruz.banca.data.model.Weather
import io.github.filipembcruz.banca.ui.components.PageDots
import io.github.filipembcruz.banca.ui.components.Sparkline
import io.github.filipembcruz.banca.ui.components.WeatherText
import io.github.filipembcruz.banca.ui.components.shimmer
import io.github.filipembcruz.banca.ui.nav.LocalSettings
import io.github.filipembcruz.banca.ui.theme.LocalExtras

@Composable
fun WeatherCard(places: List<Place>, weather: Map<String, Weather>, onClick: () -> Unit, modifier: Modifier = Modifier) {
    if (places.isEmpty()) return
    val pager = rememberPagerState { places.size }
    Column(modifier) {
        HorizontalPager(state = pager, pageSpacing = 12.dp) { page ->
            val p = places[page]
            val w = weather[p.key]
            if (w == null) {
                Box(Modifier.fillMaxWidth().height(156.dp).shimmer(RoundedCornerShape(24.dp)))
            } else {
                WeatherPanel(w, onClick)
            }
        }
        if (places.size > 1) {
            PageDots(places.size, pager.currentPage, Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp))
        }
    }
}

@Composable
private fun WeatherPanel(w: Weather, onClick: () -> Unit) {
    val s = LocalSettings.current
    val dark = LocalExtras.current.dark
    val reduced = LocalExtras.current.reducedMotion
    val colors = remember(w.code, w.isDay, dark) { WeatherText.gradient(w.code, w.isDay, dark) }
    val temp = remember(w.temp) { Animatable(0f) }
    LaunchedEffect(w.temp) {
        temp.animateTo(w.temp.toFloat(), tween(if (reduced) 0 else 900, easing = FastOutSlowInEasing))
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(colors))
            .clickable(onClick = onClick)
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(w.place.name, style = MaterialTheme.typography.titleMedium, color = Color.White)
                Text(
                    stringResource(WeatherText.label(w.code)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                )
            }
            Icon(WeatherText.icon(w.code, w.isDay), null, tint = Color.White, modifier = Modifier.size(40.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                Format.temp(temp.value.toDouble(), s.fahrenheit),
                fontSize = 44.sp,
                fontWeight = FontWeight.Light,
                color = Color.White,
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.weather_feels, Format.temp(w.feelsLike, s.fahrenheit)),
                style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.85f),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Air, null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(Format.wind(w.wind, s.mph), style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.85f))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.WaterDrop, null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("${w.humidity}%", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.85f))
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            w.days.take(5).forEachIndexed { i, d ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (i == 0) stringResource(R.string.weather_today) else Format.dayName(d.date),
                        style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f),
                    )
                    Icon(WeatherText.icon(d.code), null, tint = Color.White, modifier = Modifier.padding(vertical = 4.dp).size(20.dp))
                    Text(
                        "${Format.temp(d.max, s.fahrenheit)} ${Format.temp(d.min, s.fahrenheit)}",
                        style = MaterialTheme.typography.labelSmall, color = Color.White,
                    )
                }
            }
        }
    }
}

@Composable
fun MarketStrip(ids: List<String>, quotes: Map<String, Quote>, onOpen: (MarketDef) -> Unit, modifier: Modifier = Modifier) {
    val defs = ids.mapNotNull { Markets.byId(it) }
    if (defs.isEmpty()) return
    LazyRow(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        items(defs, key = { it.id }) { m ->
            MarketChip(m, quotes[m.id], onClick = { onOpen(m) })
        }
    }
}

@Composable
private fun MarketChip(m: MarketDef, q: Quote?, onClick: () -> Unit) {
    val s = LocalSettings.current
    val extras = LocalExtras.current
    val draw = remember(m.id) { Animatable(0f) }
    LaunchedEffect(q?.updated) {
        if (q != null) draw.animateTo(1f, tween(if (extras.reducedMotion) 0 else 900, easing = FastOutSlowInEasing))
    }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = extras.card,
        tonalElevation = 0.dp,
        modifier = Modifier.width(156.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(m.label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (q == null) {
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth(0.7f).height(18.dp).shimmer())
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth().height(28.dp).shimmer())
            } else {
                val up = q.change >= 0
                val color = if (up) extras.up else extras.down
                AnimatedContent(
                    targetState = Format.number(q.price, m.decimals),
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "price",
                ) { text ->
                    Text(text, style = MaterialTheme.typography.titleLarge, maxLines = 1)
                }
                Text(
                    Format.change(q.changePct, q.change, s.showChangeAsPercent, m.decimals),
                    style = MaterialTheme.typography.labelMedium,
                    color = color,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(6.dp))
                Sparkline(q.history.takeLast(30), color, Modifier.fillMaxWidth().height(28.dp), progress = draw.value)
            }
        }
    }
}

@Composable
fun MarketSheet(m: MarketDef, quote: Quote?, loadHistory: suspend (String) -> Quote?, onDismiss: () -> Unit) {
    val s = LocalSettings.current
    val extras = LocalExtras.current
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var range by remember { mutableStateOf("1mo") }
    var data by remember { mutableStateOf(quote) }
    var loading by remember { mutableStateOf(false) }
    val draw = remember { Animatable(0f) }
    LaunchedEffect(range) {
        loading = true
        loadHistory(range)?.let { data = it }
        loading = false
        draw.snapTo(0f)
        draw.animateTo(1f, tween(if (extras.reducedMotion) 0 else 800, easing = FastOutSlowInEasing))
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state, containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.navigationBarsPadding().padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
            Text(m.label, style = MaterialTheme.typography.headlineSmall)
            val q = data
            if (q != null) {
                val hist = q.history
                val first = hist.firstOrNull() ?: q.price
                val diff = q.price - first
                val pct = if (first != 0.0) diff / first * 100 else 0.0
                val color = if (diff >= 0) extras.up else extras.down
                Text(Format.number(q.price, m.decimals), style = MaterialTheme.typography.displaySmall)
                Text(
                    Format.change(pct, diff, s.showChangeAsPercent, m.decimals) + "  " + stringResource(rangeLabel(range)),
                    color = color, style = MaterialTheme.typography.titleSmall,
                )
                Spacer(Modifier.height(16.dp))
                Sparkline(hist, color, Modifier.fillMaxWidth().height(180.dp), progress = draw.value, fill = true, strokeWidth = 2.dp)
                Spacer(Modifier.height(12.dp))
                if (hist.isNotEmpty()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Stat(stringResource(R.string.market_low), Format.number(hist.min(), m.decimals))
                        Stat(stringResource(R.string.market_high), Format.number(hist.max(), m.decimals))
                        Stat(stringResource(R.string.market_open), Format.number(first, m.decimals))
                    }
                }
            } else {
                Box(Modifier.fillMaxWidth().height(220.dp).shimmer(RoundedCornerShape(16.dp)))
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("1d", "5d", "1mo", "3mo", "1y").forEach { r ->
                    FilterChip(selected = r == range, onClick = { range = r }, label = { Text(stringResource(rangeLabel(r))) })
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(if (loading) R.string.loading else R.string.market_delay_note),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall)
    }
}

private fun rangeLabel(r: String): Int = when (r) {
    "1d" -> R.string.range_1d
    "5d" -> R.string.range_5d
    "1mo" -> R.string.range_1m
    "3mo" -> R.string.range_3m
    else -> R.string.range_1y
}

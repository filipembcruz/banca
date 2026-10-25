package io.github.filipembcruz.banca.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabPosition
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.filipembcruz.banca.R
import io.github.filipembcruz.banca.core.Format
import io.github.filipembcruz.banca.data.model.FeedItem
import io.github.filipembcruz.banca.data.model.ForYouBlock
import io.github.filipembcruz.banca.data.model.MarketDef
import io.github.filipembcruz.banca.data.model.Quote
import io.github.filipembcruz.banca.data.model.TabDef
import io.github.filipembcruz.banca.data.model.TabType
import io.github.filipembcruz.banca.data.model.Weather
import io.github.filipembcruz.banca.ui.components.ArticleRow
import io.github.filipembcruz.banca.ui.components.EmptyState
import io.github.filipembcruz.banca.ui.components.HeroCard
import io.github.filipembcruz.banca.ui.components.SectionHeader
import io.github.filipembcruz.banca.ui.components.SkeletonHero
import io.github.filipembcruz.banca.ui.components.SkeletonRow
import io.github.filipembcruz.banca.ui.components.WeatherText
import io.github.filipembcruz.banca.ui.components.tabTitle
import io.github.filipembcruz.banca.ui.nav.LocalActions
import io.github.filipembcruz.banca.ui.nav.LocalSettings
import io.github.filipembcruz.banca.ui.theme.LocalExtras
import java.util.Calendar
import kotlin.math.abs
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(vm: HomeViewModel, bottomPadding: androidx.compose.ui.unit.Dp, onPlace: (String) -> Unit = {}) {
    val settings = LocalSettings.current
    val tabs = settings.tabs
    val pager = rememberPagerState { tabs.size }
    val refreshing by vm.refreshing.collectAsStateWithLifecycle()
    val failed by vm.failed.collectAsStateWithLifecycle()
    val weather by vm.weather.collectAsStateWithLifecycle()
    val quotes by vm.quotes.collectAsStateWithLifecycle()
    val marketNews by vm.marketNews.collectAsStateWithLifecycle()
    var marketSheet by remember { mutableStateOf<MarketDef?>(null) }

    LaunchedEffect(settings.places, settings.markets) {
        vm.loadData(settings.places, settings.markets)
    }

    Column(Modifier.fillMaxSize()) {
        HomeHeader(
            weather = settings.places.firstOrNull()?.let { weather[it.key] },
        )
        TabStrip(tabs, pager)
        HorizontalPager(
            state = pager,
            modifier = Modifier.fillMaxSize(),
            key = { tabs.getOrNull(it)?.id ?: it },
            beyondViewportPageCount = 0,
        ) { page ->
            val baseTab = tabs.getOrNull(page) ?: return@HorizontalPager
            // Local shows one place at a time, picked from chips at the top of the page.
            val place = if (baseTab.type == TabType.LOCAL) {
                settings.places.firstOrNull { it.key == settings.localPlace } ?: settings.places.firstOrNull()
            } else null
            val tab = if (place != null) TabDef("local:${place.key}", TabType.LOCAL, place.key) else baseTab
            val items by vm.feed(tab).collectAsStateWithLifecycle()
            LaunchedEffect(tab.id, tab.value) { vm.refresh(tab) }
            val header: (@Composable () -> Unit)? =
                if (baseTab.type == TabType.LOCAL && settings.places.size > 1) {
                    { PlaceChips(settings.places, place?.key, onPlace) }
                } else null
            FeedPage(
                tab = tab,
                header = header,
                items = items,
                refreshing = tab.id in refreshing,
                failed = tab.id in failed,
                onRefresh = {
                    vm.refresh(tab, force = true)
                    if (tab.type == TabType.FOR_YOU) vm.loadData(settings.places, settings.markets, force = true)
                },
                weather = weather,
                quotes = quotes,
                marketNews = marketNews.orEmpty(),
                onMarket = { marketSheet = it },
                bottomPadding = bottomPadding,
            )
        }
    }

    marketSheet?.let { m ->
        MarketSheet(m, quotes[m.id], loadHistory = { r -> vm.history(m, r) }, onDismiss = { marketSheet = null })
    }
}

@Composable
internal fun HomeHeader(weather: Weather?) {
    val actions = LocalActions.current
    val settings = LocalSettings.current
    val context = LocalContext.current
    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val greeting = when (hour) {
        in 5..11 -> R.string.greeting_morning
        in 12..17 -> R.string.greeting_afternoon
        else -> R.string.greeting_evening
    }
    val date = remember {
        android.text.format.DateUtils.formatDateTime(
            context, System.currentTimeMillis(),
            android.text.format.DateUtils.FORMAT_SHOW_WEEKDAY or android.text.format.DateUtils.FORMAT_SHOW_DATE or
                android.text.format.DateUtils.FORMAT_NO_YEAR,
        )
    }
    Column(
        Modifier
            .statusBarsPadding()
            .padding(start = 20.dp, end = 12.dp, top = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    date.replaceFirstChar { it.titlecase() },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(stringResource(greeting), style = MaterialTheme.typography.headlineMedium)
            }
            if (weather != null) {
                Surface(
                    onClick = { actions.openSettings("settings/places") },
                    shape = RoundedCornerShape(50),
                    color = LocalExtras.current.card,
                ) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            WeatherText.icon(weather.code, weather.isDay), null,
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(Format.temp(weather.temp, settings.fahrenheit), style = MaterialTheme.typography.labelLarge)
                    }
                }
                Spacer(Modifier.width(4.dp))
            }
            IconButton(onClick = { actions.openSettings("inbox") }) {
                Icon(Icons.Rounded.NotificationsNone, stringResource(R.string.inbox_title))
            }
        }
        Spacer(Modifier.height(14.dp))
        Surface(
            onClick = { actions.openSearch(null) },
            shape = RoundedCornerShape(50),
            color = LocalExtras.current.card,
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 8.dp),
        ) {
            Row(Modifier.padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(12.dp))
                Text(
                    stringResource(R.string.search_hint),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

/** Tabs with an indicator that follows the finger while the pager is swiped. */
@Composable
internal fun TabStrip(tabs: List<TabDef>, pager: PagerState) {
    val scope = rememberCoroutineScope()
    val actions = LocalActions.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        ScrollableTabRow(
            selectedTabIndex = pager.currentPage.coerceIn(0, (tabs.size - 1).coerceAtLeast(0)),
            edgePadding = 16.dp,
            containerColor = MaterialTheme.colorScheme.background,
            divider = {},
            indicator = { positions -> PagerIndicator(positions, pager) },
            modifier = Modifier.weight(1f),
        ) {
            tabs.forEachIndexed { i, tab ->
                val selected = pager.currentPage == i
                val color by animateColorAsState(
                    if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    label = "tabColor",
                )
                Tab(
                    selected = selected,
                    onClick = { scope.launch { pager.animateScrollToPage(i) } },
                    text = {
                        Text(
                            tabTitle(tab),
                            color = color,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                            style = MaterialTheme.typography.titleSmall,
                        )
                    },
                )
            }
        }
        IconButton(onClick = { actions.openSettings("settings/tabs") }) {
            Icon(Icons.Rounded.Tune, stringResource(R.string.settings_tabs), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PagerIndicator(positions: List<TabPosition>, pager: PagerState) {
    if (positions.isEmpty()) return
    val current = pager.currentPage.coerceIn(0, positions.lastIndex)
    val fraction = pager.currentPageOffsetFraction
    val target = (current + if (fraction > 0) 1 else -1).coerceIn(0, positions.lastIndex)
    val f = abs(fraction)
    val a = positions[current]
    val b = positions[target]
    val left = lerp(a.left.value, b.left.value, f)
    val width = lerp(a.width.value, b.width.value, f)
    val bar = 22f
    Box(
        Modifier
            .fillMaxWidth()
            .wrapContentSize(Alignment.BottomStart)
            .offset(x = (left + (width - bar) / 2).dp)
            .padding(bottom = 4.dp)
            .width(bar.dp)
            .height(4.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
    )
}

@Composable
fun FeedPage(
    tab: TabDef,
    items: List<FeedItem>?,
    refreshing: Boolean,
    failed: Boolean,
    onRefresh: () -> Unit,
    weather: Map<String, Weather>,
    quotes: Map<String, Quote>,
    marketNews: List<FeedItem>,
    onMarket: (MarketDef) -> Unit,
    bottomPadding: androidx.compose.ui.unit.Dp,
    header: (@Composable () -> Unit)? = null,
) {
    val settings = LocalSettings.current
    val actions = LocalActions.current
    val listState = rememberLazyListState()
    val seen = remember(tab.id) { HashSet<String>() }
    val forYou = tab.type == TabType.FOR_YOU

    PullToRefreshBox(isRefreshing = refreshing, onRefresh = onRefresh, modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = bottomPadding + 24.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            val list = items.orEmpty()
            val top = if (forYou) list.filter { it.article.image != null || it.article.related.size >= 2 }.take(5) else emptyList()
            val topIds = top.map { it.article.id }.toSet()
            val rest = if (forYou) list.filter { it.article.id !in topIds } else list

            if (header != null) item(key = "header") { header() }

            if (forYou) {
                settings.blocks.filter { it.visible }.forEach { pref ->
                    when (pref.block) {
                        ForYouBlock.WEATHER -> if (settings.places.isNotEmpty()) item(key = "weather") {
                            WeatherCard(
                                settings.places, weather,
                                onClick = { actions.openSettings("settings/places") },
                                modifier = Modifier.padding(top = 8.dp).animateItem(),
                            )
                        }
                        ForYouBlock.MARKETS -> if (settings.markets.isNotEmpty()) item(key = "markets") {
                            Column(Modifier.animateItem()) {
                                SectionHeader(stringResource(R.string.section_markets), action = stringResource(R.string.action_edit),
                                    onAction = { actions.openSettings("settings/markets") })
                                // Full-bleed strip: undo the list padding.
                                Box(Modifier.requiredWidth(LocalConfiguration.current.screenWidthDp.dp)) {
                                    MarketStrip(settings.markets, quotes, onMarket)
                                }
                            }
                        }
                        ForYouBlock.TOP -> if (top.isNotEmpty()) item(key = "top") {
                            Column(Modifier.animateItem()) {
                                SectionHeader(stringResource(R.string.section_top))
                                Box(Modifier.requiredWidth(LocalConfiguration.current.screenWidthDp.dp)) {
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        items(top, key = { it.article.id }) { fi ->
                                            HeroCard(fi, Modifier.width(300.dp), sharedKey = "img-${tab.id}-top-${fi.article.id}")
                                        }
                                    }
                                }
                            }
                        }
                        ForYouBlock.MARKET_NEWS -> if (marketNews.isNotEmpty()) item(key = "marketnews") {
                            Column(Modifier.animateItem()) {
                                SectionHeader(stringResource(R.string.section_market_news))
                                marketNews.take(3).forEachIndexed { i, fi ->
                                    ArticleRow(fi, sharedKey = "img-${tab.id}-mk-${fi.article.id}", divider = i < 2)
                                }
                            }
                        }
                        ForYouBlock.FEED -> {
                            if (rest.isNotEmpty()) item(key = "feedHeader") {
                                SectionHeader(stringResource(R.string.section_for_you), Modifier.animateItem())
                            }
                            feedItems(rest, tab, seen, showTopic = true)
                        }
                    }
                }
            } else {
                feedItems(rest, tab, seen, showTopic = tab.type == TabType.TRENDING || tab.type == TabType.LOCAL)
            }

            when {
                items == null || (list.isEmpty() && refreshing) -> {
                    if (!forYou) item(key = "skHero") { SkeletonHero(Modifier.padding(top = 8.dp)) }
                    items(6, key = { "sk$it" }) { SkeletonRow() }
                }
                list.isEmpty() && failed -> item(key = "error") {
                    EmptyState(
                        Icons.Rounded.CloudOff,
                        stringResource(R.string.error_title),
                        stringResource(R.string.error_body),
                        action = stringResource(R.string.action_retry),
                        onAction = onRefresh,
                    )
                }
                list.isEmpty() -> item(key = "empty") {
                    EmptyState(
                        Icons.Rounded.Inbox,
                        stringResource(R.string.empty_title),
                        stringResource(if (tab.type == TabType.LOCAL) R.string.empty_local else R.string.empty_body),
                        action = if (tab.type == TabType.LOCAL) stringResource(R.string.action_add_place) else null,
                        onAction = { actions.openSettings("settings/places") },
                    )
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.feedItems(
    list: List<FeedItem>,
    tab: TabDef,
    seen: HashSet<String>,
    showTopic: Boolean,
) {
    val heroFirst = tab.type != TabType.FOR_YOU
    itemsIndexed(list, key = { _, it -> "f-" + it.article.id }) { i, fi ->
        val key = "img-${tab.id}-${fi.article.id}"
        Appear(fi.article.id, i, seen, Modifier.animateItem()) {
            val hero = (heroFirst && i == 0 && fi.article.image != null) || (i > 0 && i % 7 == 6 && fi.article.image != null)
            if (hero) {
                HeroCard(fi, Modifier.padding(vertical = 10.dp), sharedKey = key)
            } else {
                ArticleRow(fi, sharedKey = key, showTopic = showTopic)
            }
        }
    }
}

/** Staggered fade-and-rise the first time an item shows up. */
@Composable
private fun Appear(id: String, index: Int, seen: HashSet<String>, modifier: Modifier, content: @Composable () -> Unit) {
    val reduced = LocalExtras.current.reducedMotion
    val alreadyShown = remember(id) { reduced || !seen.add(id) }
    val state = remember(id) { MutableTransitionState(alreadyShown).apply { targetState = true } }
    AnimatedVisibility(
        visibleState = state,
        modifier = modifier,
        enter = fadeIn(tween(320, delayMillis = (index.coerceAtMost(8)) * 40)) +
            slideInVertically(tween(380, delayMillis = (index.coerceAtMost(8)) * 40)) { it / 5 },
    ) {
        content()
    }
}

/** Chips to switch the Local tab between places, in the order set in Settings. */
@Composable
fun PlaceChips(places: List<io.github.filipembcruz.banca.data.model.Place>, selected: String?, onSelect: (String) -> Unit) {
    val actions = LocalActions.current
    Box(Modifier.requiredWidth(LocalConfiguration.current.screenWidthDp.dp)) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
        ) {
            items(places, key = { it.key }) { p ->
                androidx.compose.material3.FilterChip(
                    selected = p.key == selected,
                    onClick = { onSelect(p.key) },
                    label = { Text(p.name, maxLines = 1) },
                    shape = RoundedCornerShape(50),
                )
            }
            item(key = "edit") {
                IconButton(onClick = { actions.openSettings("settings/places") }) {
                    Icon(Icons.Rounded.Tune, stringResource(R.string.settings_places), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

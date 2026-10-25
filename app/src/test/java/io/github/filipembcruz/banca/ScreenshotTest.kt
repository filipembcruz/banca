package io.github.filipembcruz.banca

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.filipembcruz.banca.data.model.Article
import io.github.filipembcruz.banca.data.model.FeedItem
import io.github.filipembcruz.banca.data.model.Place
import io.github.filipembcruz.banca.data.model.Quote
import io.github.filipembcruz.banca.data.model.Related
import io.github.filipembcruz.banca.data.model.TabDef
import io.github.filipembcruz.banca.data.model.TabType
import io.github.filipembcruz.banca.data.model.Weather
import io.github.filipembcruz.banca.data.model.WeatherDay
import io.github.filipembcruz.banca.data.net.Block
import io.github.filipembcruz.banca.data.net.Extracted
import io.github.filipembcruz.banca.data.settings.AppSettings
import io.github.filipembcruz.banca.data.settings.ThemeMode
import io.github.filipembcruz.banca.ui.home.FeedPage
import io.github.filipembcruz.banca.ui.home.HomeHeader
import io.github.filipembcruz.banca.ui.home.TabStrip
import io.github.filipembcruz.banca.ui.home.WeatherCard
import io.github.filipembcruz.banca.ui.nav.LocalSettings
import io.github.filipembcruz.banca.ui.onboarding.TopicGrid
import io.github.filipembcruz.banca.ui.reader.ReaderBody
import io.github.filipembcruz.banca.ui.reader.ReaderViewModel
import io.github.filipembcruz.banca.ui.settings.SettingsHome
import io.github.filipembcruz.banca.ui.theme.BancaTheme
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h1900dp-xhdpi")
class ScreenshotTest {

    private val now = System.currentTimeMillis()

    private fun article(i: Int, title: String, source: String, topic: String, related: Int = 0, summary: String? = null) = Article(
        id = "a$i",
        title = title,
        link = "https://example.com/$i",
        realLink = "https://example.com/$i",
        source = source,
        domain = null,
        published = now - i * 37 * 60_000L,
        image = null,
        summary = summary,
        topic = topic,
        related = (1..related).map { Related("Coverage $it of the same story", "https://example.com/r$it", "Outlet $it") },
    )

    private val items = listOf(
        article(1, "Central bank holds interest rate and signals cuts may come later this year", "g1", "business", related = 6,
            summary = "The committee kept the benchmark rate unchanged and said inflation is slowing in line with forecasts."),
        article(2, "Belo Horizonte opens new cycling lanes downtown", "O Tempo", "nation", related = 2),
        article(3, "New space telescope images reveal a galaxy cluster in unprecedented detail", "Folha", "science", related = 4,
            summary = "Astronomers say the data will help measure how fast the universe is expanding."),
        article(4, "Cruzeiro wins away and climbs to fourth place", "ge", "sports"),
        article(5, "Startups bet on AI to cut hospital waiting times", "Estadão", "technology", related = 1),
        article(6, "Lisbon approves plan to expand tram network", "Público", "world"),
        article(7, "Coffee prices rise for the third week in a row", "Valor", "business", related = 3),
    ).map { FeedItem(it, read = it.id == "a4", saved = it.id == "a3") }

    private val place = Place("Belo Horizonte", "Minas Gerais", "Brasil", "BR", -19.9, -43.9)
    private val weather = mapOf(
        place.key to Weather(place, 24.0, 25.0, 2, true, 11.0, 58,
            listOf("2026-10-25", "2026-10-26", "2026-10-27", "2026-10-28", "2026-10-29").mapIndexed { i, d ->
                WeatherDay(d, listOf(2, 61, 3, 0, 80)[i], 27.0 + i % 2, 17.0 + i % 3)
            }, now)
    )
    private val quotes = mapOf(
        "USDBRL" to Quote("USDBRL", 5.4321, -0.021, -0.39, (0..30).map { 5.5 + kotlin.math.sin(it / 4.0) * 0.08 }, now),
        "EURBRL" to Quote("EURBRL", 6.1234, 0.031, 0.51, (0..30).map { 6.0 + kotlin.math.cos(it / 5.0) * 0.1 }, now),
        "IBOV" to Quote("IBOV", 131245.0, 820.0, 0.63, (0..30).map { 128000.0 + it * 100 + kotlin.math.sin(it / 3.0) * 900 }, now),
    )

    private fun shot(name: String, dark: Boolean = false, content: @Composable () -> Unit) {
        val settings = AppSettings(onboarded = true, places = listOf(place), theme = if (dark) ThemeMode.DARK else ThemeMode.LIGHT)
        captureRoboImage("build/screens/$name.png") {
            CompositionLocalProvider(LocalSettings provides settings) {
                BancaTheme(mode = settings.theme) {
                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) { content() }
                }
            }
        }
    }

    @Composable
    private fun Home() {
        val tabs = AppSettings.defaultTabs()
        Column {
            HomeHeader(weather[place.key])
            TabStrip(tabs, rememberPagerState { tabs.size })
            FeedPage(
                tab = tabs.first(),
                items = items,
                refreshing = false,
                failed = false,
                onRefresh = {},
                weather = weather,
                quotes = quotes,
                marketNews = items.takeLast(2),
                onMarket = {},
                bottomPadding = 0.dp,
            )
        }
    }

    @Test fun home_light() = shot("01_home_light") { Home() }

    @Test fun home_dark() = shot("02_home_dark", dark = true) { Home() }

    @Test fun local_tab() = shot("03_local") {
        FeedPage(TabDef("local", TabType.LOCAL), items.drop(1), false, false, {}, weather, quotes, emptyList(), {}, 0.dp)
    }

    @Test fun reader() = shot("04_reader") {
        val content = Extracted(
            url = "https://example.com/1",
            title = items[0].article.title,
            image = null,
            site = "g1",
            author = "Ana Souza",
            blocks = listOf(
                Block("The central bank kept its benchmark rate unchanged on Wednesday, in a unanimous decision."),
                Block("In its statement, the committee said inflation has been slowing in line with forecasts, but that uncertainty abroad requires caution."),
                Block("What the market expects", heading = true),
                Block("Economists surveyed this week expect the first cut in the first quarter of next year, with the rate ending 2027 below the current level."),
                Block("\"We remain vigilant and will adjust policy if needed\", the statement said.", quote = true),
            ),
        )
        ReaderBody(items[0].article, ReaderViewModel.Body.Ready(content), null, {}, {})
    }

    @Test fun reader_dark() = shot("05_reader_dark", dark = true) {
        ReaderBody(items[2].article, ReaderViewModel.Body.Loading, null, {}, {})
    }

    @Test fun onboarding_topics() = shot("06_onboarding_topics") {
        Box(Modifier.padding(24.dp)) { TopicGrid(setOf("world", "business", "technology", "sports")) {} }
    }

    private fun weatherWith(code: Int, day: Boolean) = mapOf(
        place.key to weather.getValue(place.key).copy(code = code, isDay = day)
    )

    @Test fun weather_cards() = shot("09_weather", dark = true) {
        Column(Modifier.padding(16.dp)) {
            WeatherCard(listOf(place), weatherWith(3, false), onClick = {}, initiallyExpanded = true)
            androidx.compose.foundation.layout.Spacer(Modifier.padding(8.dp))
            WeatherCard(listOf(place), weatherWith(95, true), onClick = {}, initiallyExpanded = true)
            androidx.compose.foundation.layout.Spacer(Modifier.padding(8.dp))
            WeatherCard(listOf(place), weatherWith(0, true), onClick = {})
            androidx.compose.foundation.layout.Spacer(Modifier.padding(8.dp))
            WeatherCard(listOf(place), weatherWith(61, true), onClick = {})
        }
    }

    @Test fun settings_home() = shot("07_settings") { SettingsHome(navigate = {}, bottomPadding = 0.dp) }

    @Test fun settings_home_dark() = shot("08_settings_dark", dark = true) { SettingsHome(navigate = {}, bottomPadding = 0.dp) }
}

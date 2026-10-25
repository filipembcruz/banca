package io.github.filipembcruz.banca.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.filipembcruz.banca.R
import io.github.filipembcruz.banca.ui.components.ChipFlow
import io.github.filipembcruz.banca.core.Format
import io.github.filipembcruz.banca.core.LocaleHelper
import io.github.filipembcruz.banca.data.model.Markets
import io.github.filipembcruz.banca.data.model.Topics
import io.github.filipembcruz.banca.data.settings.ThemeMode
import io.github.filipembcruz.banca.ui.AppViewModel
import io.github.filipembcruz.banca.ui.components.Labels
import io.github.filipembcruz.banca.ui.nav.LocalSettings
import io.github.filipembcruz.banca.ui.settings.PlacePicker
import io.github.filipembcruz.banca.ui.settings.SettingsViewModel
import io.github.filipembcruz.banca.ui.settings.changeLanguage
import io.github.filipembcruz.banca.ui.settings.rememberNotificationPermission
import io.github.filipembcruz.banca.ui.theme.LocalExtras
import kotlinx.coroutines.launch

private const val PAGES = 4

@Composable
fun OnboardingScreen(app: AppViewModel, settingsVm: SettingsViewModel, onDone: () -> Unit) {
    val s = LocalSettings.current
    val pager = rememberPagerState { PAGES }
    val scope = rememberCoroutineScope()
    val progress by animateFloatAsState((pager.currentPage + 1f) / PAGES, label = "progress")

    fun next() {
        if (pager.currentPage < PAGES - 1) scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
        else {
            app.updateSettings { it.copy(onboarded = true) }
            onDone()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Row(Modifier.padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.weight(1f).height(6.dp).clip(CircleShape),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                drawStopIndicator = {},
            )
            Spacer(Modifier.width(12.dp))
            TextButton(onClick = {
                app.updateSettings { it.copy(onboarded = true) }
                onDone()
            }) { Text(stringResource(R.string.action_skip)) }
        }

        HorizontalPager(state = pager, userScrollEnabled = false, modifier = Modifier.weight(1f)) { page ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
            ) {
                when (page) {
                    0 -> WelcomePage(app)
                    1 -> TopicsPage(app)
                    2 -> PlacesPage(app, settingsVm)
                    else -> NotificationsStep(app)
                }
            }
        }

        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            AnimatedContent(pager.currentPage > 0, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "back") { show ->
                if (show) TextButton(onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } }) {
                    Text(stringResource(R.string.action_back))
                } else Spacer(Modifier.width(1.dp))
            }
            Spacer(Modifier.weight(1f))
            val enabled = pager.currentPage != 1 || s.topics.size >= 3
            Button(onClick = { next() }, enabled = enabled, modifier = Modifier.height(52.dp)) {
                AnimatedContent(pager.currentPage == PAGES - 1, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "next") { last ->
                    Text(stringResource(if (last) R.string.onboarding_start else R.string.action_next), modifier = Modifier.padding(horizontal = 12.dp))
                }
            }
        }
    }
}

@Composable
private fun Title(title: String, body: String) {
    Text(title, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(top = 12.dp))
    Text(
        body,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp, bottom = 20.dp),
    )
}

@Composable
private fun WelcomePage(app: AppViewModel) {
    val s = LocalSettings.current
    val context = LocalContext.current
    val lang = LocaleHelper.current(context)
    Column(Modifier.verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(24.dp))
        Box(
            Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Image(painterResource(R.drawable.ic_banca_mark), null, Modifier.size(64.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.onboarding_welcome), style = MaterialTheme.typography.displaySmall)
        Text(
            stringResource(R.string.onboarding_welcome_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 10.dp, bottom = 28.dp),
        )
        Text(stringResource(R.string.settings_language), style = MaterialTheme.typography.titleMedium)
        Row(Modifier.padding(top = 10.dp, bottom = 24.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OptionCard("English", lang == "en", Modifier.weight(1f)) { changeLanguage(context, "en") }
            OptionCard("Português", lang == "pt", Modifier.weight(1f)) { changeLanguage(context, "pt") }
        }
        Text(stringResource(R.string.appearance_theme), style = MaterialTheme.typography.titleMedium)
        Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(
                ThemeMode.SYSTEM to R.string.theme_system,
                ThemeMode.LIGHT to R.string.theme_light,
                ThemeMode.DARK to R.string.theme_dark,
            ).forEach { (mode, label) ->
                OptionCard(stringResource(label), s.theme == mode, Modifier.weight(1f)) {
                    app.updateSettings { it.copy(theme = mode) }
                }
            }
        }
    }
}

@Composable
private fun OptionCard(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val border by animateColorAsState(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, label = "b")
    val bg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else LocalExtras.current.card, label = "bg",
    )
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = bg,
        border = BorderStroke(if (selected) 2.dp else 1.dp, border),
        modifier = modifier,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp),
        )
    }
}

@Composable
private fun TopicsPage(app: AppViewModel) {
    val s = LocalSettings.current
    TopicGrid(s.topics) { id ->
        app.updateSettings { st -> st.copy(topics = if (id in st.topics) st.topics - id else st.topics + id) }
    }
}

/** Title + grid of round topic tiles. Stateless so it can be rendered in screenshot tests. */
@Composable
internal fun TopicGrid(selected: Set<String>, onToggle: (String) -> Unit) {
    Column {
        Title(stringResource(R.string.onboarding_topics), stringResource(R.string.onboarding_topics_body))
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(Topics.all, key = { it.id }) { t ->
                TopicTile(Labels.topicIcon(t.id), stringResource(Labels.topicRes(t.id)!!), t.id in selected) { onToggle(t.id) }
            }
        }
    }
}

@Composable
private fun TopicTile(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val reduced = LocalExtras.current.reducedMotion
    val scale by animateFloatAsState(
        if (selected) 1f else 0.94f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = if (reduced) Spring.StiffnessHigh else Spring.StiffnessMedium),
        label = "scale",
    )
    val ring by animateColorAsState(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, label = "ring")
    val fill by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else LocalExtras.current.card, label = "fill",
    )
    val tint by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, label = "tint",
    )
    Column(
        Modifier
            .scale(scale)
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(fill)
            .border(if (selected) 2.dp else 1.dp, ring, CircleShape)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(if (selected) Icons.Rounded.Check else icon, null, tint = tint, modifier = Modifier.size(28.dp))
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 6.dp),
        )
    }
}

@Composable
private fun PlacesPage(app: AppViewModel, vm: SettingsViewModel) {
    val s = LocalSettings.current
    Column(Modifier.verticalScroll(rememberScrollState())) {
        Title(stringResource(R.string.onboarding_places), stringResource(R.string.onboarding_places_body))
        ChipFlow {
            s.places.forEach { p ->
                InputChip(
                    selected = true,
                    onClick = { app.updateSettings { st -> st.copy(places = st.places.filter { it.key != p.key }) } },
                    label = { Text(p.name) },
                    leadingIcon = { Icon(Icons.Rounded.Place, null, Modifier.size(18.dp)) },
                    trailingIcon = { Icon(Icons.Rounded.Close, stringResource(R.string.action_remove), Modifier.size(16.dp)) },
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        PlacePicker(vm) { p ->
            app.updateSettings { st -> if (st.places.any { it.key == p.key }) st else st.copy(places = st.places + p) }
        }
        Spacer(Modifier.height(28.dp))
        Text(stringResource(R.string.onboarding_markets), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.onboarding_markets_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        )
        ChipFlow {
            Markets.all.forEach { m ->
                val on = m.id in s.markets
                FilterChip(
                    selected = on,
                    onClick = { app.updateSettings { st -> st.copy(markets = if (on) st.markets - m.id else st.markets + m.id) } },
                    label = { Text(m.label) },
                    leadingIcon = if (on) ({ Icon(Icons.Rounded.Check, null, Modifier.size(18.dp)) }) else null,
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun NotificationsStep(app: AppViewModel) {
    val s = LocalSettings.current
    val ask = rememberNotificationPermission { granted ->
        app.updateSettings { it.copy(notifications = granted) }
    }
    Column(Modifier.verticalScroll(rememberScrollState())) {
        Title(stringResource(R.string.onboarding_notifications), stringResource(R.string.onboarding_notifications_body))
        val mode = when {
            !s.notifications -> 0
            s.bigStoryAlerts && !s.digest -> 2
            else -> 1
        }
        ModeCard(Icons.Rounded.NotificationsOff, stringResource(R.string.onboarding_notif_off), stringResource(R.string.onboarding_notif_off_body), mode == 0) {
            app.updateSettings { it.copy(notifications = false) }
        }
        Spacer(Modifier.height(10.dp))
        ModeCard(Icons.Rounded.Schedule, stringResource(R.string.onboarding_notif_digest), stringResource(R.string.onboarding_notif_digest_body), mode == 1) {
            app.updateSettings { it.copy(digest = true, bigStoryAlerts = false) }
            ask()
        }
        if (mode == 1) {
            ChipFlow(Modifier.padding(top = 10.dp, start = 8.dp)) {
                listOf(7 * 60, 8 * 60, 12 * 60, 18 * 60, 21 * 60).forEach { t ->
                    val on = t in s.digestTimes
                    FilterChip(
                        selected = on,
                        onClick = {
                            app.updateSettings { st ->
                                val times = if (on) st.digestTimes - t else st.digestTimes + t
                                st.copy(digestTimes = times.ifEmpty { listOf(t) }.sorted())
                            }
                        },
                        label = { Text(Format.clock(t)) },
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        ModeCard(Icons.Rounded.NotificationsActive, stringResource(R.string.onboarding_notif_big), stringResource(R.string.onboarding_notif_big_body), mode == 2) {
            app.updateSettings { it.copy(digest = false, bigStoryAlerts = true) }
            ask()
        }
        Text(
            stringResource(R.string.onboarding_notif_later),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

@Composable
private fun ModeCard(icon: ImageVector, title: String, body: String, selected: Boolean, onClick: () -> Unit) {
    val border by animateColorAsState(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, label = "mb")
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else LocalExtras.current.card,
        border = BorderStroke(if (selected) 2.dp else 1.dp, border),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

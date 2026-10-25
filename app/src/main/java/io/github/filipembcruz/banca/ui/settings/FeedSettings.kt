package io.github.filipembcruz.banca.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.RssFeed
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.filipembcruz.banca.R
import io.github.filipembcruz.banca.data.model.BlockPref
import io.github.filipembcruz.banca.data.model.ForYouBlock
import io.github.filipembcruz.banca.data.model.TabDef
import io.github.filipembcruz.banca.data.model.TabType
import io.github.filipembcruz.banca.data.model.Topics
import io.github.filipembcruz.banca.data.model.Editions
import io.github.filipembcruz.banca.ui.AppViewModel
import io.github.filipembcruz.banca.ui.components.Labels
import io.github.filipembcruz.banca.ui.components.SourceIcon
import io.github.filipembcruz.banca.ui.components.tabTitle
import io.github.filipembcruz.banca.ui.nav.LocalSettings
import io.github.filipembcruz.banca.ui.theme.LocalExtras

private fun <T> List<T>.move(from: Int, to: Int): List<T> {
    if (to !in indices || from !in indices) return this
    val m = toMutableList()
    val x = m.removeAt(from)
    m.add(to, x)
    return m
}

@Composable
fun TabsPage(app: AppViewModel, onBack: () -> Unit) {
    val s = LocalSettings.current
    var adding by remember { mutableStateOf(false) }
    SettingsPage(stringResource(R.string.settings_tabs), onBack) {
        item {
            Text(
                stringResource(R.string.tabs_help),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
        items(s.tabs, key = { it.id }) { tab ->
            val i = s.tabs.indexOf(tab)
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = LocalExtras.current.card,
                modifier = Modifier.padding(vertical = 4.dp).animateItem(),
            ) {
                Row(Modifier.padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(tabIcon(tab), null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(tabTitle(tab), style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(tabKind(tab), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { app.updateSettings { it.copy(tabs = it.tabs.move(i, i - 1)) } }, enabled = i > 0) {
                        Icon(Icons.Rounded.KeyboardArrowUp, stringResource(R.string.action_move_up))
                    }
                    IconButton(onClick = { app.updateSettings { it.copy(tabs = it.tabs.move(i, i + 1)) } }, enabled = i < s.tabs.lastIndex) {
                        Icon(Icons.Rounded.KeyboardArrowDown, stringResource(R.string.action_move_down))
                    }
                    IconButton(
                        onClick = { app.updateSettings { st -> st.copy(tabs = st.tabs.filter { it.id != tab.id }) } },
                        enabled = tab.type != TabType.FOR_YOU && s.tabs.size > 1,
                    ) {
                        Icon(Icons.Rounded.DeleteOutline, stringResource(R.string.action_remove))
                    }
                }
            }
        }
        item {
            Button(onClick = { adding = true }, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                Icon(Icons.Rounded.Add, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.action_add_tab))
            }
        }
        item {
            Group(stringResource(R.string.settings_edition), footer = stringResource(R.string.settings_edition_sub)) {
                ChoiceItem(
                    stringResource(R.string.settings_edition),
                    Editions.all.map { it.id to editionLabel(it.id) },
                    s.edition,
                ) { e -> app.updateSettings { it.copy(edition = e) } }
            }
        }
    }
    if (adding) AddTabSheet(app, onDismiss = { adding = false })
}

@Composable
private fun editionLabel(id: String): String = when (id) {
    "BR" -> stringResource(R.string.edition_br)
    "PT" -> stringResource(R.string.edition_pt)
    "US" -> stringResource(R.string.edition_us)
    else -> stringResource(R.string.edition_gb)
}

private fun tabIcon(tab: TabDef) = when (tab.type) {
    TabType.FOR_YOU -> Icons.Rounded.AutoAwesome
    TabType.TRENDING -> Labels.topicIcon("top")
    TabType.LOCAL -> Icons.Rounded.Place
    TabType.TOPIC -> Labels.topicIcon(tab.value)
    TabType.SEARCH -> Icons.Rounded.Search
    TabType.FEED -> Icons.Rounded.RssFeed
}

@Composable
private fun tabKind(tab: TabDef): String = stringResource(
    when (tab.type) {
        TabType.FOR_YOU -> R.string.tabkind_for_you
        TabType.TRENDING -> R.string.tabkind_trending
        TabType.LOCAL -> R.string.tabkind_local
        TabType.TOPIC -> R.string.tabkind_topic
        TabType.SEARCH -> R.string.tabkind_search
        TabType.FEED -> R.string.tabkind_feed
    }
)

@Composable
private fun AddTabSheet(app: AppViewModel, onDismiss: () -> Unit) {
    val s = LocalSettings.current
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var kind by remember { mutableIntStateOf(0) }
    var text by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }

    fun addTab(tab: TabDef) {
        app.updateSettings { st -> if (st.tabs.any { it.id == tab.id }) st else st.copy(tabs = st.tabs + tab) }
        onDismiss()
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state, containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.navigationBarsPadding().padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
            Text(stringResource(R.string.action_add_tab), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(12.dp))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf(R.string.tabkind_topic, R.string.tabkind_search, R.string.tabkind_feed).forEachIndexed { i, label ->
                    SegmentedButton(selected = kind == i, onClick = { kind = i }, shape = SegmentedButtonDefaults.itemShape(i, 3)) {
                        Text(stringResource(label))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            when (kind) {
                0 -> FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val builtIns = buildList {
                        if (s.tabs.none { it.type == TabType.TRENDING }) add(TabDef("trending", TabType.TRENDING))
                        if (s.tabs.none { it.type == TabType.LOCAL }) add(TabDef("local", TabType.LOCAL))
                        (listOf(Topics.SCITECH) + Topics.all.map { it.id }).forEach { id ->
                            if (s.tabs.none { it.type == TabType.TOPIC && it.value == id }) add(TabDef("t-$id", TabType.TOPIC, id))
                        }
                    }
                    builtIns.forEach { tab ->
                        FilterChip(
                            selected = false,
                            onClick = { addTab(tab) },
                            label = { Text(tabTitle(tab)) },
                            leadingIcon = { Icon(tabIcon(tab), null, Modifier.size(18.dp)) },
                        )
                    }
                }
                1 -> Column {
                    OutlinedTextField(
                        value = text, onValueChange = { text = it },
                        label = { Text(stringResource(R.string.add_tab_search_label)) },
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        stringResource(R.string.add_tab_search_help),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    Button(
                        onClick = { addTab(TabDef("q-" + text.trim().lowercase().hashCode().toUInt().toString(16), TabType.SEARCH, text.trim(), text.trim())) },
                        enabled = text.isNotBlank(),
                        modifier = Modifier.padding(top = 12.dp).fillMaxWidth(),
                    ) { Text(stringResource(R.string.action_add)) }
                }
                else -> Column {
                    OutlinedTextField(
                        value = name, onValueChange = { name = it },
                        label = { Text(stringResource(R.string.add_tab_feed_name)) },
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = text, onValueChange = { text = it },
                        label = { Text(stringResource(R.string.add_tab_feed_url)) },
                        placeholder = { Text("https://…/feed") },
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                    )
                    val url = text.trim().let { if (it.startsWith("http")) it else "https://$it" }
                    Button(
                        onClick = { addTab(TabDef("rss-" + url.hashCode().toUInt().toString(16), TabType.FEED, url, name.trim().ifBlank { url.removePrefix("https://").substringBefore('/') })) },
                        enabled = text.contains('.'),
                        modifier = Modifier.padding(top = 12.dp).fillMaxWidth(),
                    ) { Text(stringResource(R.string.action_add)) }
                }
            }
        }
    }
}

@Composable
fun ForYouPage(app: AppViewModel, onBack: () -> Unit) {
    val s = LocalSettings.current
    SettingsPage(stringResource(R.string.settings_for_you), onBack) {
        item {
            Text(
                stringResource(R.string.foryou_help),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
        items(s.blocks, key = { it.block.name }) { pref ->
            val i = s.blocks.indexOf(pref)
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = LocalExtras.current.card,
                modifier = Modifier.padding(vertical = 4.dp).animateItem(),
            ) {
                Row(Modifier.padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        app.updateSettings { st -> st.copy(blocks = st.blocks.map { if (it.block == pref.block) BlockPref(it.block, !it.visible) else it }) }
                    }) {
                        Icon(
                            if (pref.visible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                            null,
                            tint = if (pref.visible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        stringResource(blockLabel(pref.block)),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (pref.visible) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { app.updateSettings { it.copy(blocks = it.blocks.move(i, i - 1)) } }, enabled = i > 0) {
                        Icon(Icons.Rounded.KeyboardArrowUp, stringResource(R.string.action_move_up))
                    }
                    IconButton(onClick = { app.updateSettings { it.copy(blocks = it.blocks.move(i, i + 1)) } }, enabled = i < s.blocks.lastIndex) {
                        Icon(Icons.Rounded.KeyboardArrowDown, stringResource(R.string.action_move_down))
                    }
                }
            }
        }
        item {
            Group(stringResource(R.string.foryou_mix), footer = stringResource(R.string.foryou_mix_sub)) {
                FlowRow(
                    Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Topics.all.forEach { t ->
                        val on = t.id in s.topics
                        FilterChip(
                            selected = on,
                            onClick = { app.updateSettings { st -> st.copy(topics = if (on) st.topics - t.id else st.topics + t.id) } },
                            label = { Text(stringResource(Labels.topicRes(t.id)!!)) },
                            leadingIcon = { Icon(if (on) Icons.Rounded.Check else Labels.topicIcon(t.id), null, Modifier.size(18.dp)) },
                        )
                    }
                }
            }
        }
        item { TermsGroup(app) }
    }
}

@Composable
private fun TermsGroup(app: AppViewModel) {
    val s = LocalSettings.current
    var term by remember { mutableStateOf("") }
    Group(stringResource(R.string.foryou_terms), footer = stringResource(R.string.foryou_terms_sub)) {
        Column(Modifier.padding(16.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                s.terms.forEach { t ->
                    FilterChip(
                        selected = true,
                        onClick = { app.updateSettings { it.copy(terms = it.terms - t) } },
                        label = { Text(t) },
                        trailingIcon = { Icon(Icons.Rounded.Close, stringResource(R.string.action_remove), Modifier.size(16.dp)) },
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                OutlinedTextField(
                    value = term, onValueChange = { term = it },
                    placeholder = { Text(stringResource(R.string.foryou_terms_hint)) },
                    singleLine = true, modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = {
                        val t = term.trim()
                        if (t.isNotEmpty()) app.updateSettings { it.copy(terms = (it.terms + t).distinct()) }
                        term = ""
                    },
                    enabled = term.isNotBlank(),
                ) { Icon(Icons.Rounded.Add, stringResource(R.string.action_add)) }
            }
        }
    }
}

fun blockLabel(b: ForYouBlock): Int = when (b) {
    ForYouBlock.WEATHER -> R.string.block_weather
    ForYouBlock.MARKETS -> R.string.block_markets
    ForYouBlock.TOP -> R.string.block_top
    ForYouBlock.MARKET_NEWS -> R.string.block_market_news
    ForYouBlock.FEED -> R.string.block_feed
}

@Composable
fun TopicsSourcesPage(vm: SettingsViewModel, app: AppViewModel, onBack: () -> Unit) {
    val sources by vm.sources.collectAsStateWithLifecycle()
    val weights by vm.topicWeights.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.loadPrefs() }
    val muted = weights.filter { it.value <= -1.0 }
    SettingsPage(stringResource(R.string.settings_topics_sources), onBack) {
        item {
            Group(stringResource(R.string.topics_shown_less), footer = stringResource(R.string.topics_shown_less_sub)) {
                if (muted.isEmpty()) {
                    Item(stringResource(R.string.none_yet))
                } else {
                    muted.keys.forEachIndexed { i, t ->
                        if (i > 0) Divider()
                        val label = Labels.topicRes(t)?.let { stringResource(it) } ?: t.substringAfter(':')
                        Item(label, icon = Labels.topicIcon(t), chevron = false) {
                            TextButton(onClick = { vm.resetTopic(t) }) { Text(stringResource(R.string.action_restore)) }
                        }
                    }
                }
            }
        }
        item {
            Group(stringResource(R.string.sources_blocked_less), footer = stringResource(R.string.sources_blocked_less_sub)) {
                if (sources.isEmpty()) {
                    Item(stringResource(R.string.none_yet))
                } else {
                    sources.forEachIndexed { i, src ->
                        if (i > 0) Divider()
                        Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            SourceIcon(src.name, "https://www.google.com/s2/favicons?domain=${src.domain}&sz=128", size = 28.dp)
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(src.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    stringResource(if (src.blocked) R.string.source_blocked else R.string.source_less),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (src.blocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            TextButton(onClick = { vm.unblock(src) }) {
                                Text(stringResource(if (src.blocked) R.string.action_unblock else R.string.action_restore))
                            }
                        }
                    }
                }
            }
        }
        item {
            Group(stringResource(R.string.learning), footer = stringResource(R.string.learning_sub)) {
                Item(stringResource(R.string.action_reset_learning), onClick = {
                    vm.resetLearning(); app.notify(R.string.msg_learning_reset)
                }, chevron = false)
                Divider()
                Item(stringResource(R.string.action_unhide_all), onClick = {
                    vm.clearHidden(); app.notify(R.string.msg_unhidden)
                }, chevron = false)
            }
        }
    }
}

package io.github.filipembcruz.banca.ui.explore

import android.app.Application
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import io.github.filipembcruz.banca.R
import io.github.filipembcruz.banca.core.container
import io.github.filipembcruz.banca.data.db.SourceInfo
import io.github.filipembcruz.banca.data.model.FeedItem
import io.github.filipembcruz.banca.data.model.Topics
import io.github.filipembcruz.banca.ui.AppViewModel
import io.github.filipembcruz.banca.ui.components.ArticleRow
import io.github.filipembcruz.banca.ui.components.EmptyState
import io.github.filipembcruz.banca.ui.components.Labels
import io.github.filipembcruz.banca.ui.components.SectionHeader
import io.github.filipembcruz.banca.ui.components.SkeletonRow
import io.github.filipembcruz.banca.ui.components.SourceIcon
import io.github.filipembcruz.banca.ui.nav.LocalSettings
import io.github.filipembcruz.banca.ui.theme.LocalExtras
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ExploreViewModel(app: Application) : AndroidViewModel(app) {
    private val c = app.container

    sealed interface Results {
        data object Idle : Results
        data object Loading : Results
        data object Failed : Results
        data class Done(val items: List<FeedItem>) : Results
    }

    private val _results = MutableStateFlow<Results>(Results.Idle)
    val results: StateFlow<Results> = _results

    private val _sources = MutableStateFlow<List<SourceInfo>>(emptyList())
    val sources: StateFlow<List<SourceInfo>> = _sources

    var lastQuery: String = ""
        private set
    var lastWindow: String? = null
        private set

    fun search(query: String, window: String?) {
        val q = query.trim()
        if (q.isEmpty()) return
        lastQuery = q
        lastWindow = window
        _results.value = Results.Loading
        viewModelScope.launch {
            _results.value = runCatching { Results.Done(c.news.search(q, window)) }.getOrElse { Results.Failed }
            c.settings.update { s -> s.copy(recentSearches = (listOf(q) + s.recentSearches.filter { !it.equals(q, true) }).take(8)) }
        }
    }

    fun clear() {
        _results.value = Results.Idle
        lastQuery = ""
    }

    fun loadSources() {
        viewModelScope.launch {
            _sources.value = withContext(Dispatchers.IO) {
                val known = c.store.knownSources()
                val blocked = c.store.sourcePrefs().values.filter { it.blocked && known.none { k -> k.domain == it.domain } }
                known + blocked
            }
        }
    }

    fun setBlocked(s: SourceInfo, blocked: Boolean) {
        viewModelScope.launch {
            c.news.block(s.domain, s.name, blocked)
            loadSources()
        }
    }

    fun clearRecent() {
        viewModelScope.launch { c.settings.update { it.copy(recentSearches = emptyList()) } }
    }
}

@Composable
fun ExploreScreen(
    vm: ExploreViewModel,
    app: AppViewModel,
    initialQuery: String?,
    focusSearch: Boolean,
    bottomPadding: androidx.compose.ui.unit.Dp,
) {
    val settings = LocalSettings.current
    val results by vm.results.collectAsStateWithLifecycle()
    val sources by vm.sources.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf(initialQuery ?: vm.lastQuery) }
    var window by rememberSaveable { mutableStateOf(vm.lastWindow ?: "7d") }
    val focus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) { vm.loadSources() }
    LaunchedEffect(focusSearch) { if (focusSearch) runCatching { focus.requestFocus() } }
    LaunchedEffect(initialQuery) { if (!initialQuery.isNullOrBlank()) vm.search(initialQuery, window) }

    fun run() {
        focusManager.clearFocus()
        vm.search(query, window.takeIf { it != "any" })
    }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.statusBarsPadding().padding(horizontal = 20.dp).padding(top = 12.dp)) {
            Text(stringResource(R.string.nav_explore), style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(12.dp))
            TextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(stringResource(R.string.search_hint)) },
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                trailingIcon = {
                    if (query.isNotEmpty()) IconButton(onClick = { query = ""; vm.clear() }) { Icon(Icons.Rounded.Close, stringResource(R.string.action_clear)) }
                },
                singleLine = true,
                shape = RoundedCornerShape(50),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = LocalExtras.current.card,
                    unfocusedContainerColor = LocalExtras.current.card,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { run() }),
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
            )
            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("1d" to R.string.window_24h, "7d" to R.string.window_week, "any" to R.string.window_any).forEach { (w, label) ->
                    FilterChip(selected = window == w, onClick = { window = w; if (query.isNotBlank()) run() }, label = { Text(stringResource(label)) })
                }
            }
        }

        AnimatedContent(
            targetState = results,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            contentKey = { it::class },
            label = "explore",
            modifier = Modifier.fillMaxSize(),
        ) { r ->
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = bottomPadding + 24.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                when (r) {
                    is ExploreViewModel.Results.Loading -> items(6) { SkeletonRow() }
                    is ExploreViewModel.Results.Failed -> item {
                        EmptyState(Icons.Rounded.SearchOff, stringResource(R.string.error_title), stringResource(R.string.error_body),
                            action = stringResource(R.string.action_retry), onAction = { run() })
                    }
                    is ExploreViewModel.Results.Done -> {
                        item {
                            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    stringResource(R.string.results_for, vm.lastQuery),
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                AssistChip(
                                    onClick = { app.addSearchTab(vm.lastQuery) },
                                    label = { Text(stringResource(R.string.action_add_as_tab)) },
                                    leadingIcon = { Icon(Icons.Rounded.Add, null, Modifier.size(18.dp)) },
                                )
                            }
                        }
                        if (r.items.isEmpty()) item {
                            EmptyState(Icons.Rounded.SearchOff, stringResource(R.string.no_results), stringResource(R.string.no_results_body))
                        }
                        items(r.items, key = { it.article.id }) { fi ->
                            ArticleRow(fi, sharedKey = "img-search-${fi.article.id}", modifier = Modifier.animateItem())
                        }
                    }
                    is ExploreViewModel.Results.Idle -> {
                        if (settings.recentSearches.isNotEmpty()) {
                            item {
                                SectionHeader(stringResource(R.string.recent_searches), action = stringResource(R.string.action_clear),
                                    onAction = { vm.clearRecent() })
                            }
                            items(settings.recentSearches) { q ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { query = q; run() }
                                        .padding(horizontal = 8.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(Icons.Rounded.History, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.width(14.dp))
                                    Text(q, style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                        }
                        item { SectionHeader(stringResource(R.string.section_topics)) }
                        item {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Topics.all.forEach { t ->
                                    val followed = t.id in settings.topics
                                    val label = stringResource(Labels.topicRes(t.id)!!)
                                    FilterChip(
                                        selected = followed,
                                        onClick = {
                                            app.updateSettings { s -> s.copy(topics = if (followed) s.topics - t.id else s.topics + t.id) }
                                        },
                                        label = { Text(label) },
                                        leadingIcon = { Icon(if (followed) Icons.Rounded.Check else Labels.topicIcon(t.id), null, Modifier.size(18.dp)) },
                                    )
                                }
                            }
                        }
                        if (sources.isNotEmpty()) {
                            item { SectionHeader(stringResource(R.string.section_sources)) }
                            items(sources, key = { "src-" + it.domain }) { s ->
                                SourceRow(s, onToggle = { vm.setBlocked(s, !s.blocked) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceRow(s: SourceInfo, onToggle: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        SourceIcon(s.name, "https://www.google.com/s2/favicons?domain=${s.domain}&sz=128", size = 32.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(s.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(s.domain, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (s.blocked) {
            OutlinedButton(onClick = onToggle) { Text(stringResource(R.string.action_unblock)) }
        } else {
            Surface(onClick = onToggle, shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceVariant) {
                Text(
                    stringResource(R.string.action_block),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
    }
}

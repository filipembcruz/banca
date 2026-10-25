package io.github.filipembcruz.banca.ui.saved

import android.app.Application
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.graphics.Color
import io.github.filipembcruz.banca.ui.theme.LocalExtras
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import io.github.filipembcruz.banca.R
import io.github.filipembcruz.banca.core.container
import io.github.filipembcruz.banca.data.db.HistoryEntry
import io.github.filipembcruz.banca.data.model.Article
import io.github.filipembcruz.banca.data.model.FeedItem
import io.github.filipembcruz.banca.ui.AppViewModel
import io.github.filipembcruz.banca.ui.components.ArticleRow
import io.github.filipembcruz.banca.ui.components.EmptyState
import io.github.filipembcruz.banca.ui.components.topicLabel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SavedViewModel(app: Application) : AndroidViewModel(app) {
    private val c = app.container

    val saved: StateFlow<List<FeedItem>?> = c.store.changes
        .map { _ ->
            val read = c.store.readIds()
            c.store.saved().map { FeedItem(it.article, read = it.article.id in read, saved = true) }
        }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val history: StateFlow<List<HistoryEntry>> = c.store.changes
        .map { c.store.history() }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun clearHistory() {
        viewModelScope.launch(Dispatchers.IO) { c.store.clearHistory() }
    }
}

@Composable
fun SavedScreen(vm: SavedViewModel, app: AppViewModel, bottomPadding: androidx.compose.ui.unit.Dp) {
    val saved by vm.saved.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    var mode by rememberSaveable { mutableStateOf(0) }
    var filter by rememberSaveable { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    val q = io.github.filipembcruz.banca.core.Text.fold(query.trim())
    fun matches(title: String, source: String) =
        q.isEmpty() || io.github.filipembcruz.banca.core.Text.fold("$title $source").contains(q)

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.statusBarsPadding().padding(horizontal = 20.dp).padding(top = 12.dp)) {
            Text(stringResource(R.string.nav_saved), style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(12.dp))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(selected = mode == 0, onClick = { mode = 0 }, shape = SegmentedButtonDefaults.itemShape(0, 2)) {
                    Text(stringResource(R.string.saved_tab_saved))
                }
                SegmentedButton(selected = mode == 1, onClick = { mode = 1 }, shape = SegmentedButtonDefaults.itemShape(1, 2)) {
                    Text(stringResource(R.string.saved_tab_history))
                }
            }
            TextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(stringResource(if (mode == 0) R.string.saved_search_hint else R.string.history_search_hint)) },
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                trailingIcon = {
                    if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Rounded.Close, stringResource(R.string.action_clear)) }
                },
                singleLine = true,
                shape = RoundedCornerShape(50),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = LocalExtras.current.card,
                    unfocusedContainerColor = LocalExtras.current.card,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )
        }

        if (mode == 0) {
            val list = saved.orEmpty()
            val topics = list.mapNotNull { it.article.topic }.distinct()
            val shown = (if (filter == null) list else list.filter { it.article.topic == filter })
                .filter { matches(it.article.title, it.article.source) }
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = bottomPadding + 24.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                if (topics.size > 1) item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 4.dp)) {
                        item { FilterChip(selected = filter == null, onClick = { filter = null }, label = { Text(stringResource(R.string.filter_all)) }) }
                        items(topics) { t ->
                            val label = topicLabel(t) ?: t
                            FilterChip(selected = filter == t, onClick = { filter = if (filter == t) null else t }, label = { Text(label) })
                        }
                    }
                }
                if (saved != null && list.isEmpty()) item {
                    EmptyState(Icons.Rounded.BookmarkBorder, stringResource(R.string.saved_empty_title), stringResource(R.string.saved_empty_body))
                }
                items(shown, key = { it.article.id }) { fi ->
                    SwipeToRemove(onRemove = { app.toggleSave(fi) }, modifier = Modifier.animateItem()) {
                        ArticleRow(fi, sharedKey = "img-saved-${fi.article.id}")
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = bottomPadding + 24.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                if (history.isEmpty()) item {
                    EmptyState(Icons.Rounded.History, stringResource(R.string.history_empty_title), stringResource(R.string.history_empty_body))
                } else item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { vm.clearHistory() }) { Text(stringResource(R.string.action_clear_history)) }
                    }
                }
                items(history.filter { matches(it.title, it.source) }, key = { "h-" + it.id }) { h ->
                    val fi = FeedItem(
                        Article(id = h.id, title = h.title, link = h.link, realLink = h.link, source = h.source, domain = h.domain,
                            published = h.openedAt, image = h.image),
                        read = true,
                    )
                    ArticleRow(fi, sharedKey = "img-hist-${h.id}", modifier = Modifier.animateItem())
                }
            }
        }
    }
}

@Composable
private fun SwipeToRemove(onRemove: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { v ->
            if (v == SwipeToDismissBoxValue.EndToStart) {
                onRemove(); true
            } else false
        },
    )
    LaunchedEffect(Unit) { }
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        modifier = modifier,
        backgroundContent = {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(vertical = 6.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(Icons.Rounded.Delete, stringResource(R.string.action_remove), tint = MaterialTheme.colorScheme.onErrorContainer)
            }
        },
    ) {
        Box(Modifier.background(MaterialTheme.colorScheme.background)) { content() }
    }
}

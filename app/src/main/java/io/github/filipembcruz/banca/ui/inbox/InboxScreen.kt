package io.github.filipembcruz.banca.ui.inbox

import android.app.Application
import android.text.format.DateUtils
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import io.github.filipembcruz.banca.R
import io.github.filipembcruz.banca.core.container
import io.github.filipembcruz.banca.data.model.FeedItem
import io.github.filipembcruz.banca.ui.components.ArticleRow
import io.github.filipembcruz.banca.ui.components.EmptyState
import io.github.filipembcruz.banca.ui.components.SectionHeader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class DigestGroup(val at: Long, val items: List<FeedItem>)
data class Inbox(val digests: List<DigestGroup>, val alerts: List<FeedItem>)

class InboxViewModel(app: Application) : AndroidViewModel(app) {
    private val c = app.container

    val inbox: StateFlow<Inbox?> = c.store.changes
        .map {
            val saved = c.store.savedIds()
            val read = c.store.readIds()
            fun wrap(a: io.github.filipembcruz.banca.data.model.Article) = FeedItem(a, read = a.id in read, saved = a.id in saved)
            Inbox(
                digests = c.store.digests().take(5).map { (at, ids) ->
                    DigestGroup(at, ids.mapNotNull { c.store.findArticle(it) }.map(::wrap))
                }.filter { it.items.isNotEmpty() },
                alerts = c.store.recentAlerts().map(::wrap),
            )
        }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

/** What the notifications brought: each digest as a tappable list, then recent alerts. */
@Composable
fun InboxScreen(vm: InboxViewModel, onBack: () -> Unit, onSettings: () -> Unit) {
    val inbox by vm.inbox.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.inbox_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back)) }
                },
                actions = {
                    IconButton(onClick = onSettings) { Icon(Icons.Rounded.Settings, stringResource(R.string.settings_notifications)) }
                },
                scrollBehavior = scroll,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { pad ->
        val data = inbox
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = pad.calculateTopPadding(), bottom = 32.dp),
            modifier = Modifier.fillMaxSize().navigationBarsPadding(),
        ) {
            if (data != null && data.digests.isEmpty() && data.alerts.isEmpty()) {
                item {
                    EmptyState(
                        Icons.Rounded.NotificationsNone,
                        stringResource(R.string.inbox_empty_title),
                        stringResource(R.string.inbox_empty_body),
                        action = stringResource(R.string.settings_notifications),
                        onAction = onSettings,
                    )
                }
            }
            data?.digests?.forEachIndexed { gi, g ->
                item(key = "dh-${g.at}") {
                    SectionHeader(
                        stringResource(
                            R.string.inbox_digest_at,
                            DateUtils.formatDateTime(context, g.at, DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_ABBREV_WEEKDAY),
                        ),
                        Modifier.padding(top = if (gi == 0) 0.dp else 8.dp),
                    )
                }
                items(g.items, key = { "d-${g.at}-${it.article.id}" }) { fi ->
                    ArticleRow(fi, sharedKey = "img-inbox-${g.at}-${fi.article.id}", modifier = Modifier.animateItem())
                }
            }
            if (!data?.alerts.isNullOrEmpty()) {
                item(key = "ah") { SectionHeader(stringResource(R.string.inbox_alerts)) }
                items(data!!.alerts, key = { "a-${it.article.id}" }) { fi ->
                    ArticleRow(fi, sharedKey = "img-inbox-a-${fi.article.id}", modifier = Modifier.animateItem())
                }
            }
        }
    }
}

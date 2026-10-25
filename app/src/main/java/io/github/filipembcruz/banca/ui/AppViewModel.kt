package io.github.filipembcruz.banca.ui

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.filipembcruz.banca.R
import io.github.filipembcruz.banca.core.container
import io.github.filipembcruz.banca.data.model.Article
import io.github.filipembcruz.banca.data.model.FeedItem
import io.github.filipembcruz.banca.data.model.Related
import io.github.filipembcruz.banca.data.model.TabDef
import io.github.filipembcruz.banca.data.model.TabType
import io.github.filipembcruz.banca.data.model.Topics
import io.github.filipembcruz.banca.data.settings.AppSettings
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A snackbar message, optionally with Undo. */
data class UiMessage(@StringRes val text: Int, val arg: String? = null, val undo: (() -> Unit)? = null)

/** App-wide state and the actions any screen can trigger on an article. */
class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val c = app.container
    val news = c.news

    val settings: StateFlow<AppSettings?> = c.settings.flow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _messages = Channel<UiMessage>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { c.settings.update(transform) }
    }

    private fun say(@StringRes text: Int, arg: String? = null, undo: (() -> Unit)? = null) {
        _messages.trySend(UiMessage(text, arg, undo))
    }

    fun toggleSave(item: FeedItem) {
        viewModelScope.launch {
            if (item.saved) {
                news.unsave(item.article.id)
                say(R.string.msg_removed_saved, undo = { save(item.article) })
            } else {
                news.save(item.article)
                say(R.string.msg_saved, undo = { viewModelScope.launch { news.unsave(item.article.id) } })
            }
        }
    }

    fun save(a: Article) {
        viewModelScope.launch { news.save(a) }
    }

    fun hide(a: Article) {
        viewModelScope.launch {
            news.hide(a.id)
            say(R.string.msg_hidden, undo = { viewModelScope.launch { news.unhide(a.id) } })
        }
    }

    fun showLessTopic(a: Article, label: String) {
        val topic = a.topic ?: return
        viewModelScope.launch {
            val prev = news.showLessTopic(topic)
            say(R.string.msg_less_topic, label, undo = { viewModelScope.launch { news.setTopicWeight(topic, prev) } })
        }
    }

    fun showLessSource(a: Article) {
        val domain = a.domain ?: a.source
        viewModelScope.launch {
            val prev = news.showLessSource(domain, a.source)
            say(R.string.msg_less_source, a.source, undo = { viewModelScope.launch { news.setSourceWeight(domain, a.source, prev) } })
        }
    }

    fun block(a: Article) {
        val domain = a.domain ?: a.source
        viewModelScope.launch {
            news.block(domain, a.source, true)
            say(R.string.msg_blocked, a.source, undo = { viewModelScope.launch { news.block(domain, a.source, false) } })
        }
    }

    fun followSource(a: Article) {
        val domain = a.domain ?: return
        val id = "src-$domain"
        updateSettings { s ->
            if (s.tabs.any { it.id == id }) s
            else s.copy(tabs = s.tabs + TabDef(id, TabType.SEARCH, "site:$domain", a.source))
        }
        say(R.string.msg_followed, a.source)
    }

    fun followTopic(topic: String, label: String) {
        if (Topics.byId(topic) == null) return
        updateSettings { s -> s.copy(topics = s.topics + topic) }
        say(R.string.msg_followed, label)
    }

    fun addSearchTab(query: String) {
        val id = "q-" + query.lowercase().hashCode().toUInt().toString(16)
        updateSettings { s ->
            if (s.tabs.any { it.id == id }) s
            else s.copy(tabs = s.tabs + TabDef(id, TabType.SEARCH, query, query))
        }
        say(R.string.msg_tab_added, query)
    }

    suspend fun relatedId(r: Related, parent: Article): String = news.openRelated(r, parent)

    fun notify(@StringRes text: Int, arg: String? = null) = say(text, arg)
}

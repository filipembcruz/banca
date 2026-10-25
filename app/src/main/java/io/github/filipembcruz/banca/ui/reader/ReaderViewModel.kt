package io.github.filipembcruz.banca.ui.reader

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import io.github.filipembcruz.banca.core.container
import io.github.filipembcruz.banca.data.model.Article
import io.github.filipembcruz.banca.data.net.Extracted
import io.github.filipembcruz.banca.data.net.Summarizer
import io.github.filipembcruz.banca.data.net.Summary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ReaderViewModel(app: Application, handle: SavedStateHandle) : AndroidViewModel(app) {

    private val c = app.container
    val id: String = handle.get<String>("id").orEmpty()

    sealed interface Body {
        data object Loading : Body
        data object Failed : Body
        data class Ready(val content: Extracted) : Body
    }

    sealed interface SummaryState {
        data object Hidden : SummaryState
        data object Working : SummaryState
        data object Unavailable : SummaryState
        data class Ready(val summary: Summary) : SummaryState
    }

    private val _article = MutableStateFlow<Article?>(null)
    val article: StateFlow<Article?> = _article

    private val _body = MutableStateFlow<Body>(Body.Loading)
    val body: StateFlow<Body> = _body

    private val _url = MutableStateFlow<String?>(null)
    val url: StateFlow<String?> = _url

    private val _readerMode = MutableStateFlow<Boolean?>(null)
    val readerMode: StateFlow<Boolean?> = _readerMode

    private val _summary = MutableStateFlow<SummaryState>(SummaryState.Hidden)
    val summary: StateFlow<SummaryState> = _summary

    val saved: StateFlow<Boolean> = combine(c.store.changes, _article) { _, a ->
        a != null && c.store.savedIds().contains(a.id)
    }.flowOn(Dispatchers.IO).stateIn(viewModelScope, SharingStarted.Eagerly, false)

    init {
        viewModelScope.launch {
            val a = c.news.article(id) ?: run {
                _body.value = Body.Failed
                return@launch
            }
            _article.value = a
            val s = c.settings.current()
            _readerMode.value = s.readerDefault
            c.news.opened(a)
            _url.value = c.news.realLink(a)
            val content = c.news.extract(a)
            _body.value = if (content != null) Body.Ready(content) else Body.Failed
            if (content == null && _readerMode.value == true) _readerMode.value = false
        }
    }

    fun setReaderMode(on: Boolean) {
        _readerMode.value = on
    }

    fun summarize() {
        val a = _article.value ?: return
        if (_summary.value is SummaryState.Ready) return
        _summary.value = SummaryState.Working
        viewModelScope.launch {
            val s = c.settings.current()
            Summarizer.publisher(a.summary, a.title)?.let {
                _summary.value = SummaryState.Ready(it)
                return@launch
            }
            // Wait for the article text if it is still loading.
            val body = _body.first { it !is Body.Loading }
            val paragraphs = (body as? Body.Ready)?.content?.blocks?.filter { !it.heading }?.map { it.text }.orEmpty()
            val result = Summarizer.keySentences(paragraphs, a.title, s.summaryLength.coerceIn(3, 5))
            _summary.value = if (result != null && result.points.isNotEmpty()) SummaryState.Ready(result) else SummaryState.Unavailable
        }
    }

    fun toggleSave() {
        val a = _article.value ?: return
        viewModelScope.launch {
            if (saved.value) c.news.unsave(a.id) else {
                c.news.save(a)
                (body.value as? Body.Ready)?.let { ready ->
                    withContext(Dispatchers.IO) { c.store.setSavedBody(a.id, c.news.encodeBody(ready.content)) }
                }
            }
        }
    }
}

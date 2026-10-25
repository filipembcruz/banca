package io.github.filipembcruz.banca.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.filipembcruz.banca.core.container
import io.github.filipembcruz.banca.data.model.FeedItem
import io.github.filipembcruz.banca.data.model.Place
import io.github.filipembcruz.banca.data.model.Quote
import io.github.filipembcruz.banca.data.model.TabDef
import io.github.filipembcruz.banca.data.model.Weather
import io.github.filipembcruz.banca.data.model.MarketDef
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val c = app.container
    private val feeds = HashMap<String, StateFlow<List<FeedItem>?>>()

    private val _refreshing = MutableStateFlow<Set<String>>(emptySet())
    val refreshing: StateFlow<Set<String>> = _refreshing

    private val _failed = MutableStateFlow<Set<String>>(emptySet())
    val failed: StateFlow<Set<String>> = _failed

    val weather: StateFlow<Map<String, Weather>> = c.data.weather
    val quotes: StateFlow<Map<String, Quote>> = c.data.quotes

    val marketNews: StateFlow<List<FeedItem>?> =
        c.news.marketNews().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun feed(tab: TabDef): StateFlow<List<FeedItem>?> = feeds.getOrPut(tab.id) {
        c.news.feed(tab.id, tab.type).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    }

    fun refresh(tab: TabDef, force: Boolean = false) {
        if (tab.id in _refreshing.value) return
        viewModelScope.launch {
            _refreshing.update { it + tab.id }
            val ok = runCatching { c.news.refresh(tab, force) }.getOrDefault(false)
            _failed.update { if (ok) it - tab.id else it + tab.id }
            _refreshing.update { it - tab.id }
        }
    }

    fun loadData(places: List<Place>, markets: List<String>, force: Boolean = false) {
        viewModelScope.launch { runCatching { c.data.loadWeather(places, force) } }
        viewModelScope.launch { runCatching { c.data.loadQuotes(markets, force) } }
    }

    suspend fun history(m: MarketDef, range: String): Quote? = c.data.history(m, range)

    fun lastUpdated(tabId: String): Long = c.store.tabFetchedAt(tabId)
}

package io.github.filipembcruz.banca.data.repo

import io.github.filipembcruz.banca.data.db.Store
import io.github.filipembcruz.banca.data.model.MarketDef
import io.github.filipembcruz.banca.data.model.Markets
import io.github.filipembcruz.banca.data.model.Place
import io.github.filipembcruz.banca.data.model.Quote
import io.github.filipembcruz.banca.data.model.Weather
import io.github.filipembcruz.banca.data.net.MarketApi
import io.github.filipembcruz.banca.data.net.WeatherApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

/** Weather and market quotes, cached in the database so the cards show instantly. */
class DataRepository(
    private val store: Store,
    private val weatherApi: WeatherApi,
    private val marketApi: MarketApi,
) {
    private val _weather = MutableStateFlow<Map<String, Weather>>(emptyMap())
    val weather: StateFlow<Map<String, Weather>> = _weather

    private val _quotes = MutableStateFlow<Map<String, Quote>>(emptyMap())
    val quotes: StateFlow<Map<String, Quote>> = _quotes

    suspend fun loadWeather(places: List<Place>, force: Boolean = false) = withContext(Dispatchers.IO) {
        val result = coroutineScope {
            places.map { p ->
                async {
                    val key = "weather:${p.key}"
                    val cached = store.kvGet(key)?.let { (v, at) -> weatherApi.fromJson(p, v)?.takeIf { true } to at }
                    val fresh = cached?.first != null && System.currentTimeMillis() - cached.second < WEATHER_TTL
                    if (fresh && !force) return@async p.key to cached!!.first!!
                    // Show the cached value right away while the network call runs.
                    cached?.first?.let { w -> _weather.value = _weather.value + (p.key to w) }
                    val w = runCatching { weatherApi.forecast(p) }.getOrNull()
                    if (w != null) runCatching { store.kvPut(key, weatherApi.toJson(w)) }
                    p.key to (w ?: cached?.first)
                }
            }.awaitAll()
        }
        _weather.value = result.mapNotNull { (k, w) -> w?.let { k to it } }.toMap()
    }

    suspend fun loadQuotes(ids: List<String>, force: Boolean = false) = withContext(Dispatchers.IO) {
        val defs = ids.mapNotNull { Markets.byId(it) }
        val cachedAll = defs.mapNotNull { m -> store.kvGet("quote:${m.id}")?.let { (v, _) -> marketApi.fromJson(v) }?.let { m.id to it } }.toMap()
        if (_quotes.value.isEmpty() && cachedAll.isNotEmpty()) _quotes.value = cachedAll
        val result = coroutineScope {
            defs.map { m ->
                async {
                    val cached = store.kvGet("quote:${m.id}")
                    val fresh = cached != null && System.currentTimeMillis() - cached.second < QUOTE_TTL
                    if (fresh && !force) return@async m.id to cachedAll[m.id]
                    val q = runCatching { marketApi.quote(m) }.getOrNull()?.takeIf { !it.price.isNaN() }
                    if (q != null) runCatching { store.kvPut("quote:${m.id}", marketApi.toJson(q)) }
                    m.id to (q ?: cachedAll[m.id])
                }
            }.awaitAll()
        }
        _quotes.value = result.mapNotNull { (k, q) -> q?.let { k to it } }.toMap()
    }

    /** Longer history for the detail sheet. */
    suspend fun history(m: MarketDef, range: String): Quote? = withContext(Dispatchers.IO) {
        val interval = when (range) {
            "1d" -> "5m"
            "5d" -> "30m"
            "1mo", "3mo" -> "1d"
            else -> "1wk"
        }
        runCatching { marketApi.quote(m, range, interval) }.getOrNull()
    }

    suspend fun searchPlaces(query: String, language: String): List<Place> = withContext(Dispatchers.IO) {
        runCatching { weatherApi.searchPlaces(query, language) }.getOrDefault(emptyList())
    }

    companion object {
        const val WEATHER_TTL = 30 * 60 * 1000L
        const val QUOTE_TTL = 10 * 60 * 1000L
    }
}

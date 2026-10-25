package io.github.filipembcruz.banca.ui.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import coil.imageLoader
import io.github.filipembcruz.banca.core.LocaleHelper
import io.github.filipembcruz.banca.core.container
import io.github.filipembcruz.banca.data.db.SourceInfo
import io.github.filipembcruz.banca.data.model.Place
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val c = app.container

    private val _sources = MutableStateFlow<List<SourceInfo>>(emptyList())
    val sources: StateFlow<List<SourceInfo>> = _sources

    private val _topicWeights = MutableStateFlow<Map<String, Double>>(emptyMap())
    val topicWeights: StateFlow<Map<String, Double>> = _topicWeights

    private val _placeResults = MutableStateFlow<List<Place>>(emptyList())
    val placeResults: StateFlow<List<Place>> = _placeResults

    private val _searching = MutableStateFlow(false)
    val searching: StateFlow<Boolean> = _searching

    private var searchJob: Job? = null

    fun loadPrefs() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                _sources.value = c.store.sourcePrefs().values.filter { it.blocked || it.weight <= -1.0 }.sortedBy { it.name.lowercase() }
                _topicWeights.value = c.store.topicWeights()
            }
        }
    }

    fun unblock(s: SourceInfo) {
        viewModelScope.launch {
            c.news.block(s.domain, s.name, false)
            if (s.weight < 0) c.news.setSourceWeight(s.domain, s.name, 0.0)
            loadPrefs()
        }
    }

    fun resetTopic(topic: String) {
        viewModelScope.launch {
            c.news.setTopicWeight(topic, 0.0)
            loadPrefs()
        }
    }

    fun resetLearning() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { c.store.resetLearning() }
            loadPrefs()
        }
    }

    fun clearHidden() {
        viewModelScope.launch(Dispatchers.IO) { c.store.clearHidden() }
    }

    fun searchPlaces(query: String, language: String) {
        searchJob?.cancel()
        if (query.trim().length < 2) {
            _placeResults.value = emptyList()
            return
        }
        searchJob = viewModelScope.launch {
            delay(350)
            _searching.value = true
            _placeResults.value = c.data.searchPlaces(query.trim(), language)
            _searching.value = false
        }
    }

    fun clearPlaceResults() {
        _placeResults.value = emptyList()
    }

    @OptIn(coil.annotation.ExperimentalCoilApi::class)
    fun clearCache(done: () -> Unit) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                c.store.clearCache()
                c.http.clearCache()
                runCatching { getApplication<Application>().imageLoader.diskCache?.clear() }
            }
            runCatching { getApplication<Application>().imageLoader.memoryCache?.clear() }
            done()
        }
    }

    fun cacheSizeMb(): Long {
        val dir = getApplication<Application>().cacheDir
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() } / (1024 * 1024)
    }

    /** Settings + blocked/muted sources as one JSON file. */
    fun export(uri: Uri, done: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    val o = JSONObject()
                    o.put("app", "banca")
                    o.put("version", 1)
                    o.put("language", LocaleHelper.current(getApplication()))
                    o.put("settings", JSONObject(c.settings.export()))
                    o.put("sources", JSONArray(c.store.sourcePrefs().values.map {
                        JSONObject().put("domain", it.domain).put("name", it.name).put("weight", it.weight).put("blocked", it.blocked)
                    }))
                    getApplication<Application>().contentResolver.openOutputStream(uri)?.use { it.write(o.toString(2).toByteArray()) }
                        ?: error("no stream")
                }.isSuccess
            }
            done(ok)
        }
    }

    fun importFrom(uri: Uri, done: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    val text = getApplication<Application>().contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                        ?: error("no stream")
                    val o = JSONObject(text)
                    require(o.optString("app") == "banca")
                    c.settings.replace(o.getJSONObject("settings").toString())
                    val sources = o.optJSONArray("sources")
                    if (sources != null) {
                        for (i in 0 until sources.length()) {
                            val s = sources.getJSONObject(i)
                            val domain = s.getString("domain")
                            val name = s.optString("name", domain)
                            c.store.setBlocked(domain, name, s.optBoolean("blocked"))
                            c.store.setSourceWeight(domain, name, s.optDouble("weight", 0.0))
                        }
                    }
                }.isSuccess
            }
            done(ok)
        }
    }
}

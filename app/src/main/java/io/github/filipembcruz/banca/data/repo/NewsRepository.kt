package io.github.filipembcruz.banca.data.repo

import io.github.filipembcruz.banca.data.db.SourceInfo
import io.github.filipembcruz.banca.data.db.Store
import io.github.filipembcruz.banca.data.model.Article
import io.github.filipembcruz.banca.data.model.Editions
import io.github.filipembcruz.banca.data.model.FeedItem
import io.github.filipembcruz.banca.data.model.Markets
import io.github.filipembcruz.banca.data.model.Related
import io.github.filipembcruz.banca.data.model.TabDef
import io.github.filipembcruz.banca.data.model.TabType
import io.github.filipembcruz.banca.data.net.ArticleExtractor
import io.github.filipembcruz.banca.data.net.Block
import io.github.filipembcruz.banca.data.net.Extracted
import io.github.filipembcruz.banca.data.net.FeedCatalog
import io.github.filipembcruz.banca.data.net.FeedUrl
import io.github.filipembcruz.banca.data.net.GoogleNews
import io.github.filipembcruz.banca.data.net.Http
import io.github.filipembcruz.banca.data.net.LinkResolver
import io.github.filipembcruz.banca.data.net.RssParser
import io.github.filipembcruz.banca.data.settings.AppSettings
import io.github.filipembcruz.banca.data.settings.SettingsStore
import io.github.filipembcruz.banca.data.settings.objects
import io.github.filipembcruz.banca.core.Text
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.exp
import kotlin.math.ln

class NewsRepository(
    private val store: Store,
    private val settings: SettingsStore,
    private val http: Http,
    private val resolver: LinkResolver,
) {
    private val tabLocks = ConcurrentHashMap<String, Mutex>()
    private val imageGate = Semaphore(4)
    private val extracted = object : LinkedHashMap<String, Extracted>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Extracted>?) = size > 24
    }

    // ---------------- feeds ----------------

    fun feed(tabId: String, type: TabType): Flow<List<FeedItem>> =
        combine(store.changes, settings.flow) { _, s -> load(tabId, type, s) }.flowOn(Dispatchers.IO)

    fun lastUpdated(tabId: String): Long = store.tabFetchedAt(tabId)

    private fun load(tabId: String, type: TabType, s: AppSettings): List<FeedItem> {
        val items = store.tabItems(tabId)
        val hidden = store.hiddenIds()
        val sources = store.sourcePrefs()
        val blocked = sources.values.filter { it.blocked }
        val read = store.readIds()
        val saved = store.savedIds()
        val topicWeights = store.topicWeights()

        val visible = items.filter { a -> a.id !in hidden && !isBlocked(a, blocked) }
            .map { a -> a.copy(related = a.related.filterNot { r -> blocked.any { b -> b.name.equals(r.source, true) } }) }

        val now = System.currentTimeMillis()
        val ordered = when (type) {
            TabType.FOR_YOU -> visible.sortedByDescending { score(it, now, topicWeights, sources) }
            TabType.TRENDING -> visible.sortedByDescending { trendScore(it, now) }
            else -> visible.sortedWith(
                compareByDescending<Article> { a -> (a.domain?.let { sources[it] }?.weight ?: 0.0) > -2.5 }.thenByDescending { it.published }
            )
        }
        return ordered.map { FeedItem(it, read = it.id in read, saved = it.id in saved) }
    }

    private fun isBlocked(a: Article, blocked: List<SourceInfo>): Boolean =
        blocked.any { b -> b.domain == a.domain || b.name.equals(a.source, ignoreCase = true) }

    private fun score(a: Article, now: Long, topics: Map<String, Double>, sources: Map<String, SourceInfo>): Double {
        val ageH = ((now - a.published).coerceAtLeast(0L)) / 3_600_000.0
        val recency = if (a.published == 0L) 0.3 else exp(-ageH / 14.0)
        val tw = a.topic?.let { topics[it] } ?: 0.0
        val sw = a.domain?.let { sources[it] }?.weight ?: 0.0
        val coverage = ln(1.0 + a.related.size) * 0.12
        val picture = if (a.image != null) 0.05 else 0.0
        return recency * (1.0 + 0.14 * tw + 0.12 * sw) + coverage + picture
    }

    private fun trendScore(a: Article, now: Long): Double {
        val ageH = ((now - a.published).coerceAtLeast(0L)) / 3_600_000.0
        val recency = if (a.published == 0L) 0.2 else exp(-ageH / 10.0)
        return a.related.size * 0.6 + recency * 2.0
    }

    /** Downloads the feeds behind a tab and stores them. Returns false when nothing could be fetched. */
    suspend fun refresh(tab: TabDef, force: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        val lock = tabLocks.getOrPut(tab.id) { Mutex() }
        lock.withLock {
            if (!force && System.currentTimeMillis() - store.tabFetchedAt(tab.id) < FRESH_MS) return@withLock true
            val s = settings.current()
            val urls = urlsFor(tab, s)
            if (urls.isEmpty()) {
                store.replaceTab(tab.id, emptyList())
                return@withLock true
            }
            val fetched = fetchAll(urls, perFeed = if (tab.type == TabType.FOR_YOU) 14 else 30)
            if (fetched == null) return@withLock false
            store.replaceTab(tab.id, Clusterer.cluster(fetched))
            if (tab.type == TabType.FOR_YOU) refreshMarketNews(s)
            true
        }
    }

    private suspend fun refreshMarketNews(s: AppSettings) {
        val e = Editions.byId(s.edition)
        val urls = s.markets.mapNotNull { Markets.byId(it) }.filter { !it.isCurrency && it.newsEn.isNotBlank() }
            .map { m -> FeedUrl(GoogleNews.search(e, if (e.isPt) m.newsPt else m.newsEn, "1d"), "markets") }
        if (urls.isEmpty()) {
            store.replaceTab(MARKET_NEWS, emptyList())
            return
        }
        val items = fetchAll(urls, perFeed = 6) ?: return
        store.replaceTab(MARKET_NEWS, Clusterer.cluster(items).sortedByDescending { it.published }.take(6))
    }

    /** Runs a search and keeps the results under the "search" tab so the reader can open them. */
    suspend fun search(query: String, window: String?): List<FeedItem> = withContext(Dispatchers.IO) {
        val s = settings.current()
        val e = Editions.byId(s.edition)
        val items = fetchAll(listOf(FeedUrl(GoogleNews.search(e, query, window), null)), perFeed = 60)
            ?: throw IOException("search failed")
        val clustered = Clusterer.cluster(items).sortedByDescending { it.published }
        store.replaceTab(SEARCH, clustered)
        load(SEARCH, TabType.SEARCH, s)
    }

    /** Fetches several feeds in parallel. Null when every feed failed. */
    suspend fun fetchAll(urls: List<FeedUrl>, perFeed: Int): List<Article>? = coroutineScope {
        val results = urls.map { u ->
            async(Dispatchers.IO) {
                runCatching { RssParser.parse(http.getText(u.url), u.topic).take(perFeed) }.getOrNull()
            }
        }.awaitAll()
        if (results.all { it == null }) null else results.filterNotNull().flatten()
    }

    fun urlsFor(tab: TabDef, s: AppSettings): List<FeedUrl> {
        val e = Editions.byId(s.edition)
        return when (tab.type) {
            TabType.FOR_YOU -> buildList {
                add(FeedUrl(GoogleNews.top(e), "top"))
                s.topics.forEach { addAll(FeedCatalog.forTopicId(e, it)) }
                s.terms.forEach { add(FeedCatalog.forTerm(e, it)) }
            }
            TabType.TRENDING -> listOf(
                FeedUrl(GoogleNews.top(e), "top"),
                FeedUrl(GoogleNews.topic(e, "NATION"), "nation"),
                FeedUrl(GoogleNews.topic(e, "WORLD"), "world"),
            )
            TabType.LOCAL -> s.places.map { FeedCatalog.forPlace(e, it) }
            TabType.TOPIC -> FeedCatalog.forTopicId(e, tab.value)
            TabType.SEARCH -> listOf(FeedCatalog.forTerm(e, tab.value, "7d"))
            TabType.FEED -> listOf(FeedUrl(tab.value, null))
        }
    }

    fun marketNews(): Flow<List<FeedItem>> = feed(MARKET_NEWS, TabType.SEARCH)

    // ---------------- articles ----------------

    suspend fun article(id: String): Article? = withContext(Dispatchers.IO) { store.findArticle(id) }

    /** Stores an outlet from "other coverage" so it can open in the reader. */
    suspend fun openRelated(r: Related, parent: Article): String = withContext(Dispatchers.IO) {
        val a = Article(
            id = Text.idFor(r.link),
            title = r.title,
            link = r.link,
            realLink = if (r.link.contains("news.google.com")) null else r.link,
            source = r.source.ifBlank { parent.source },
            domain = if (r.link.contains("news.google.com")) null else RssParser.hostOf(r.link),
            published = parent.published,
            topic = parent.topic,
        )
        store.putTransient(RELATED, a)
        a.id
    }

    suspend fun realLink(a: Article): String = withContext(Dispatchers.IO) {
        a.realLink?.let { return@withContext it }
        store.linkInfo(a.link)?.first?.let { return@withContext it }
        if (!a.isGoogleLink) return@withContext a.link
        val real = resolver.decodeGoogle(a.link)
        if (real != null) store.setResolved(a.link, real, null)
        real ?: a.link
    }

    /** Picture for a card without one: the publisher page's share image, cached. */
    suspend fun image(a: Article): String? = withContext(Dispatchers.IO) {
        a.image?.let { return@withContext it }
        val known = store.linkInfo(a.link)
        // "" marks a page already checked that has no share image.
        known?.second?.let { return@withContext it.ifEmpty { null } }
        imageGate.withPermit {
            val real = known?.first ?: if (a.isGoogleLink) resolver.decodeGoogle(a.link) else a.link
            if (real == null || real.contains("news.google.com")) {
                store.setResolved(a.link, null, "")
                return@withPermit null
            }
            val img = resolver.shareImage(real)
            store.setResolved(a.link, real, img ?: "")
            img
        }
    }

    suspend fun extract(a: Article): Extracted? = withContext(Dispatchers.IO) {
        synchronized(extracted) { extracted[a.id] }?.let { return@withContext it }
        store.savedBody(a.id)?.let { body -> decodeBody(a, body)?.let { return@withContext it } }
        val url = realLink(a)
        if (url.contains("news.google.com")) return@withContext null
        val result = runCatching {
            http.execute(http.request(url).build()).use { r ->
                if (!r.isSuccessful) return@use null
                val html = r.body?.string() ?: return@use null
                ArticleExtractor.parse(html, r.request.url.toString())
            }
        }.getOrNull()
        if (result != null) {
            synchronized(extracted) { extracted[a.id] = result }
            if (a.image == null && result.image != null) store.setResolved(a.link, url, result.image)
        }
        result
    }

    fun encodeBody(e: Extracted): String = JSONObject()
        .put("url", e.url).put("title", e.title).put("image", e.image).put("site", e.site).put("author", e.author)
        .put("blocks", JSONArray(e.blocks.map { JSONObject().put("t", it.text).put("h", it.heading).put("q", it.quote) }))
        .toString()

    private fun decodeBody(a: Article, body: String): Extracted? = runCatching {
        val o = JSONObject(body)
        Extracted(
            url = o.optString("url", a.bestLink),
            title = o.optString("title", a.title),
            image = o.optString("image").takeIf { it.isNotBlank() && it != "null" },
            site = o.optString("site").takeIf { it.isNotBlank() && it != "null" },
            author = o.optString("author").takeIf { it.isNotBlank() && it != "null" },
            blocks = o.getJSONArray("blocks").objects().map { Block(it.optString("t"), it.optBoolean("h"), it.optBoolean("q")) },
        )
    }.getOrNull()

    // ---------------- user actions ----------------

    suspend fun opened(a: Article) = withContext(Dispatchers.IO) {
        store.markOpened(a)
        a.topic?.let { store.adjustTopic(it, 0.4, notify = false) }
        a.domain?.let { store.adjustSource(it, a.source, 0.25, notify = false) }
    }

    suspend fun save(a: Article) = withContext(Dispatchers.IO) {
        store.save(a)
        a.topic?.let { store.adjustTopic(it, 1.0, notify = false) }
        // Keep the text for offline reading.
        runCatching { extract(a)?.let { store.setSavedBody(a.id, encodeBody(it)) } }
    }

    suspend fun unsave(id: String) = withContext(Dispatchers.IO) { store.unsave(id) }
    suspend fun hide(id: String) = withContext(Dispatchers.IO) { store.hide(id) }
    suspend fun unhide(id: String) = withContext(Dispatchers.IO) { store.unhide(id) }

    suspend fun showLessTopic(topic: String): Double = withContext(Dispatchers.IO) {
        val prev = store.topicWeights()[topic] ?: 0.0
        store.setTopicWeight(topic, minOf(prev, 0.0) - 3.0)
        prev
    }

    suspend fun setTopicWeight(topic: String, w: Double) = withContext(Dispatchers.IO) { store.setTopicWeight(topic, w) }

    suspend fun showLessSource(domain: String, name: String): Double = withContext(Dispatchers.IO) {
        val prev = store.sourcePrefs()[domain]?.weight ?: 0.0
        store.setSourceWeight(domain, name, minOf(prev, 0.0) - 3.0)
        prev
    }

    suspend fun setSourceWeight(domain: String, name: String, w: Double) = withContext(Dispatchers.IO) {
        store.setSourceWeight(domain, name, w)
    }

    suspend fun block(domain: String, name: String, blocked: Boolean) = withContext(Dispatchers.IO) {
        store.setBlocked(domain, name, blocked)
    }

    companion object {
        const val FRESH_MS = 10 * 60 * 1000L
        const val MARKET_NEWS = "marketnews"
        const val SEARCH = "search"
        const val RELATED = "related"
    }
}

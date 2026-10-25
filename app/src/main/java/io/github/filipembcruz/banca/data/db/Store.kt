package io.github.filipembcruz.banca.data.db

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import io.github.filipembcruz.banca.data.model.Article
import io.github.filipembcruz.banca.data.model.Related
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject

data class SourceInfo(val domain: String, val name: String, val weight: Double, val blocked: Boolean, val count: Int = 0)

data class SavedArticle(val article: Article, val savedAt: Long, val body: String?)

data class HistoryEntry(val id: String, val title: String, val link: String, val source: String, val domain: String?, val image: String?, val openedAt: Long)

/**
 * All database access. Writes bump [changes]; screens re-query when it changes.
 * Callers run these on Dispatchers.IO.
 */
class Store(private val helper: BancaDb) {

    private val _changes = MutableStateFlow(0L)
    val changes: StateFlow<Long> = _changes

    fun notifyChanged() {
        _changes.value = _changes.value + 1
    }

    private val db: SQLiteDatabase get() = helper.writableDatabase

    // ---------- items ----------

    fun replaceTab(tab: String, articles: List<Article>) {
        val now = System.currentTimeMillis()
        db.beginTransaction()
        try {
            db.delete("items", "tab = ?", arrayOf(tab))
            articles.forEachIndexed { i, a ->
                val known = linkInfo(a.link)
                db.insertWithOnConflict("items", null, a.toValues().apply {
                    put("tab", tab)
                    put("position", i)
                    put("fetched", now)
                    if (a.realLink == null && known?.first != null) put("real_link", known.first)
                    val knownImage = known?.second
                    if (a.image == null && !knownImage.isNullOrEmpty()) put("image", knownImage)
                }, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        notifyChanged()
    }

    /** Inserts single articles (search results, related coverage) so the reader can open them by id. */
    fun putTransient(tab: String, a: Article) {
        db.insertWithOnConflict("items", null, a.toValues().apply {
            put("tab", tab)
            put("fetched", System.currentTimeMillis())
        }, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun tabItems(tab: String): List<Article> =
        db.rawQuery("SELECT * FROM items WHERE tab = ? ORDER BY position", arrayOf(tab)).use { c ->
            c.list { it.toArticle() }
        }

    fun tabFetchedAt(tab: String): Long =
        db.rawQuery("SELECT MAX(fetched) FROM items WHERE tab = ?", arrayOf(tab)).use { c ->
            if (c.moveToFirst()) c.getLong(0) else 0L
        }

    fun findArticle(id: String): Article? {
        db.rawQuery("SELECT * FROM items WHERE id = ? ORDER BY fetched DESC LIMIT 1", arrayOf(id)).use { c ->
            if (c.moveToFirst()) return c.toArticle()
        }
        db.rawQuery("SELECT * FROM saved WHERE id = ?", arrayOf(id)).use { c ->
            if (c.moveToFirst()) return c.toArticle()
        }
        db.rawQuery("SELECT * FROM history WHERE id = ?", arrayOf(id)).use { c ->
            if (c.moveToFirst()) return Article(
                id = c.str("id")!!, title = c.str("title")!!, link = c.str("link")!!,
                source = c.str("source")!!, domain = c.str("domain"), image = c.str("image"),
            )
        }
        return null
    }

    fun setResolved(link: String, real: String?, image: String?) {
        val prev = linkInfo(link)
        val r = real ?: prev?.first
        val im = image ?: prev?.second
        db.insertWithOnConflict("links", null, ContentValues().apply {
            put("link", link); put("real", r); put("image", im); put("at", System.currentTimeMillis())
        }, SQLiteDatabase.CONFLICT_REPLACE)
        val v = ContentValues()
        if (r != null) v.put("real_link", r)
        if (!im.isNullOrEmpty()) v.put("image", im)
        if (v.size() > 0) {
            db.update("items", v, "link = ?", arrayOf(link))
            db.update("saved", v, "link = ?", arrayOf(link))
        }
    }

    /** (real link, image) already resolved for a feed link, or null. */
    fun linkInfo(link: String): Pair<String?, String?>? =
        db.rawQuery("SELECT real, image FROM links WHERE link = ?", arrayOf(link)).use { c ->
            if (c.moveToFirst()) (if (c.isNull(0)) null else c.getString(0)) to (if (c.isNull(1)) null else c.getString(1)) else null
        }

    fun clearCache() {
        db.delete("items", null, null)
        db.delete("links", null, null)
        db.delete("kv", null, null)
        notifyChanged()
    }

    fun knownSources(limit: Int = 60): List<SourceInfo> {
        val prefs = sourcePrefs()
        return db.rawQuery(
            "SELECT domain, source, COUNT(*) n FROM items WHERE domain IS NOT NULL GROUP BY domain ORDER BY n DESC LIMIT ?",
            arrayOf(limit.toString())
        ).use { c ->
            c.list {
                val d = it.getString(0)
                val p = prefs[d]
                SourceInfo(d, it.getString(1), p?.weight ?: 0.0, p?.blocked ?: false, it.getInt(2))
            }
        }
    }

    // ---------- saved ----------

    fun save(a: Article, body: String? = null) {
        db.insertWithOnConflict("saved", null, a.toValues().apply {
            put("saved_at", System.currentTimeMillis())
            put("body", body)
        }, SQLiteDatabase.CONFLICT_REPLACE)
        notifyChanged()
    }

    fun setSavedBody(id: String, body: String) {
        db.update("saved", ContentValues().apply { put("body", body) }, "id = ?", arrayOf(id))
    }

    fun unsave(id: String) {
        db.delete("saved", "id = ?", arrayOf(id))
        notifyChanged()
    }

    fun savedIds(): Set<String> = db.rawQuery("SELECT id FROM saved", null).use { c -> c.list { it.getString(0) }.toSet() }

    fun saved(): List<SavedArticle> =
        db.rawQuery("SELECT * FROM saved ORDER BY saved_at DESC", null).use { c ->
            c.list { SavedArticle(it.toArticle(), it.getLong(it.getColumnIndexOrThrow("saved_at")), it.str("body")) }
        }

    fun savedBody(id: String): String? =
        db.rawQuery("SELECT body FROM saved WHERE id = ?", arrayOf(id)).use { c ->
            if (c.moveToFirst() && !c.isNull(0)) c.getString(0) else null
        }

    // ---------- history ----------

    fun markOpened(a: Article) {
        db.insertWithOnConflict("history", null, ContentValues().apply {
            put("id", a.id); put("title", a.title); put("link", a.bestLink); put("source", a.source)
            put("domain", a.domain); put("image", a.image); put("opened_at", System.currentTimeMillis())
        }, SQLiteDatabase.CONFLICT_REPLACE)
        notifyChanged()
    }

    fun readIds(): Set<String> =
        db.rawQuery("SELECT id FROM history WHERE opened_at > ?", arrayOf((System.currentTimeMillis() - 7 * DAY).toString()))
            .use { c -> c.list { it.getString(0) }.toSet() }

    fun history(limit: Int = 200): List<HistoryEntry> =
        db.rawQuery("SELECT * FROM history ORDER BY opened_at DESC LIMIT ?", arrayOf(limit.toString())).use { c ->
            c.list {
                HistoryEntry(it.str("id")!!, it.str("title")!!, it.str("link")!!, it.str("source")!!, it.str("domain"), it.str("image"),
                    it.getLong(it.getColumnIndexOrThrow("opened_at")))
            }
        }

    fun clearHistory() {
        db.delete("history", null, null)
        notifyChanged()
    }

    // ---------- hidden ----------

    fun hide(id: String) {
        db.insertWithOnConflict("hidden", null, ContentValues().apply {
            put("id", id); put("at", System.currentTimeMillis())
        }, SQLiteDatabase.CONFLICT_REPLACE)
        notifyChanged()
    }

    fun unhide(id: String) {
        db.delete("hidden", "id = ?", arrayOf(id))
        notifyChanged()
    }

    fun hiddenIds(): Set<String> = db.rawQuery("SELECT id FROM hidden", null).use { c -> c.list { it.getString(0) }.toSet() }

    fun clearHidden() {
        db.delete("hidden", null, null)
        notifyChanged()
    }

    // ---------- sources & topics (learning and preferences) ----------

    fun sourcePrefs(): Map<String, SourceInfo> =
        db.rawQuery("SELECT domain, name, weight, blocked FROM sources", null).use { c ->
            c.list { SourceInfo(it.getString(0), it.getString(1) ?: it.getString(0), it.getDouble(2), it.getInt(3) == 1) }
        }.associateBy { it.domain }

    private fun ensureSource(domain: String, name: String) {
        db.insertWithOnConflict("sources", null, ContentValues().apply {
            put("domain", domain); put("name", name); put("weight", 0.0); put("blocked", 0)
        }, SQLiteDatabase.CONFLICT_IGNORE)
    }

    fun setBlocked(domain: String, name: String, blocked: Boolean) {
        ensureSource(domain, name)
        db.update("sources", ContentValues().apply { put("blocked", if (blocked) 1 else 0) }, "domain = ?", arrayOf(domain))
        notifyChanged()
    }

    fun adjustSource(domain: String, name: String, delta: Double, notify: Boolean = true) {
        ensureSource(domain, name)
        db.execSQL(
            "UPDATE sources SET weight = MIN(10, MAX(-5, weight + ?)) WHERE domain = ?",
            arrayOf<Any>(delta, domain)
        )
        if (notify) notifyChanged()
    }

    fun setSourceWeight(domain: String, name: String, weight: Double) {
        ensureSource(domain, name)
        db.update("sources", ContentValues().apply { put("weight", weight) }, "domain = ?", arrayOf(domain))
        notifyChanged()
    }

    fun topicWeights(): Map<String, Double> =
        db.rawQuery("SELECT topic, weight FROM topics", null).use { c -> c.list { it.getString(0) to it.getDouble(1) } }.toMap()

    fun adjustTopic(topic: String, delta: Double, notify: Boolean = true) {
        db.insertWithOnConflict("topics", null, ContentValues().apply { put("topic", topic); put("weight", 0.0) },
            SQLiteDatabase.CONFLICT_IGNORE)
        db.execSQL("UPDATE topics SET weight = MIN(10, MAX(-5, weight + ?)) WHERE topic = ?", arrayOf<Any>(delta, topic))
        if (notify) notifyChanged()
    }

    fun setTopicWeight(topic: String, weight: Double) {
        db.insertWithOnConflict("topics", null, ContentValues().apply { put("topic", topic); put("weight", weight) },
            SQLiteDatabase.CONFLICT_REPLACE)
        notifyChanged()
    }

    fun resetLearning() {
        db.delete("topics", null, null)
        db.execSQL("UPDATE sources SET weight = 0")
        notifyChanged()
    }

    // ---------- notifications ----------

    fun wasNotified(id: String): Boolean =
        db.rawQuery("SELECT 1 FROM notified WHERE id = ?", arrayOf(id)).use { it.moveToFirst() }

    fun markNotified(id: String, rule: String, pending: Boolean) {
        db.insertWithOnConflict("notified", null, ContentValues().apply {
            put("id", id); put("at", System.currentTimeMillis()); put("rule", rule); put("pending", if (pending) 1 else 0)
        }, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun pendingDigest(): List<String> =
        db.rawQuery("SELECT id FROM notified WHERE pending = 1 ORDER BY at DESC", null).use { c -> c.list { it.getString(0) } }

    fun clearPending() {
        db.execSQL("UPDATE notified SET pending = 0")
    }

    fun notifiedSince(since: Long): Int =
        db.rawQuery("SELECT COUNT(*) FROM notified WHERE pending = 0 AND rule != 'digest-queue' AND at > ?", arrayOf(since.toString()))
            .use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }

    fun pruneNotified() {
        db.delete("notified", "at < ?", arrayOf((System.currentTimeMillis() - 14 * DAY).toString()))
        db.delete("links", "at < ?", arrayOf((System.currentTimeMillis() - 30 * DAY).toString()))
    }

    // ---------- key/value cache ----------

    fun kvGet(k: String): Pair<String, Long>? =
        db.rawQuery("SELECT v, at FROM kv WHERE k = ?", arrayOf(k)).use { c ->
            if (c.moveToFirst() && !c.isNull(0)) c.getString(0) to c.getLong(1) else null
        }

    fun kvPut(k: String, v: String) {
        db.insertWithOnConflict("kv", null, ContentValues().apply {
            put("k", k); put("v", v); put("at", System.currentTimeMillis())
        }, SQLiteDatabase.CONFLICT_REPLACE)
    }

    // ---------- mapping ----------

    private fun Article.toValues() = ContentValues().apply {
        put("id", id); put("title", title); put("link", link); put("real_link", realLink)
        put("source", source); put("domain", domain); put("published", published); put("image", image)
        put("summary", summary); put("topic", topic); put("related", relatedToJson(related))
    }

    private fun Cursor.toArticle() = Article(
        id = str("id")!!,
        title = str("title")!!,
        link = str("link")!!,
        realLink = str("real_link"),
        source = str("source")!!,
        domain = str("domain"),
        published = getLong(getColumnIndexOrThrow("published")),
        image = str("image"),
        summary = str("summary"),
        topic = str("topic"),
        related = relatedFromJson(str("related")),
    )

    companion object {
        const val DAY = 24L * 60 * 60 * 1000

        fun relatedToJson(list: List<Related>): String? =
            if (list.isEmpty()) null else JSONArray(list.map {
                JSONObject().put("t", it.title).put("l", it.link).put("s", it.source)
            }).toString()

        fun relatedFromJson(s: String?): List<Related> {
            if (s.isNullOrBlank()) return emptyList()
            return runCatching {
                val a = JSONArray(s)
                (0 until a.length()).map { i ->
                    val o = a.getJSONObject(i)
                    Related(o.optString("t"), o.optString("l"), o.optString("s"))
                }
            }.getOrDefault(emptyList())
        }
    }
}

private fun Cursor.str(col: String): String? {
    val i = getColumnIndex(col)
    return if (i < 0 || isNull(i)) null else getString(i)
}

private inline fun <T> Cursor.list(map: (Cursor) -> T): List<T> {
    val out = ArrayList<T>(count.coerceAtLeast(0))
    while (moveToNext()) out += map(this)
    return out
}

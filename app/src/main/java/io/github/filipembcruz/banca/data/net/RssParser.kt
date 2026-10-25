package io.github.filipembcruz.banca.data.net

import io.github.filipembcruz.banca.core.Text
import io.github.filipembcruz.banca.data.model.Article
import io.github.filipembcruz.banca.data.model.Related
import org.jsoup.Jsoup
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.net.URI
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * RSS 2.0 and Atom reader. Finds the item image in this order:
 * media:content, media:thumbnail, enclosure, then the first <img> in description/content.
 */
object RssParser {

    private val dateFormats = listOf(
        "EEE, dd MMM yyyy HH:mm:ss zzz",
        "EEE, dd MMM yyyy HH:mm:ss Z",
        "EEE, d MMM yyyy HH:mm:ss zzz",
        "EEE, d MMM yyyy HH:mm:ss Z",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
    )

    private class Raw {
        var title = ""
        var link = ""
        var date = ""
        var description = ""
        var content = ""
        var source = ""
        var sourceUrl: String? = null
        var image: String? = null
    }

    fun parse(xml: String, topic: String? = null): List<Article> {
        val parser = XmlPullParserFactory.newInstance().apply { isNamespaceAware = false }.newPullParser()
        val clean = xml.trimStart('\uFEFF', ' ', '\n', '\r', '\t').replace("&nbsp;", "&#160;")
        parser.setInput(StringReader(clean))

        val out = mutableListOf<Article>()
        var channelTitle = ""
        var cur: Raw? = null
        var event = runCatching { parser.eventType }.getOrDefault(XmlPullParser.END_DOCUMENT)
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                val tag = parser.name.lowercase(Locale.ROOT)
                val item = cur
                if (tag == "item" || tag == "entry") {
                    cur = Raw()
                } else if (item == null) {
                    if (tag == "title" && channelTitle.isEmpty()) channelTitle = safeText(parser)
                } else {
                    when (tag) {
                        "title" -> item.title = safeText(parser)
                        "link" -> {
                            val href = parser.getAttributeValue(null, "href")
                            if (href != null) {
                                val rel = parser.getAttributeValue(null, "rel")
                                if (rel == null || rel == "alternate") item.link = href
                            } else {
                                item.link = safeText(parser)
                            }
                        }
                        "pubdate", "published", "updated", "dc:date" ->
                            if (item.date.isEmpty()) item.date = safeText(parser)
                        "description", "summary" -> item.description = safeText(parser)
                        "content:encoded", "content" -> item.content = safeText(parser)
                        "source" -> {
                            item.sourceUrl = parser.getAttributeValue(null, "url")
                            item.source = safeText(parser)
                        }
                        "media:content", "media:thumbnail" -> {
                            val medium = parser.getAttributeValue(null, "medium")
                            val type = parser.getAttributeValue(null, "type")
                            val isImage = medium == null || medium == "image" || type?.startsWith("image") == true
                            if (item.image == null && isImage) item.image = parser.getAttributeValue(null, "url")
                        }
                        "enclosure" -> {
                            val type = parser.getAttributeValue(null, "type") ?: ""
                            if (item.image == null && type.startsWith("image")) item.image = parser.getAttributeValue(null, "url")
                        }
                    }
                }
            } else if (event == XmlPullParser.END_TAG) {
                val tag = parser.name.lowercase(Locale.ROOT)
                val item = cur
                if ((tag == "item" || tag == "entry") && item != null) {
                    build(item, channelTitle, topic)?.let { out += it }
                    cur = null
                }
            }
            event = try {
                parser.next()
            } catch (e: Exception) {
                XmlPullParser.END_DOCUMENT // malformed tail: keep what was read
            }
        }
        return out
    }

    private fun safeText(p: XmlPullParser): String = runCatching { p.nextText() }.getOrDefault("").trim()

    private fun build(r: Raw, channel: String, topic: String?): Article? {
        val link = r.link.trim()
        if (link.isEmpty() || r.title.isBlank()) return null
        val google = link.contains("news.google.com")
        val html = Jsoup.parseBodyFragment(r.description.ifBlank { r.content })

        val sourceName = r.source.ifBlank { cleanChannel(channel) }
        val title = Text.unescape(
            if (google && sourceName.isNotBlank()) r.title.removeSuffix(" - $sourceName") else r.title
        ).trim()

        val image = r.image
            ?: html.selectFirst("img[src]")?.attr("src")?.takeIf { it.startsWith("http") }
            ?: Jsoup.parseBodyFragment(r.content).selectFirst("img[src]")?.attr("src")?.takeIf { it.startsWith("http") }

        val related = if (google) html.select("li").mapNotNull { li ->
            val a = li.selectFirst("a") ?: return@mapNotNull null
            Related(Text.unescape(a.text()), a.attr("href"), li.selectFirst("font")?.text().orEmpty())
        }.filter { it.link != link } else emptyList()

        // Publisher feeds carry a real description; Google's is only the list of outlets.
        val summary = if (google) null else {
            val text = Text.squash(html.text())
            text.takeIf { it.length >= 40 && !Text.similar(it, title, 0.85) }
        }

        val domain = hostOf(r.sourceUrl) ?: if (!google) hostOf(link) else null

        return Article(
            id = Text.idFor(link),
            title = title,
            link = link,
            realLink = if (google) null else link,
            source = sourceName.ifBlank { domain.orEmpty() },
            domain = domain,
            published = parseDate(r.date),
            image = image,
            summary = summary,
            topic = topic,
            related = related,
        )
    }

    private fun cleanChannel(t: String): String =
        t.substringBefore(" > ").substringBefore(" - ").substringBefore(" | ").trim()

    fun hostOf(url: String?): String? =
        url?.let { runCatching { URI(it.trim()).host?.removePrefix("www.")?.removePrefix("m.") }.getOrNull() }

    fun parseDate(s: String): Long {
        val v = s.trim()
        if (v.isEmpty()) return 0L
        for (f in dateFormats) {
            val ms = runCatching { SimpleDateFormat(f, Locale.US).parse(v)?.time }.getOrNull()
            if (ms != null) return ms
        }
        return 0L
    }
}

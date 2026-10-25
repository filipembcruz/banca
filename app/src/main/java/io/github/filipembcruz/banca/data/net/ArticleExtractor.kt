package io.github.filipembcruz.banca.data.net

import io.github.filipembcruz.banca.core.Text
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

data class Block(val text: String, val heading: Boolean = false, val quote: Boolean = false)

data class Extracted(
    val url: String,
    val title: String,
    val image: String?,
    val site: String?,
    val author: String?,
    val blocks: List<Block>,
) {
    val words: Int get() = blocks.sumOf { it.text.split(' ').size }
    val minutes: Int get() = maxOf(1, (words + 199) / 220)
    val plainText: String get() = blocks.joinToString("\n\n") { it.text }
}

/** A small readability-style extractor: finds the main text block of a news page. */
object ArticleExtractor {

    private val junkSelectors = listOf(
        "script", "style", "noscript", "iframe", "svg", "form", "button", "nav", "header", "footer", "aside",
        "figure figcaption", "[role=navigation]", "[role=banner]", "[role=complementary]", "[aria-hidden=true]",
    )

    private val junkName = Regex(
        "(^|[\\s_-])(share|sharing|social|newsletter|related|recommend\\w*|promo|advert\\w*|ads?|banner|cookie\\w*|comments?|paywall|subscribe|tags|breadcrumbs?)([\\s_-]|$)",
        RegexOption.IGNORE_CASE,
    )

    private val junkLines = listOf(
        "leia também", "leia mais", "veja também", "saiba mais", "assine", "newsletter", "publicidade",
        "continua após", "clique aqui", "siga o", "receba as notícias", "read more", "subscribe", "advertisement",
        "sign up", "follow us", "related:",
    )

    fun parse(html: String, url: String): Extracted? {
        val doc = Jsoup.parse(html, url)
        val title = meta(doc, "og:title") ?: doc.selectFirst("h1")?.text() ?: doc.title()
        val image = doc.selectFirst("meta[property=og:image]")?.absUrl("content")?.takeIf { it.startsWith("http") }
        val site = meta(doc, "og:site_name")
        val author = doc.selectFirst("meta[name=author]")?.attr("content")?.takeIf { it.isNotBlank() }

        junkSelectors.forEach { sel -> runCatching { doc.select(sel).remove() } }
        doc.select("[class], [id]").filter { el ->
            el.tagName() !in setOf("html", "body", "article", "main") &&
                (junkName.containsMatchIn(el.className()) || junkName.containsMatchIn(el.id())) &&
                el.text().length < 1500
        }.forEach { it.remove() }

        val root = pickRoot(doc) ?: return null
        val blocks = root.select("p, h2, h3, blockquote, li").mapNotNull { el -> toBlock(el) }
            .distinctBy { it.text }
            .filterNot { b -> b.text.equals(title, ignoreCase = true) }
        if (blocks.count { !it.heading } < 2) return null
        return Extracted(url, Text.unescape(title).trim(), image, site, author, blocks)
    }

    private fun toBlock(el: Element): Block? {
        // Skip list items and paragraphs nested inside an element we already take.
        if (el.tagName() == "li" && el.parents().any { it.tagName() == "nav" }) return null
        if (el.tagName() == "p" && el.parents().any { it.tagName() == "blockquote" }) return null
        val text = Text.squash(el.text())
        if (text.isEmpty()) return null
        val lower = text.lowercase()
        if (junkLines.any { lower.startsWith(it) }) return null
        return when (el.tagName()) {
            "h2", "h3" -> if (text.length in 4..160) Block(text, heading = true) else null
            "blockquote" -> if (text.length >= 30) Block(text, quote = true) else null
            "li" -> if (text.length >= 60) Block("• $text") else null
            else -> if (text.length >= 40) Block(text) else null
        }
    }

    /** The element whose direct paragraphs hold the most text. */
    private fun pickRoot(doc: Document): Element? {
        doc.select("article").maxByOrNull { textOfParagraphs(it) }?.let { if (textOfParagraphs(it) > 600) return it }
        val scores = HashMap<Element, Int>()
        for (p in doc.select("p")) {
            val len = p.text().length
            if (len < 40) continue
            val parent = p.parent() ?: continue
            scores[parent] = (scores[parent] ?: 0) + len
            val grand = parent.parent()
            if (grand != null) scores[grand] = (scores[grand] ?: 0) + len / 2
        }
        return scores.maxByOrNull { it.value }?.key ?: doc.body()
    }

    private fun textOfParagraphs(e: Element): Int = e.select("p").sumOf { it.text().length }

    private fun meta(doc: Document, property: String): String? =
        doc.selectFirst("meta[property=$property]")?.attr("content")?.takeIf { it.isNotBlank() }
}

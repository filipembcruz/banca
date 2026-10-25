package io.github.filipembcruz.banca.data.net

import io.github.filipembcruz.banca.data.model.Edition
import io.github.filipembcruz.banca.data.model.Editions
import io.github.filipembcruz.banca.data.model.Place
import io.github.filipembcruz.banca.data.model.TopicDef
import io.github.filipembcruz.banca.data.model.Topics
import java.net.URLEncoder

/** A feed to download, with the topic label its items carry. */
data class FeedUrl(val url: String, val topic: String?)

object GoogleNews {

    private fun params(e: Edition) = "hl=${e.hl}&gl=${e.gl}&ceid=${e.ceid}"

    fun top(e: Edition) = "https://news.google.com/rss?${params(e)}"

    fun topic(e: Edition, section: String) =
        "https://news.google.com/rss/headlines/section/topic/$section?${params(e)}"

    fun search(e: Edition, query: String, window: String? = null): String {
        val q = if (window != null) "$query when:$window" else query
        return "https://news.google.com/rss/search?q=${URLEncoder.encode(q, "UTF-8")}&${params(e)}"
    }
}

/** Publisher feeds that add images to Google's text-only results (Brazilian edition). */
private object Publishers {
    val br: Map<String, List<String>> = mapOf(
        "nation" to listOf("https://g1.globo.com/rss/g1/", "https://agenciabrasil.ebc.com.br/rss/ultimasnoticias/feed.xml"),
        "world" to listOf("https://g1.globo.com/rss/g1/mundo/"),
        "politics" to listOf("https://g1.globo.com/rss/g1/politica/"),
        "business" to listOf("https://g1.globo.com/rss/g1/economia/"),
        "technology" to listOf("https://g1.globo.com/rss/g1/tecnologia/"),
        "entertainment" to listOf("https://g1.globo.com/rss/g1/pop-arte/"),
        "cars" to listOf("https://g1.globo.com/rss/g1/carros/"),
    )
}

object FeedCatalog {

    fun forTopic(e: Edition, t: TopicDef): List<FeedUrl> {
        val google = if (t.googleTopic != null) {
            GoogleNews.topic(e, t.googleTopic)
        } else {
            GoogleNews.search(e, if (e.isPt) t.queryPt else t.queryEn, "2d")
        }
        val extra = if (e.id == "BR") Publishers.br[t.id].orEmpty() else emptyList()
        return listOf(FeedUrl(google, t.id)) + extra.map { FeedUrl(it, t.id) }
    }

    fun forTopicId(e: Edition, id: String): List<FeedUrl> =
        if (id == Topics.SCITECH) {
            forTopic(e, Topics.byId("science")!!) + forTopic(e, Topics.byId("technology")!!)
        } else {
            Topics.byId(id)?.let { forTopic(e, it) } ?: emptyList()
        }

    fun forPlace(fallback: Edition, p: Place): FeedUrl {
        val e = Editions.forCountry(p.countryCode, fallback)
        return FeedUrl(GoogleNews.search(e, "\"${p.name}\"", "3d"), "place:${p.name}")
    }

    fun forTerm(e: Edition, term: String, window: String? = "2d"): FeedUrl =
        FeedUrl(GoogleNews.search(e, term, window), "term:$term")

    fun forSource(e: Edition, domain: String): FeedUrl =
        FeedUrl(GoogleNews.search(e, "site:$domain", "2d"), null)
}

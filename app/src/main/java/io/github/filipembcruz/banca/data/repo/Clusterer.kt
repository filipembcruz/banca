package io.github.filipembcruz.banca.data.repo

import io.github.filipembcruz.banca.core.Text
import io.github.filipembcruz.banca.data.model.Article
import io.github.filipembcruz.banca.data.model.Related

/**
 * Groups the same story coming from different feeds. The lead keeps a picture when one exists
 * and the others become its "other coverage".
 */
object Clusterer {

    fun cluster(items: List<Article>, threshold: Double = 0.42): List<Article> {
        val tokens = items.map { Text.tokens(it.title) }
        val used = BooleanArray(items.size)
        val out = ArrayList<Article>()
        for (i in items.indices) {
            if (used[i]) continue
            used[i] = true
            val group = mutableListOf(items[i])
            for (j in i + 1 until items.size) {
                if (used[j]) continue
                if (items[j].link == items[i].link || Text.jaccard(tokens[i], tokens[j]) >= threshold) {
                    used[j] = true
                    group += items[j]
                }
            }
            out += merge(group)
        }
        return out
    }

    private fun merge(group: List<Article>): Article {
        if (group.size == 1) return group[0]
        // Lead: Google's lead story first (it has the grouped coverage), then anything with an image.
        val lead = group.maxByOrNull { a ->
            (if (a.related.isNotEmpty()) 4 else 0) + (if (a.image != null) 2 else 0) + (if (a.summary != null) 1 else 0)
        } ?: group[0]
        val seenSources = mutableSetOf(lead.source.lowercase())
        val related = ArrayList<Related>()
        (lead.related + group.filter { it !== lead }.flatMap { listOf(Related(it.title, it.bestLink, it.source)) + it.related })
            .forEach { r ->
                val key = r.source.lowercase()
                if (r.link != lead.link && key !in seenSources) {
                    seenSources += key
                    related += r
                }
            }
        val image = lead.image ?: group.firstNotNullOfOrNull { it.image }
        val summary = lead.summary ?: group.firstNotNullOfOrNull { it.summary }
        val published = group.maxOf { it.published }
        return lead.copy(image = image, summary = summary, related = related, published = published)
    }
}

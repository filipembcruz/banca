package io.github.filipembcruz.banca.data.net

import io.github.filipembcruz.banca.core.Text

data class Summary(val points: List<String>, val fromPublisher: Boolean)

/**
 * Offline summaries, no AI and no network:
 * 1. the publisher's own description when it is real text;
 * 2. otherwise the key sentences of the article, skipping clickbait.
 */
object Summarizer {

    private val bait = listOf(
        "saiba mais", "saiba como", "veja ", "veja:", "confira", "leia também", "leia mais", "clique", "assista",
        "você não vai acreditar", "voce nao vai acreditar", "entenda", "descubra", "não perca", "inscreva-se", "assine",
        "click here", "read more", "you won't believe", "find out", "watch:", "subscribe", "sign up", "here's why",
        "this is why", "what you need to know",
    )

    private val sentenceEnd = Regex("(?<=[.!?…])[\"”’)]?\\s+(?=[\"“‘(]?[\\p{Lu}0-9])")

    fun publisher(description: String?, title: String): Summary? {
        val d = description?.let { Text.squash(it) } ?: return null
        if (d.length < 200) return null
        if (Text.similar(d, title, 0.7)) return null
        val sentences = split(d).filterNot { isBait(it) }
        if (sentences.isEmpty()) return null
        return Summary(listOf(Text.clip(sentences.joinToString(" "), 700)), fromPublisher = true)
    }

    fun keySentences(paragraphs: List<String>, title: String, count: Int): Summary? {
        val titleTokens = Text.tokens(title)
        val sentences = paragraphs.flatMapIndexed { pi, p -> split(p).map { pi to it } }
            .filter { (_, s) -> s.length in 50..420 && !isBait(s) }
        if (sentences.isEmpty()) return null

        val freq = HashMap<String, Int>()
        sentences.forEach { (_, s) -> Text.tokens(s).forEach { t -> freq[t] = (freq[t] ?: 0) + 1 } }
        val maxFreq = (freq.values.maxOrNull() ?: 1).toDouble()

        val scored = sentences.mapIndexed { i, (pi, s) ->
            val toks = Text.tokens(s)
            val position = 1.0 / (1.0 + pi / 3.0)
            val titleOverlap = if (titleTokens.isEmpty()) 0.0 else toks.count { it in titleTokens }.toDouble() / titleTokens.size
            val density = if (toks.isEmpty()) 0.0 else toks.sumOf { (freq[it] ?: 0) / maxFreq } / toks.size
            val quotePenalty = if (s.startsWith("\"") || s.startsWith("“")) 0.15 else 0.0
            Triple(i, s, position + 1.5 * titleOverlap + 0.8 * density - quotePenalty)
        }

        val picked = mutableListOf<Triple<Int, String, Double>>()
        for (c in scored.sortedByDescending { it.third }) {
            if (picked.size >= count) break
            if (picked.any { Text.similar(it.second, c.second, 0.6) }) continue
            picked += c
        }
        return Summary(picked.sortedBy { it.first }.map { it.second }, fromPublisher = false)
    }

    fun split(text: String): List<String> = text.split(sentenceEnd).map { it.trim() }.filter { it.isNotEmpty() }

    private fun isBait(s: String): Boolean {
        val t = s.trim()
        if (t.endsWith("?") || t.endsWith("!")) return true
        val low = t.lowercase()
        return bait.any { low.startsWith(it) || low.contains(" $it") }
    }
}

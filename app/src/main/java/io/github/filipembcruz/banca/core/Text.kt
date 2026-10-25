package io.github.filipembcruz.banca.core

import org.jsoup.parser.Parser
import java.security.MessageDigest
import java.text.Normalizer
import java.util.Locale

object Text {

    private val whitespace = Regex("\\s+")
    private val accents = Regex("\\p{InCombiningDiacriticalMarks}+")
    private val nonWord = Regex("[^\\p{L}\\p{N}]+")

    private val stopwords = setOf(
        // pt
        "a", "o", "as", "os", "um", "uma", "uns", "umas", "de", "da", "do", "das", "dos", "em", "na", "no", "nas", "nos",
        "por", "para", "pra", "com", "sem", "sob", "sobre", "e", "ou", "que", "se", "ao", "aos", "à", "às", "é", "foi",
        "ser", "são", "seu", "sua", "seus", "suas", "mais", "menos", "após", "apos", "entre", "como", "diz", "dizem",
        "afirma", "veja", "vai", "vão", "tem", "têm", "ja", "já", "nao", "não", "ele", "ela", "eles", "elas", "isso",
        // en
        "the", "an", "of", "to", "in", "on", "at", "for", "with", "and", "or", "is", "are", "was", "were", "be", "by",
        "from", "as", "it", "its", "this", "that", "after", "over", "says", "say", "new", "how", "why", "what", "who",
    )

    fun unescape(s: String): String = runCatching { Parser.unescapeEntities(s, false) }.getOrDefault(s)

    fun squash(s: String): String = s.replace(whitespace, " ").trim()

    fun fold(s: String): String =
        accents.replace(Normalizer.normalize(s.lowercase(Locale.ROOT), Normalizer.Form.NFD), "")

    fun tokens(s: String): Set<String> =
        fold(s).split(nonWord).filter { it.length >= 3 && it !in stopwords }.toSet()

    fun jaccard(a: Set<String>, b: Set<String>): Double {
        if (a.isEmpty() || b.isEmpty()) return 0.0
        val inter = a.count { it in b }
        return inter.toDouble() / (a.size + b.size - inter)
    }

    fun similar(a: String, b: String, threshold: Double): Boolean = jaccard(tokens(a), tokens(b)) >= threshold

    fun idFor(link: String): String {
        val md = MessageDigest.getInstance("SHA-1").digest(link.trim().toByteArray())
        return md.take(10).joinToString("") { "%02x".format(it) }
    }

    /** Cuts at a sentence end near [max] characters. */
    fun clip(s: String, max: Int): String {
        if (s.length <= max) return s
        val cut = s.substring(0, max)
        val end = maxOf(cut.lastIndexOf(". "), cut.lastIndexOf("! "), cut.lastIndexOf("? "))
        return if (end > max / 2) cut.substring(0, end + 1) else cut.substringBeforeLast(' ') + "…"
    }
}

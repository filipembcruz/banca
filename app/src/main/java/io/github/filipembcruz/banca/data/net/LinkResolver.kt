package io.github.filipembcruz.banca.data.net

import android.util.Base64
import okhttp3.FormBody
import org.json.JSONArray
import org.jsoup.Jsoup

/**
 * Turns Google News article links into publisher links, and finds a page's share image.
 * Google changes this format from time to time; every step fails soft and returns null.
 */
class LinkResolver(private val http: Http) {

    fun decodeGoogle(link: String): String? {
        val id = link.substringAfter("/articles/", "").substringBefore("?").takeIf { it.isNotBlank() } ?: return null
        legacyDecode(id)?.let { return it }
        return runCatching { batchDecode(id) }.getOrNull()
    }

    /** Old-style ids carry the URL inside the base64 payload. */
    private fun legacyDecode(id: String): String? = runCatching {
        val padded = id + "=".repeat((4 - id.length % 4) % 4)
        val bytes = Base64.decode(padded, Base64.URL_SAFE)
        var s = String(bytes, Charsets.ISO_8859_1)
        val prefix = String(charArrayOf(0x08.toChar(), 0x13.toChar(), 0x22.toChar()))
        if (s.startsWith(prefix)) s = s.substring(prefix.length)
        val suffix = String(charArrayOf(0xD2.toChar(), 0x01.toChar(), 0x00.toChar()))
        if (s.endsWith(suffix)) s = s.substring(0, s.length - suffix.length)
        if (s.isEmpty()) return@runCatching null
        val len = s[0].code
        s = if (len >= 0x80) s.substring(2, minOf(s.length, len + 1)) else s.substring(1, minOf(s.length, len + 1))
        s.takeIf { it.startsWith("http") && !it.startsWith("AU_yqL") }
    }.getOrNull()

    /** New-style ids need Google's batchexecute endpoint with a signature from the article page. */
    private fun batchDecode(id: String): String? {
        val page = http.getText("https://news.google.com/rss/articles/$id")
        val doc = Jsoup.parse(page)
        val node = doc.selectFirst("[data-n-a-sg][data-n-a-ts]") ?: return null
        val sg = node.attr("data-n-a-sg")
        val ts = node.attr("data-n-a-ts")
        val inner = "[\"garturlreq\",[[\"X\",\"X\",[\"X\",\"X\"],null,null,1,1,\"US:en\",null,1,null,null,null,null,null,0,1]," +
            "\"X\",\"X\",1,[1,1,1],1,1,null,0,0,null,0],\"$id\",$ts,\"$sg\"]"
        val req = JSONArray().put(JSONArray().put(JSONArray().put("Fbv4je").put(inner)))
        val body = FormBody.Builder().add("f.req", req.toString()).build()
        val request = http.request("https://news.google.com/_/DotsSplashUi/data/batchexecute")
            .header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8")
            .post(body)
            .build()
        val text = http.execute(request).use { it.body?.string() } ?: return null
        val chunk = text.split("\n\n").getOrNull(1) ?: return null
        val outer = JSONArray(chunk)
        for (i in 0 until outer.length()) {
            val row = outer.optJSONArray(i) ?: continue
            if (row.optString(1) == "Fbv4je") {
                val payload = JSONArray(row.optString(2))
                return payload.optString(1).takeIf { it.startsWith("http") }
            }
        }
        return null
    }

    /** og:image or twitter:image of a page. */
    fun shareImage(url: String): String? = runCatching {
        http.execute(http.request(url).build()).use { r ->
            if (!r.isSuccessful) return@use null
            val finalUrl = r.request.url.toString()
            if (finalUrl.contains("news.google.com")) return@use null
            val stream = r.body?.byteStream() ?: return@use null
            val doc = Jsoup.parse(stream, null, finalUrl)
            (doc.selectFirst("meta[property=og:image]")?.absUrl("content")
                ?: doc.selectFirst("meta[name=twitter:image]")?.absUrl("content"))
                ?.takeIf { it.startsWith("http") }
        }
    }.getOrNull()
}

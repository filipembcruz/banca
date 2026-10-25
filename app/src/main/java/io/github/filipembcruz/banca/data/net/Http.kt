package io.github.filipembcruz.banca.data.net

import android.content.Context
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class Http(context: Context) {

    val client: OkHttpClient = OkHttpClient.Builder()
        .cache(Cache(File(context.cacheDir, "http"), 30L * 1024 * 1024))
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    fun request(url: String): Request.Builder = Request.Builder()
        .url(url)
        .header("User-Agent", USER_AGENT)
        .header("Accept-Language", "pt-BR,pt;q=0.9,en;q=0.8")

    fun execute(request: Request): Response = client.newCall(request).execute()

    /** GET as text; throws on HTTP errors. */
    fun getText(url: String): String {
        execute(request(url).build()).use { r ->
            if (!r.isSuccessful) throw IOException("HTTP ${r.code} for $url")
            return r.body?.string() ?: throw IOException("Empty body for $url")
        }
    }

    fun clearCache() {
        runCatching { client.cache?.evictAll() }
    }

    companion object {
        const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14; SM-S928B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36"
    }
}

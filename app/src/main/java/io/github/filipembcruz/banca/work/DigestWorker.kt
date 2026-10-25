package io.github.filipembcruz.banca.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.github.filipembcruz.banca.core.container
import io.github.filipembcruz.banca.data.model.Article

/** Posts the grouped digest at the chosen times, then schedules the next one. */
class DigestWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val c = applicationContext.container
        val s = c.settings.current()
        try {
            if (!s.notifications || !s.digest) return Result.success()

            // Top stories of the moment, plus what the rules queued since the last digest.
            val forYou = s.tabs.firstOrNull()
            if (forYou != null) runCatching { c.news.refresh(forYou, force = true) }
            val hidden = c.store.hiddenIds()
            val top = forYou?.let { c.store.tabItems(it.id) }.orEmpty()
                .filter { it.id !in hidden }
                .sortedByDescending { it.related.size * 3 + (if (it.image != null) 1 else 0) }
                .take(4)
            val queued = c.store.pendingDigest().mapNotNull { c.store.findArticle(it) }
            val items: List<Article> = (queued + top).distinctBy { it.id }.take(6)

            var weatherLine: String? = null
            if (s.digestWeather && s.places.isNotEmpty()) {
                runCatching { c.data.loadWeather(s.places.take(1)) }
                c.data.weather.value[s.places.first().key]?.let { w ->
                    weatherLine = Notifier.weatherLine(applicationContext, w, s.fahrenheit)
                }
            }
            if (items.isNotEmpty()) {
                items.forEach { c.store.putTransient("digest", it) }
                c.store.addDigest(items.map { it.id })
                Notifier.digest(applicationContext, items, weatherLine)
                items.forEach { c.store.markNotified(it.id, "digest", pending = false) }
            }
            c.store.clearPending()
        } finally {
            WorkScheduler.scheduleDigest(applicationContext, c.settings.current())
        }
        return Result.success()
    }
}

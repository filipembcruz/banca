package io.github.filipembcruz.banca.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.github.filipembcruz.banca.core.container
import io.github.filipembcruz.banca.data.model.Article
import io.github.filipembcruz.banca.data.model.Delivery
import io.github.filipembcruz.banca.data.model.Edition
import io.github.filipembcruz.banca.data.model.Editions
import io.github.filipembcruz.banca.data.model.NotificationRule
import io.github.filipembcruz.banca.data.model.Place
import io.github.filipembcruz.banca.data.model.RuleKind
import io.github.filipembcruz.banca.data.net.FeedCatalog
import io.github.filipembcruz.banca.data.net.FeedUrl
import io.github.filipembcruz.banca.data.net.GoogleNews
import io.github.filipembcruz.banca.data.repo.Clusterer
import java.util.Calendar

/**
 * Periodic background work: refreshes the cached feeds and, when notifications are on,
 * matches new stories against the user's rules.
 */
class RefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val c = applicationContext.container
        val s = c.settings.current()
        if (!s.onboarded) return Result.success()

        // Keep the first tabs warm so the app opens with fresh stories.
        s.tabs.take(2).forEach { runCatching { c.news.refresh(it, force = true) } }
        runCatching { c.data.loadWeather(s.places) }
        runCatching { c.data.loadQuotes(s.markets) }
        c.store.pruneNotified()

        if (!s.notifications) return Result.success()

        val e = Editions.byId(s.edition)
        val now = System.currentTimeMillis()
        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
        }.timeInMillis
        var sentToday = c.store.notifiedSince(startOfDay)
        val quiet = s.quietHours && WorkScheduler.inQuietHours(s.quietStart, s.quietEnd)
        val hidden = c.store.hiddenIds()
        val blocked = c.store.sourcePrefs().values.filter { it.blocked }

        fun usable(a: Article) = a.id !in hidden && !c.store.wasNotified(a.id) &&
            (a.published == 0L || now - a.published < 8 * 3_600_000L) &&
            blocked.none { b -> b.domain == a.domain || b.name.equals(a.source, true) }

        val rules = buildList {
            addAll(s.rules.filter { it.delivery != Delivery.OFF })
            if (s.bigStoryAlerts) add(NotificationRule("big", RuleKind.TOPIC, "top", "", Delivery.INSTANT, bigOnly = true))
        }

        for (rule in rules) {
            val url = urlFor(rule, e, s.places) ?: continue
            val items = c.news.fetchAll(listOf(url), perFeed = 25) ?: continue
            val candidates = Clusterer.cluster(items)
                .filter { usable(it) }
                .filter { !rule.bigOnly || it.related.size >= 3 }
                .sortedByDescending { it.related.size * 2 + if (it.image != null) 1 else 0 }
                .take(3)
            for (a in candidates) {
                c.store.putTransient("notif", a)
                if (rule.delivery == Delivery.DIGEST) {
                    c.store.markNotified(a.id, "digest-queue", pending = true)
                    continue
                }
                val urgent = rule.bigOnly && a.related.size >= 5
                if (quiet && !(s.quietAllowUrgent && urgent)) {
                    c.store.markNotified(a.id, "digest-queue", pending = true)
                    continue
                }
                if (sentToday >= s.dailyLimit) break
                Notifier.article(applicationContext, a, rule)
                c.store.markNotified(a.id, rule.id, pending = false)
                sentToday++
            }
        }
        return Result.success()
    }

    private fun urlFor(rule: NotificationRule, e: Edition, places: List<Place>): FeedUrl? =
        when (rule.kind) {
            RuleKind.TOPIC -> if (rule.value == "top") FeedUrl(GoogleNews.top(e), "top") else FeedCatalog.forTopicId(e, rule.value).firstOrNull()
            RuleKind.TERM -> FeedCatalog.forTerm(e, rule.value, "1d")
            RuleKind.SOURCE -> FeedCatalog.forSource(e, rule.value)
            RuleKind.PLACE -> places.firstOrNull { it.name == rule.value }?.let { FeedCatalog.forPlace(e, it) }
                ?: FeedCatalog.forTerm(e, "\"${rule.value}\"", "1d")
        }
}

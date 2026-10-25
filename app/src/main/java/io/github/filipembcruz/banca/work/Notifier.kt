package io.github.filipembcruz.banca.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import io.github.filipembcruz.banca.R
import io.github.filipembcruz.banca.core.Format
import io.github.filipembcruz.banca.core.LocaleHelper
import io.github.filipembcruz.banca.data.model.Article
import io.github.filipembcruz.banca.data.model.NotifStyle
import io.github.filipembcruz.banca.data.model.NotificationRule
import io.github.filipembcruz.banca.data.model.Weather
import io.github.filipembcruz.banca.ui.MainActivity
import io.github.filipembcruz.banca.ui.components.WeatherText

object Notifier {

    const val CH_ALERTS = "alerts"
    const val CH_DIGEST = "digest"
    const val CH_FOLLOWING = "following"
    const val EXTRA_ARTICLE = "article_id"
    private const val DIGEST_ID = 4100

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val ctx = LocaleHelper.wrap(context)
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CH_ALERTS, ctx.getString(R.string.channel_alerts), NotificationManager.IMPORTANCE_HIGH)
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_DIGEST, ctx.getString(R.string.channel_digest), NotificationManager.IMPORTANCE_DEFAULT)
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_FOLLOWING, ctx.getString(R.string.channel_following), NotificationManager.IMPORTANCE_DEFAULT)
        )
    }

    /** Each rule gets its own channel so sound and vibration can be tuned in system settings. */
    private fun ruleChannel(context: Context, rule: NotificationRule): String {
        if (rule.id == "big") return CH_ALERTS
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return CH_FOLLOWING
        val id = "rule_${rule.id}"
        val nm = context.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(id) == null) {
            nm.createNotificationChannel(
                NotificationChannel(id, rule.label.ifBlank { rule.value }, NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        return id
    }

    fun deleteRuleChannel(context: Context, ruleId: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        runCatching { context.getSystemService(NotificationManager::class.java).deleteNotificationChannel("rule_$ruleId") }
    }

    fun canPost(context: Context): Boolean {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        return Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    private fun openIntent(context: Context, articleId: String?, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (articleId != null) putExtra(EXTRA_ARTICLE, articleId)
        }
        return PendingIntent.getActivity(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun actionIntent(context: Context, action: String, articleId: String, ruleId: String, notifId: Int): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            this.action = action
            putExtra(EXTRA_ARTICLE, articleId)
            putExtra(NotificationActionReceiver.EXTRA_RULE, ruleId)
            putExtra(NotificationActionReceiver.EXTRA_NOTIF, notifId)
        }
        return PendingIntent.getBroadcast(context, (action + articleId).hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    suspend fun article(context: Context, a: Article, rule: NotificationRule) {
        if (!canPost(context)) return
        val ctx = LocaleHelper.wrap(context)
        val id = a.id.hashCode()
        val channel = ruleChannel(context, rule)
        val title = if (rule.id == "big") ctx.getString(R.string.notif_big_story) else rule.label.ifBlank { rule.value }
        val b = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_banca)
            .setColor(0xFF3B5BFD.toInt())
            .setContentTitle(a.title)
            .setContentText(listOfNotNull(a.source, title.takeIf { it.isNotBlank() }).joinToString(" · "))
            .setSubText(title)
            .setWhen(if (a.published > 0) a.published else System.currentTimeMillis())
            .setShowWhen(true)
            .setAutoCancel(true)
            .setGroup("rule_${rule.id}")
            .setContentIntent(openIntent(context, a.id, id))
            .addAction(0, ctx.getString(R.string.action_save), actionIntent(context, NotificationActionReceiver.ACTION_SAVE, a.id, rule.id, id))
            .addAction(0, ctx.getString(R.string.action_show_less), actionIntent(context, NotificationActionReceiver.ACTION_LESS, a.id, rule.id, id))
            .addAction(0, ctx.getString(R.string.action_mute_rule), actionIntent(context, NotificationActionReceiver.ACTION_MUTE, a.id, rule.id, id))

        when (rule.style) {
            NotifStyle.LARGE -> {
                val bmp = a.image?.let { loadBitmap(context, it) }
                if (bmp != null) {
                    b.setLargeIcon(bmp)
                    b.setStyle(NotificationCompat.BigPictureStyle().bigPicture(bmp).bigLargeIcon(null as Bitmap?).setSummaryText(a.source))
                } else {
                    b.setStyle(NotificationCompat.BigTextStyle().bigText(a.summary ?: a.title))
                }
            }
            NotifStyle.COMPACT -> {
                a.image?.let { loadBitmap(context, it) }?.let { b.setLargeIcon(it) }
                b.setStyle(NotificationCompat.BigTextStyle().bigText(a.summary ?: a.title))
            }
            NotifStyle.HEADLINE -> Unit
        }

        val nm = NotificationManagerCompat.from(context)
        try {
            nm.notify(id, b.build())
            // Group summary so several stories of a rule fold together in the shade.
            val summary = NotificationCompat.Builder(context, channel)
                .setSmallIcon(R.drawable.ic_stat_banca)
                .setColor(0xFF3B5BFD.toInt())
                .setContentTitle(title)
                .setGroup("rule_${rule.id}")
                .setGroupSummary(true)
                .setAutoCancel(true)
                .setContentIntent(openIntent(context, null, rule.id.hashCode()))
                .build()
            nm.notify(rule.id.hashCode(), summary)
        } catch (e: SecurityException) {
            // Permission revoked between the check and the post.
        }
    }

    fun digest(context: Context, items: List<Article>, weatherLine: String?) {
        if (!canPost(context)) return
        val ctx = LocaleHelper.wrap(context)
        val style = NotificationCompat.InboxStyle()
        weatherLine?.let { style.addLine(it) }
        items.forEach { style.addLine("${it.source}: ${it.title}") }
        style.setSummaryText(ctx.getString(R.string.notif_digest_summary, items.size))
        val n = NotificationCompat.Builder(context, CH_DIGEST)
            .setSmallIcon(R.drawable.ic_stat_banca)
            .setColor(0xFF3B5BFD.toInt())
            .setContentTitle(ctx.getString(R.string.notif_digest_title))
            .setContentText(items.firstOrNull()?.title ?: "")
            .setStyle(style)
            .setAutoCancel(true)
            .setContentIntent(openIntent(context, null, DIGEST_ID))
            .build()
        try {
            NotificationManagerCompat.from(context).notify(DIGEST_ID, n)
        } catch (e: SecurityException) {
            // ignored
        }
    }

    fun weatherLine(context: Context, w: Weather, fahrenheit: Boolean): String {
        val ctx = LocaleHelper.wrap(context)
        val today = w.days.firstOrNull()
        val range = if (today != null) " · ${Format.temp(today.max, fahrenheit)}/${Format.temp(today.min, fahrenheit)}" else ""
        return "${w.place.name}: ${Format.temp(w.temp, fahrenheit)}, ${ctx.getString(WeatherText.label(w.code))}$range"
    }

    fun cancel(context: Context, notifId: Int) {
        NotificationManagerCompat.from(context).cancel(notifId)
    }

    private suspend fun loadBitmap(context: Context, url: String): Bitmap? = runCatching {
        val req = ImageRequest.Builder(context).data(url).allowHardware(false).size(1024, 576).build()
        val result = context.imageLoader.execute(req)
        (result as? SuccessResult)?.drawable?.toBitmap()
    }.getOrNull()
}

package io.github.filipembcruz.banca.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.filipembcruz.banca.core.container
import io.github.filipembcruz.banca.data.model.Delivery
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(Notifier.EXTRA_ARTICLE) ?: return
        val ruleId = intent.getStringExtra(EXTRA_RULE) ?: ""
        val notifId = intent.getIntExtra(EXTRA_NOTIF, 0)
        val c = context.container
        val pending = goAsync()
        c.scope.launch {
            try {
                when (intent.action) {
                    ACTION_SAVE -> c.news.article(id)?.let { c.news.save(it) }
                    ACTION_LESS -> c.news.article(id)?.let { a ->
                        a.topic?.let { c.news.showLessTopic(it) }
                        a.domain?.let { c.news.showLessSource(it, a.source) }
                    }
                    ACTION_MUTE -> c.settings.update { s ->
                        s.copy(rules = s.rules.map { if (it.id == ruleId) it.copy(delivery = Delivery.OFF) else it })
                    }
                }
                Notifier.cancel(context, notifId)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_SAVE = "io.github.filipembcruz.banca.SAVE"
        const val ACTION_LESS = "io.github.filipembcruz.banca.LESS"
        const val ACTION_MUTE = "io.github.filipembcruz.banca.MUTE"
        const val EXTRA_RULE = "rule_id"
        const val EXTRA_NOTIF = "notif_id"
    }
}

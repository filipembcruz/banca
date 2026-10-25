package io.github.filipembcruz.banca.ui.settings

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.filipembcruz.banca.R
import io.github.filipembcruz.banca.core.Format
import io.github.filipembcruz.banca.data.model.Delivery
import io.github.filipembcruz.banca.data.model.NotifStyle
import io.github.filipembcruz.banca.data.model.NotificationRule
import io.github.filipembcruz.banca.data.model.RuleKind
import io.github.filipembcruz.banca.data.model.Topics
import io.github.filipembcruz.banca.ui.AppViewModel
import io.github.filipembcruz.banca.ui.components.Labels
import io.github.filipembcruz.banca.ui.nav.LocalSettings
import io.github.filipembcruz.banca.work.Notifier
import io.github.filipembcruz.banca.work.WorkScheduler

@Composable
fun rememberNotificationPermission(onResult: (Boolean) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { onResult(it) }
    return {
        if (Build.VERSION.SDK_INT >= 33 && !Notifier.canPost(context)) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            onResult(true)
        }
    }
}

@Composable
fun NotificationsPage(app: AppViewModel, onBack: () -> Unit, navigate: (String) -> Unit) {
    val s = LocalSettings.current
    val context = LocalContext.current
    var addRule by remember { mutableStateOf(false) }
    var editTime by remember { mutableStateOf<Pair<String, Int>?>(null) }
    val askPermission = rememberNotificationPermission { granted ->
        app.updateSettings { it.copy(notifications = granted) }
        if (!granted) app.notify(R.string.msg_permission_denied)
    }

    SettingsPage(stringResource(R.string.settings_notifications), onBack) {
        item {
            Group {
                SwitchItem(stringResource(R.string.notif_enable), s.notifications, stringResource(R.string.notif_enable_sub),
                    Icons.Rounded.NotificationsActive) { on ->
                    if (on) askPermission() else app.updateSettings { it.copy(notifications = false) }
                }
            }
        }
        item {
            Group(stringResource(R.string.notif_following), footer = stringResource(R.string.notif_following_sub)) {
                SwitchItem(stringResource(R.string.notif_big_stories), s.bigStoryAlerts, stringResource(R.string.notif_big_stories_sub),
                    enabled = s.notifications) { v -> app.updateSettings { it.copy(bigStoryAlerts = v) } }
                s.rules.forEach { r ->
                    Divider()
                    Item(
                        r.label.ifBlank { r.value },
                        "${stringResource(kindLabel(r.kind))} · ${stringResource(deliveryLabel(r.delivery))}",
                        ruleIcon(r),
                        onClick = { navigate("settings/rule/${r.id}") },
                    )
                }
                Divider()
                Item(stringResource(R.string.notif_add_rule), icon = Icons.Rounded.Add, onClick = { addRule = true }, chevron = false)
            }
        }
        item {
            Group(stringResource(R.string.notif_digest), footer = stringResource(R.string.notif_digest_sub)) {
                SwitchItem(stringResource(R.string.notif_digest_enable), s.digest, enabled = s.notifications) { v ->
                    app.updateSettings { it.copy(digest = v) }
                }
                Divider()
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.notif_digest_times), style = MaterialTheme.typography.bodyLarge)
                    FlowRow(
                        Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        s.digestTimes.sorted().forEach { t ->
                            InputChip(
                                selected = true,
                                onClick = { app.updateSettings { it.copy(digestTimes = it.digestTimes - t) } },
                                label = { Text(Format.clock(t)) },
                                trailingIcon = { Icon(Icons.Rounded.Close, stringResource(R.string.action_remove), Modifier.size(16.dp)) },
                                enabled = s.digest && s.notifications,
                            )
                        }
                        FilterChip(
                            selected = false,
                            onClick = { editTime = "digest" to 12 * 60 },
                            label = { Text(stringResource(R.string.action_add_time)) },
                            leadingIcon = { Icon(Icons.Rounded.Add, null, Modifier.size(16.dp)) },
                            enabled = s.digest && s.notifications && s.digestTimes.size < 6,
                        )
                    }
                }
                Divider()
                SwitchItem(stringResource(R.string.notif_digest_weather), s.digestWeather, enabled = s.digest && s.notifications) { v ->
                    app.updateSettings { it.copy(digestWeather = v) }
                }
            }
        }
        item {
            Group(stringResource(R.string.notif_limits)) {
                SwitchItem(stringResource(R.string.notif_quiet), s.quietHours,
                    stringResource(R.string.notif_quiet_sub, Format.clock(s.quietStart), Format.clock(s.quietEnd)),
                    Icons.Rounded.Schedule) { v -> app.updateSettings { it.copy(quietHours = v) } }
                if (s.quietHours) {
                    Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = false, onClick = { editTime = "quietStart" to s.quietStart },
                            label = { Text(stringResource(R.string.notif_quiet_from, Format.clock(s.quietStart))) })
                        FilterChip(selected = false, onClick = { editTime = "quietEnd" to s.quietEnd },
                            label = { Text(stringResource(R.string.notif_quiet_to, Format.clock(s.quietEnd))) })
                    }
                    SwitchItem(stringResource(R.string.notif_quiet_urgent), s.quietAllowUrgent) { v ->
                        app.updateSettings { it.copy(quietAllowUrgent = v) }
                    }
                }
                Divider()
                SliderItem(
                    stringResource(R.string.notif_daily_limit),
                    s.dailyLimit.toFloat(), 1f..30f, 28,
                    label = { it.toInt().toString() },
                ) { v -> app.updateSettings { it.copy(dailyLimit = v.toInt()) } }
            }
        }
        item {
            Group(stringResource(R.string.notif_refresh), footer = stringResource(R.string.notif_refresh_sub)) {
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.notif_refresh_every), style = MaterialTheme.typography.bodyLarge)
                    FlowRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(15, 30, 60, 180, 0).forEach { m ->
                            FilterChip(
                                selected = s.refreshMinutes == m,
                                onClick = { app.updateSettings { it.copy(refreshMinutes = m) } },
                                label = { Text(refreshLabel(m)) },
                            )
                        }
                    }
                }
                Divider()
                SwitchItem(stringResource(R.string.notif_wifi_only), s.wifiOnly) { v -> app.updateSettings { it.copy(wifiOnly = v) } }
                Divider()
                SwitchItem(stringResource(R.string.notif_charging_only), s.chargingOnly) { v -> app.updateSettings { it.copy(chargingOnly = v) } }
                Divider()
                SwitchItem(stringResource(R.string.notif_battery), s.pauseOnSaver) { v -> app.updateSettings { it.copy(pauseOnSaver = v) } }
                Divider()
                Item(stringResource(R.string.notif_refresh_now), onClick = {
                    WorkScheduler.refreshNow(context); app.notify(R.string.msg_refreshing)
                }, chevron = false)
                Divider()
                Item(stringResource(R.string.notif_system_settings), onClick = {
                    val i = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    runCatching { context.startActivity(i) }
                })
            }
        }
    }

    if (addRule) AddRuleSheet(app, onDismiss = { addRule = false }, onAdded = { id -> addRule = false; navigate("settings/rule/$id") })

    editTime?.let { (which, initial) ->
        TimeDialog(initial, onDismiss = { editTime = null }) { minutes ->
            app.updateSettings {
                when (which) {
                    "quietStart" -> it.copy(quietStart = minutes)
                    "quietEnd" -> it.copy(quietEnd = minutes)
                    else -> it.copy(digestTimes = (it.digestTimes + minutes).distinct().sorted())
                }
            }
            editTime = null
        }
    }
}

@Composable
private fun refreshLabel(m: Int): String = when (m) {
    0 -> stringResource(R.string.refresh_manual)
    in 1..59 -> stringResource(R.string.refresh_minutes, m)
    else -> stringResource(R.string.refresh_hours, m / 60)
}

@Composable
fun TimeDialog(initial: Int, onDismiss: () -> Unit, onPick: (Int) -> Unit) {
    val state = rememberTimePickerState(initialHour = initial / 60, initialMinute = initial % 60, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onPick(state.hour * 60 + state.minute) }) { Text(stringResource(R.string.ok)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
        text = { TimePicker(state = state) },
    )
}

fun kindLabel(k: RuleKind) = when (k) {
    RuleKind.TOPIC -> R.string.rule_kind_topic
    RuleKind.TERM -> R.string.rule_kind_term
    RuleKind.SOURCE -> R.string.rule_kind_source
    RuleKind.PLACE -> R.string.rule_kind_place
}

fun deliveryLabel(d: Delivery) = when (d) {
    Delivery.INSTANT -> R.string.delivery_instant
    Delivery.DIGEST -> R.string.delivery_digest
    Delivery.OFF -> R.string.delivery_off
}

private fun ruleIcon(r: NotificationRule) = when (r.kind) {
    RuleKind.TOPIC -> Labels.topicIcon(r.value)
    RuleKind.TERM -> Icons.Rounded.Search
    RuleKind.SOURCE -> Icons.Rounded.Public
    RuleKind.PLACE -> Icons.Rounded.Place
}

@Composable
private fun AddRuleSheet(app: AppViewModel, onDismiss: () -> Unit, onAdded: (String) -> Unit) {
    val s = LocalSettings.current
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var text by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf(RuleKind.TOPIC) }

    fun add(kind: RuleKind, value: String, label: String) {
        val id = (kind.name + value).lowercase().hashCode().toUInt().toString(16)
        app.updateSettings { st ->
            if (st.rules.any { it.id == id }) st else st.copy(rules = st.rules + NotificationRule(id, kind, value, label))
        }
        onAdded(id)
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.navigationBarsPadding().padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
            Text(stringResource(R.string.notif_add_rule), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(12.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RuleKind.entries.forEach { k ->
                    FilterChip(selected = kind == k, onClick = { kind = k; text = "" }, label = { Text(stringResource(kindLabel(k))) })
                }
            }
            Spacer(Modifier.height(12.dp))
            when (kind) {
                RuleKind.TOPIC -> FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Topics.all.forEach { t ->
                        val label = stringResource(Labels.topicRes(t.id)!!)
                        FilterChip(
                            selected = false,
                            onClick = { add(RuleKind.TOPIC, t.id, label) },
                            label = { Text(label) },
                            leadingIcon = { Icon(Labels.topicIcon(t.id), null, Modifier.size(18.dp)) },
                        )
                    }
                }
                RuleKind.PLACE -> FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (s.places.isEmpty()) Text(stringResource(R.string.rule_no_places))
                    s.places.forEach { p ->
                        FilterChip(selected = false, onClick = { add(RuleKind.PLACE, p.name, p.name) }, label = { Text(p.name) },
                            leadingIcon = { Icon(Icons.Rounded.Place, null, Modifier.size(18.dp)) })
                    }
                }
                else -> Column {
                    OutlinedTextField(
                        value = text, onValueChange = { text = it },
                        label = { Text(stringResource(if (kind == RuleKind.SOURCE) R.string.rule_source_hint else R.string.rule_term_hint)) },
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick = {
                            val v = text.trim().let { if (kind == RuleKind.SOURCE) it.removePrefix("https://").removePrefix("http://").removePrefix("www.").substringBefore('/') else it }
                            add(kind, v, v)
                        },
                        enabled = text.isNotBlank(),
                        modifier = Modifier.padding(top = 12.dp).fillMaxWidth(),
                    ) { Text(stringResource(R.string.action_add)) }
                }
            }
        }
    }
}

@Composable
fun RulePage(ruleId: String, app: AppViewModel, onBack: () -> Unit) {
    val s = LocalSettings.current
    val context = LocalContext.current
    val rule = s.rules.firstOrNull { it.id == ruleId }
    SettingsPage(rule?.let { r -> r.label.ifBlank { r.value } } ?: stringResource(R.string.notif_rule), onBack) {
        if (rule == null) {
            item { Text(stringResource(R.string.rule_deleted), modifier = Modifier.padding(16.dp)) }
            return@SettingsPage
        }
        fun update(transform: (NotificationRule) -> NotificationRule) =
            app.updateSettings { st -> st.copy(rules = st.rules.map { if (it.id == ruleId) transform(it) else it }) }

        item {
            Group(stringResource(R.string.rule_delivery)) {
                ChoiceItem(
                    stringResource(R.string.rule_delivery),
                    Delivery.entries.map { it to stringResource(deliveryLabel(it)) },
                    rule.delivery,
                ) { d -> update { it.copy(delivery = d) } }
                Divider()
                SwitchItem(stringResource(R.string.rule_big_only), rule.bigOnly, stringResource(R.string.rule_big_only_sub)) { v ->
                    update { it.copy(bigOnly = v) }
                }
            }
        }
        item {
            Group(stringResource(R.string.rule_style), footer = stringResource(R.string.rule_style_sub)) {
                ChoiceItem(
                    stringResource(R.string.rule_style),
                    listOf(
                        NotifStyle.LARGE to stringResource(R.string.style_large),
                        NotifStyle.COMPACT to stringResource(R.string.style_compact),
                        NotifStyle.HEADLINE to stringResource(R.string.style_headline),
                    ),
                    rule.style,
                ) { st -> update { it.copy(style = st) } }
                Divider()
                Item(stringResource(R.string.rule_sound), stringResource(R.string.rule_sound_sub), onClick = {
                    val i = if (Build.VERSION.SDK_INT >= 26) {
                        Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            .putExtra(Settings.EXTRA_CHANNEL_ID, "rule_${rule.id}")
                    } else {
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    }
                    runCatching { context.startActivity(i) }
                })
            }
        }
        item {
            Group {
                Item(stringResource(R.string.rule_delete), onClick = {
                    Notifier.deleteRuleChannel(context, rule.id)
                    app.updateSettings { st -> st.copy(rules = st.rules.filter { it.id != ruleId }) }
                    onBack()
                }, chevron = false)
            }
        }
    }
}

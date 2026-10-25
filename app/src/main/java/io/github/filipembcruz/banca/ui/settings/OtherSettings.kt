package io.github.filipembcruz.banca.ui.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.filipembcruz.banca.BuildConfig
import io.github.filipembcruz.banca.R
import io.github.filipembcruz.banca.core.LocaleHelper
import io.github.filipembcruz.banca.data.settings.ThemeMode
import io.github.filipembcruz.banca.ui.AppViewModel
import io.github.filipembcruz.banca.ui.nav.LocalSettings
import io.github.filipembcruz.banca.ui.theme.readerStyle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReadingPage(app: AppViewModel, onBack: () -> Unit) {
    val s = LocalSettings.current
    val context = LocalContext.current
    SettingsPage(stringResource(R.string.settings_reading), onBack) {
        item {
            Group(stringResource(R.string.reading_preview)) {
                Text(
                    stringResource(R.string.text_size_sample),
                    style = readerStyle(s.serifReader, s.textScale, MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
        item {
            Group(stringResource(R.string.reading_reader)) {
                SwitchItem(stringResource(R.string.reading_reader_default), s.readerDefault, stringResource(R.string.reading_reader_default_sub)) { v ->
                    app.updateSettings { it.copy(readerDefault = v) }
                }
                Divider()
                ChoiceItem(
                    stringResource(R.string.reading_font),
                    listOf(true to stringResource(R.string.font_serif), false to stringResource(R.string.font_sans)),
                    s.serifReader,
                ) { v -> app.updateSettings { it.copy(serifReader = v) } }
                Divider()
                SliderItem(stringResource(R.string.action_text_size), s.textScale, 0.85f..1.4f, 10, label = { "${(it * 100).toInt()}%" }) { v ->
                    app.updateSettings { it.copy(textScale = v) }
                }
            }
        }
        item {
            Group(stringResource(R.string.reading_summaries), footer = stringResource(R.string.reading_summaries_sub)) {
                SwitchItem(stringResource(R.string.reading_feed_summaries), s.feedSummaries, stringResource(R.string.reading_feed_summaries_sub)) { v ->
                    app.updateSettings { it.copy(feedSummaries = v) }
                }
                Divider()
                SliderItem(stringResource(R.string.reading_summary_length), s.summaryLength.toFloat(), 3f..5f, 1,
                    label = { context.getString(R.string.reading_summary_points, it.toInt()) }) { v ->
                    app.updateSettings { it.copy(summaryLength = v.toInt()) }
                }
            }
        }
    }
}

@Composable
fun AppearancePage(app: AppViewModel, onBack: () -> Unit) {
    val s = LocalSettings.current
    SettingsPage(stringResource(R.string.settings_appearance), onBack) {
        item {
            Group(stringResource(R.string.appearance_theme)) {
                ChoiceItem(
                    stringResource(R.string.appearance_theme),
                    listOf(
                        ThemeMode.SYSTEM to stringResource(R.string.theme_system),
                        ThemeMode.LIGHT to stringResource(R.string.theme_light),
                        ThemeMode.DARK to stringResource(R.string.theme_dark),
                    ),
                    s.theme,
                ) { v -> app.updateSettings { it.copy(theme = v) } }
                Divider()
                SwitchItem(stringResource(R.string.appearance_amoled), s.amoled, stringResource(R.string.appearance_amoled_sub), Icons.Rounded.DarkMode) { v ->
                    app.updateSettings { it.copy(amoled = v) }
                }
                Divider()
                SwitchItem(
                    stringResource(R.string.appearance_dynamic), s.dynamicColor, stringResource(R.string.appearance_dynamic_sub),
                    Icons.Rounded.Wallpaper, enabled = android.os.Build.VERSION.SDK_INT >= 31,
                ) { v -> app.updateSettings { it.copy(dynamicColor = v) } }
            }
        }
        item {
            Group(stringResource(R.string.appearance_motion), footer = stringResource(R.string.appearance_motion_sub)) {
                SwitchItem(stringResource(R.string.appearance_reduce_motion), s.reducedMotion, icon = Icons.Rounded.Animation) { v ->
                    app.updateSettings { it.copy(reducedMotion = v) }
                }
            }
        }
    }
}

@Composable
fun DataPage(vm: SettingsViewModel, app: AppViewModel, onBack: () -> Unit) {
    val s = LocalSettings.current
    var cacheMb by remember { mutableLongStateOf(vm.cacheSizeMb()) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) vm.export(uri) { ok -> app.notify(if (ok) R.string.msg_exported else R.string.msg_export_failed) }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importFrom(uri) { ok -> app.notify(if (ok) R.string.msg_imported else R.string.msg_import_failed) }
    }
    SettingsPage(stringResource(R.string.settings_data), onBack) {
        item {
            Group(stringResource(R.string.data_usage)) {
                SwitchItem(stringResource(R.string.data_images_wifi), s.imagesWifiOnly, stringResource(R.string.data_images_wifi_sub), Icons.Rounded.Image) { v ->
                    app.updateSettings { it.copy(imagesWifiOnly = v) }
                }
                Divider()
                Item(
                    stringResource(R.string.data_clear_cache),
                    stringResource(R.string.data_cache_size, cacheMb),
                    Icons.Rounded.DeleteSweep,
                    onClick = { vm.clearCache { cacheMb = vm.cacheSizeMb(); app.notify(R.string.msg_cache_cleared) } },
                    chevron = false,
                )
            }
        }
        item {
            Group(stringResource(R.string.data_backup), footer = stringResource(R.string.data_backup_sub)) {
                Item(stringResource(R.string.data_export), icon = Icons.Rounded.Upload, onClick = {
                    val stamp = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
                    exportLauncher.launch("banca-settings-$stamp.json")
                })
                Divider()
                Item(stringResource(R.string.data_import), icon = Icons.Rounded.Download, onClick = {
                    importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                })
            }
        }
    }
}

@Composable
fun LanguagePage(onBack: () -> Unit) {
    val context = LocalContext.current
    val current = LocaleHelper.current(context)
    SettingsPage(stringResource(R.string.settings_language), onBack) {
        item {
            Group(footer = stringResource(R.string.language_sub)) {
                listOf("en" to "English", "pt" to "Português (Brasil)").forEachIndexed { i, (code, label) ->
                    if (i > 0) Divider()
                    Item(label, onClick = { changeLanguage(context, code) }, chevron = false) {
                        RadioButton(selected = current == code, onClick = { changeLanguage(context, code) })
                    }
                }
            }
        }
    }
}

fun changeLanguage(context: android.content.Context, code: String) {
    if (LocaleHelper.current(context) == code && LocaleHelper.saved(context) != null) return
    LocaleHelper.set(context, code)
    (context as? Activity)?.recreate() ?: (context as? android.content.ContextWrapper)?.baseContext?.let { base ->
        (base as? Activity)?.recreate()
    }
}

@Composable
fun AboutPage(onBack: () -> Unit) {
    val context = LocalContext.current
    fun open(url: String) = runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    SettingsPage(stringResource(R.string.settings_about), onBack) {
        item {
            Group {
                Column(Modifier.padding(20.dp)) {
                    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall)
                    Text(stringResource(R.string.app_tagline), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        stringResource(R.string.about_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
        item {
            Group {
                Item(stringResource(R.string.about_source), "github.com/filipembcruz/banca", Icons.Rounded.Code,
                    onClick = { open("https://github.com/filipembcruz/banca") })
                Divider()
                Item(stringResource(R.string.about_privacy), stringResource(R.string.about_privacy_sub), Icons.Rounded.PrivacyTip)
                Divider()
                Item(stringResource(R.string.about_data_sources), stringResource(R.string.about_data_sources_sub), Icons.Rounded.Public)
            }
        }
        item {
            Group(stringResource(R.string.about_licenses)) {
                Text(
                    "Jetpack Compose, AndroidX, Material Components — Apache 2.0\n" +
                        "OkHttp — Apache 2.0\nCoil — Apache 2.0\njsoup — MIT\nKotlin Coroutines — Apache 2.0\n" +
                        "Weather: Open-Meteo (CC BY 4.0)\nQuotes: Yahoo Finance, AwesomeAPI\nNews: Google News RSS and publishers' feeds",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}

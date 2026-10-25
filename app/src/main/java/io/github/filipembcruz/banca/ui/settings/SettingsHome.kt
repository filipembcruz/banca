package io.github.filipembcruz.banca.ui.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.filipembcruz.banca.R
import io.github.filipembcruz.banca.core.LocaleHelper
import io.github.filipembcruz.banca.data.settings.ThemeMode
import io.github.filipembcruz.banca.ui.nav.LocalSettings

@Composable
fun SettingsHome(navigate: (String) -> Unit, bottomPadding: androidx.compose.ui.unit.Dp) {
    val s = LocalSettings.current
    val context = LocalContext.current
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = bottomPadding + 24.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            Text(
                stringResource(R.string.nav_settings),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.statusBarsPadding().padding(start = 4.dp, top = 12.dp, bottom = 8.dp),
            )
        }
        item {
            Group(stringResource(R.string.group_feed)) {
                Item(stringResource(R.string.settings_tabs), pluralStringResource(R.plurals.tabs_count, s.tabs.size, s.tabs.size),
                    Icons.Rounded.ViewAgenda, onClick = { navigate("settings/tabs") })
                Divider()
                Item(stringResource(R.string.settings_for_you), stringResource(R.string.settings_for_you_sub),
                    Icons.Rounded.Dashboard, onClick = { navigate("settings/foryou") })
                Divider()
                Item(stringResource(R.string.settings_topics_sources), stringResource(R.string.settings_topics_sources_sub),
                    Icons.Rounded.Block, onClick = { navigate("settings/topics") })
            }
        }
        item {
            Group(stringResource(R.string.group_cards)) {
                Item(stringResource(R.string.settings_places), s.places.joinToString { it.name }.ifBlank { stringResource(R.string.none) },
                    Icons.Rounded.Place, onClick = { navigate("settings/places") })
                Divider()
                Item(stringResource(R.string.settings_markets), pluralStringResource(R.plurals.items_count, s.markets.size, s.markets.size),
                    Icons.Rounded.ShowChart, onClick = { navigate("settings/markets") })
            }
        }
        item {
            Group(stringResource(R.string.group_experience)) {
                Item(stringResource(R.string.settings_reading), stringResource(R.string.settings_reading_sub),
                    Icons.Rounded.MenuBook, onClick = { navigate("settings/reading") })
                Divider()
                Item(stringResource(R.string.settings_notifications),
                    stringResource(if (s.notifications) R.string.on else R.string.off),
                    Icons.Rounded.Notifications, onClick = { navigate("settings/notifications") })
                Divider()
                Item(stringResource(R.string.settings_appearance), stringResource(when (s.theme) {
                    ThemeMode.SYSTEM -> R.string.theme_system
                    ThemeMode.LIGHT -> R.string.theme_light
                    ThemeMode.DARK -> R.string.theme_dark
                }), Icons.Rounded.Palette, onClick = { navigate("settings/appearance") })
                Divider()
                Item(stringResource(R.string.settings_language),
                    if (LocaleHelper.current(context) == "pt") "Português (Brasil)" else "English",
                    Icons.Rounded.Language, onClick = { navigate("settings/language") })
            }
        }
        item {
            Group(stringResource(R.string.group_app)) {
                Item(stringResource(R.string.settings_data), stringResource(R.string.settings_data_sub),
                    Icons.Rounded.Storage, onClick = { navigate("settings/data") })
                Divider()
                Item(stringResource(R.string.settings_about), stringResource(R.string.settings_about_sub),
                    Icons.Rounded.Info, onClick = { navigate("settings/about") })
            }
        }
    }
}

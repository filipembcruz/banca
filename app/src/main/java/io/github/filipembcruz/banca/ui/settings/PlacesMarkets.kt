package io.github.filipembcruz.banca.ui.settings

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.filipembcruz.banca.R
import io.github.filipembcruz.banca.core.LocaleHelper
import io.github.filipembcruz.banca.data.model.Markets
import io.github.filipembcruz.banca.data.model.Place
import io.github.filipembcruz.banca.ui.AppViewModel
import io.github.filipembcruz.banca.ui.nav.LocalSettings
import io.github.filipembcruz.banca.ui.theme.LocalExtras
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

private fun <T> List<T>.swap(from: Int, to: Int): List<T> {
    if (to !in indices || from !in indices) return this
    val m = toMutableList()
    val x = m.removeAt(from)
    m.add(to, x)
    return m
}

/** Search box + results; used here and in onboarding. */
@Composable
fun PlacePicker(vm: SettingsViewModel, onPick: (Place) -> Unit) {
    val context = LocalContext.current
    val results by vm.placeResults.collectAsStateWithLifecycle()
    val searching by vm.searching.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var locating by remember { mutableStateOf(false) }
    val lang = LocaleHelper.current(context)

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            locating = true
            scope.launch {
                currentPlace(context)?.let(onPick)
                locating = false
            }
        }
    }

    Column {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it; vm.searchPlaces(it, lang) },
            placeholder = { Text(stringResource(R.string.places_search_hint)) },
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            trailingIcon = { if (searching) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        results.forEach { p ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable {
                        onPick(p)
                        query = ""
                        vm.clearPlaceResults()
                    }
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Place, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(p.name, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        listOf(p.admin, p.country).filter { it.isNotBlank() }.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(Icons.Rounded.Add, null, tint = MaterialTheme.colorScheme.primary)
            }
        }
        OutlinedButton(
            onClick = {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    locating = true
                    scope.launch {
                        currentPlace(context)?.let(onPick)
                        locating = false
                    }
                } else {
                    permission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                }
            },
            modifier = Modifier.padding(top = 8.dp),
        ) {
            if (locating) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            else Icon(Icons.Rounded.MyLocation, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.places_use_location))
        }
    }
}

/** Last known coarse location, named with the platform geocoder. */
@SuppressLint("MissingPermission")
suspend fun currentPlace(context: Context): Place? = withContext(Dispatchers.IO) {
    runCatching {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val loc = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER, LocationManager.GPS_PROVIDER)
            .mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time } ?: return@runCatching null
        @Suppress("DEPRECATION")
        val addr = runCatching { Geocoder(context, Locale.getDefault()).getFromLocation(loc.latitude, loc.longitude, 1)?.firstOrNull() }.getOrNull()
        Place(
            name = addr?.locality ?: addr?.subAdminArea ?: addr?.adminArea ?: context.getString(R.string.places_my_location),
            admin = addr?.adminArea.orEmpty(),
            country = addr?.countryName.orEmpty(),
            countryCode = addr?.countryCode.orEmpty(),
            lat = loc.latitude,
            lon = loc.longitude,
        )
    }.getOrNull()
}

@Composable
fun PlacesPage(vm: SettingsViewModel, app: AppViewModel, onBack: () -> Unit) {
    val s = LocalSettings.current
    SettingsPage(stringResource(R.string.settings_places), onBack) {
        item {
            Text(
                stringResource(R.string.places_help),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
        items(s.places, key = { it.key }) { p ->
            val i = s.places.indexOf(p)
            Surface(shape = RoundedCornerShape(18.dp), color = LocalExtras.current.card, modifier = Modifier.padding(vertical = 4.dp).animateItem()) {
                Row(Modifier.padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Place, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(p.name, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            listOf(p.admin, p.country).filter { it.isNotBlank() }.joinToString(", "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(onClick = { app.updateSettings { it.copy(places = it.places.swap(i, i - 1)) } }, enabled = i > 0) {
                        Icon(Icons.Rounded.KeyboardArrowUp, stringResource(R.string.action_move_up))
                    }
                    IconButton(onClick = { app.updateSettings { st -> st.copy(places = st.places.filter { it.key != p.key }) } }) {
                        Icon(Icons.Rounded.DeleteOutline, stringResource(R.string.action_remove))
                    }
                }
            }
        }
        item {
            Group(stringResource(R.string.places_add)) {
                Column(Modifier.padding(16.dp)) {
                    PlacePicker(vm) { p ->
                        app.updateSettings { st -> if (st.places.any { it.key == p.key }) st else st.copy(places = st.places + p) }
                    }
                }
            }
        }
        item {
            Group(stringResource(R.string.units)) {
                ChoiceItem(stringResource(R.string.units_temperature), listOf(false to "°C", true to "°F"), s.fahrenheit) { v ->
                    app.updateSettings { it.copy(fahrenheit = v) }
                }
                Divider()
                ChoiceItem(stringResource(R.string.units_wind), listOf(false to "km/h", true to "mph"), s.mph) { v ->
                    app.updateSettings { it.copy(mph = v) }
                }
            }
        }
    }
}

@Composable
fun MarketsPage(app: AppViewModel, onBack: () -> Unit) {
    val s = LocalSettings.current
    SettingsPage(stringResource(R.string.settings_markets), onBack) {
        item {
            Text(
                stringResource(R.string.markets_help),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
        items(s.markets, key = { it }) { id ->
            val m = Markets.byId(id) ?: return@items
            val i = s.markets.indexOf(id)
            Surface(shape = RoundedCornerShape(18.dp), color = LocalExtras.current.card, modifier = Modifier.padding(vertical = 4.dp).animateItem()) {
                Row(Modifier.padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(m.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    IconButton(onClick = { app.updateSettings { it.copy(markets = it.markets.swap(i, i - 1)) } }, enabled = i > 0) {
                        Icon(Icons.Rounded.KeyboardArrowUp, stringResource(R.string.action_move_up))
                    }
                    IconButton(onClick = { app.updateSettings { it.copy(markets = it.markets.swap(i, i + 1)) } }, enabled = i < s.markets.lastIndex) {
                        Icon(Icons.Rounded.KeyboardArrowDown, stringResource(R.string.action_move_down))
                    }
                    IconButton(onClick = { app.updateSettings { st -> st.copy(markets = st.markets - id) } }) {
                        Icon(Icons.Rounded.DeleteOutline, stringResource(R.string.action_remove))
                    }
                }
            }
        }
        item {
            Group(stringResource(R.string.markets_available)) {
                val available = Markets.all.filter { it.id !in s.markets }
                if (available.isEmpty()) Item(stringResource(R.string.none))
                available.forEachIndexed { i, m ->
                    if (i > 0) Divider()
                    Item(m.label, stringResource(if (m.isCurrency) R.string.market_currency else R.string.market_index),
                        onClick = { app.updateSettings { it.copy(markets = it.markets + m.id) } }, chevron = false) {
                        Icon(Icons.Rounded.Add, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
        item {
            Group(stringResource(R.string.markets_display)) {
                Item(stringResource(R.string.markets_change_percent), chevron = false) {
                    Switch(checked = s.showChangeAsPercent, onCheckedChange = { v -> app.updateSettings { it.copy(showChangeAsPercent = v) } })
                }
            }
        }
    }
}

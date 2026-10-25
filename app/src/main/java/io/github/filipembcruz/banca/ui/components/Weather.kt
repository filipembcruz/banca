package io.github.filipembcruz.banca.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Dehaze
import androidx.compose.material.icons.rounded.Grain
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Thunderstorm
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbCloudy
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.filipembcruz.banca.R

/** WMO weather codes used by Open-Meteo. */
object WeatherText {

    fun label(code: Int): Int = when (code) {
        0 -> R.string.wx_clear
        1, 2 -> R.string.wx_partly_cloudy
        3 -> R.string.wx_cloudy
        45, 48 -> R.string.wx_fog
        51, 53, 55, 56, 57 -> R.string.wx_drizzle
        61, 63, 65, 66, 67 -> R.string.wx_rain
        71, 73, 75, 77, 85, 86 -> R.string.wx_snow
        80, 81, 82 -> R.string.wx_showers
        95, 96, 99 -> R.string.wx_storm
        else -> R.string.wx_cloudy
    }

    fun icon(code: Int, day: Boolean = true): ImageVector = when (code) {
        0 -> if (day) Icons.Rounded.WbSunny else Icons.Rounded.NightsStay
        1, 2 -> if (day) Icons.Rounded.WbCloudy else Icons.Rounded.NightsStay
        3 -> Icons.Rounded.Cloud
        45, 48 -> Icons.Rounded.Dehaze
        51, 53, 55, 56, 57 -> Icons.Rounded.Grain
        61, 63, 65, 66, 67, 80, 81, 82 -> Icons.Rounded.WaterDrop
        71, 73, 75, 77, 85, 86 -> Icons.Rounded.AcUnit
        95, 96, 99 -> Icons.Rounded.Thunderstorm
        else -> Icons.Rounded.Cloud
    }

    /** Two-stop gradient for the weather card. */
    fun gradient(code: Int, day: Boolean, dark: Boolean): List<Color> {
        val base = when {
            !day -> listOf(Color(0xFF1B2350), Color(0xFF3A2D6B))
            code == 0 || code in 1..2 -> listOf(Color(0xFF3B6FFD), Color(0xFF59B7FF))
            code in 95..99 -> listOf(Color(0xFF3B3F58), Color(0xFF6A5A9E))
            code in 51..86 -> listOf(Color(0xFF4A6382), Color(0xFF6E8CB0))
            else -> listOf(Color(0xFF5B74A8), Color(0xFF8AA4D6))
        }
        return if (dark) base.map { it.copy(alpha = 0.92f) } else base
    }
}

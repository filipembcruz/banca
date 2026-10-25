package io.github.filipembcruz.banca.core

import android.content.Context
import android.net.ConnectivityManager
import android.text.format.DateUtils
import io.github.filipembcruz.banca.R
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

object Format {

    fun ago(context: Context, time: Long, now: Long = System.currentTimeMillis()): String {
        if (time <= 0L) return ""
        val diff = now - time
        return when {
            diff < 60_000L -> context.getString(R.string.time_now)
            diff < 3_600_000L -> context.getString(R.string.time_minutes, (diff / 60_000L).toInt())
            diff < 86_400_000L -> context.getString(R.string.time_hours, (diff / 3_600_000L).toInt())
            diff < 7 * 86_400_000L -> context.getString(R.string.time_days, (diff / 86_400_000L).toInt())
            else -> DateUtils.formatDateTime(context, time, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH)
        }
    }

    fun temp(c: Double, fahrenheit: Boolean): String {
        if (c.isNaN()) return "–"
        val v = if (fahrenheit) c * 9 / 5 + 32 else c
        return "${v.roundToInt()}°"
    }

    fun wind(kmh: Double, mph: Boolean): String =
        if (mph) "${(kmh / 1.609).roundToInt()} mph" else "${kmh.roundToInt()} km/h"

    fun number(v: Double, decimals: Int, locale: Locale = Locale.getDefault()): String {
        val f = NumberFormat.getNumberInstance(locale)
        f.minimumFractionDigits = decimals
        f.maximumFractionDigits = decimals
        return f.format(v)
    }

    fun change(pct: Double, abs: Double, asPercent: Boolean, decimals: Int): String {
        val sign = if ((if (asPercent) pct else abs) >= 0) "+" else "−"
        return if (asPercent) "$sign${number(kotlin.math.abs(pct), 2)}%" else "$sign${number(kotlin.math.abs(abs), decimals)}"
    }

    fun dayName(isoDate: String, locale: Locale = Locale.getDefault()): String =
        runCatching { LocalDate.parse(isoDate).dayOfWeek.getDisplayName(TextStyle.SHORT, locale).replace(".", "") }
            .getOrDefault("")
            .replaceFirstChar { it.titlecase(locale) }

    fun clock(minutes: Int): String = "%02d:%02d".format(minutes / 60, minutes % 60)

    fun isMetered(context: Context): Boolean =
        runCatching {
            (context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).isActiveNetworkMetered
        }.getOrDefault(false)

    @Suppress("unused")
    private fun near(a: Double, b: Double) = abs(a - b) < 1e-9
}

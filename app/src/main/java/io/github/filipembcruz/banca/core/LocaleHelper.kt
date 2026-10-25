package io.github.filipembcruz.banca.core

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * App language without AppCompat: the choice lives in plain SharedPreferences
 * (read synchronously at startup) and every Activity/Application context is wrapped with it.
 */
object LocaleHelper {

    private const val PREFS = "banca_locale"
    private const val KEY = "lang"

    fun saved(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)

    /** "en" or "pt". Before a choice is made, Portuguese devices start in Portuguese. */
    fun current(context: Context): String =
        saved(context) ?: if (Locale.getDefault().language == "pt") "pt" else "en"

    fun set(context: Context, lang: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, lang).apply()
    }

    fun localeFor(lang: String): Locale = if (lang == "pt") Locale("pt", "BR") else Locale("en", "US")

    fun wrap(base: Context): Context {
        val locale = localeFor(current(base))
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        return base.createConfigurationContext(config)
    }
}

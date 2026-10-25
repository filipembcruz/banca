package io.github.filipembcruz.banca.data.model

/** Another outlet covering the same story. */
data class Related(
    val title: String,
    val link: String,
    val source: String,
)

/** A news item as shown in the app, already normalized from any feed. */
data class Article(
    val id: String,
    val title: String,
    val link: String,
    val realLink: String? = null,
    val source: String,
    val domain: String? = null,
    val published: Long = 0L,
    val image: String? = null,
    val summary: String? = null,
    val topic: String? = null,
    val related: List<Related> = emptyList(),
) {
    val favicon: String?
        get() = domain?.let { "https://www.google.com/s2/favicons?domain=$it&sz=128" }

    val isGoogleLink: Boolean get() = link.contains("news.google.com")

    /** Link to open: the publisher's page when known. */
    val bestLink: String get() = realLink ?: link
}

/** Article plus per-user state, ready for the feed UI. */
data class FeedItem(
    val article: Article,
    val read: Boolean = false,
    val saved: Boolean = false,
)

enum class TabType { FOR_YOU, TRENDING, LOCAL, TOPIC, SEARCH, FEED }

/**
 * A tab on Home. Built-in tabs take their title from string resources at display time,
 * so they follow the app language; SEARCH and FEED tabs keep the title the user typed.
 */
data class TabDef(
    val id: String,
    val type: TabType,
    val value: String = "",
    val title: String = "",
)

data class Place(
    val name: String,
    val admin: String = "",
    val country: String = "",
    val countryCode: String = "",
    val lat: Double,
    val lon: Double,
) {
    val key: String get() = "%.3f,%.3f".format(java.util.Locale.US, lat, lon)
    val label: String get() = listOf(name, admin.takeIf { it != name }, country).filter { !it.isNullOrBlank() }.joinToString(", ")
}

enum class RuleKind { TOPIC, TERM, SOURCE, PLACE }
enum class Delivery { INSTANT, DIGEST, OFF }
enum class NotifStyle { LARGE, COMPACT, HEADLINE }

data class NotificationRule(
    val id: String,
    val kind: RuleKind,
    val value: String,
    val label: String,
    val delivery: Delivery = Delivery.DIGEST,
    val bigOnly: Boolean = false,
    val style: NotifStyle = NotifStyle.LARGE,
)

enum class ForYouBlock { WEATHER, MARKETS, TOP, MARKET_NEWS, FEED }

data class BlockPref(val block: ForYouBlock, val visible: Boolean = true)

data class WeatherDay(val date: String, val code: Int, val max: Double, val min: Double)

data class Weather(
    val place: Place,
    val temp: Double,
    val feelsLike: Double,
    val code: Int,
    val isDay: Boolean,
    val wind: Double,
    val humidity: Int,
    val days: List<WeatherDay>,
    val updated: Long,
)

data class Quote(
    val symbol: String,
    val price: Double,
    val change: Double,
    val changePct: Double,
    val history: List<Double>,
    val updated: Long,
)

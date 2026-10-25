package io.github.filipembcruz.banca.data.model

/**
 * Topics offered in onboarding and Settings.
 * [googleTopic] uses a Google News section; otherwise [queryEn]/[queryPt] run a search.
 */
data class TopicDef(
    val id: String,
    val googleTopic: String? = null,
    val queryEn: String = "",
    val queryPt: String = "",
)

object Topics {
    val all = listOf(
        TopicDef("world", googleTopic = "WORLD"),
        TopicDef("nation", googleTopic = "NATION"),
        TopicDef("politics", queryEn = "politics", queryPt = "política"),
        TopicDef("business", googleTopic = "BUSINESS"),
        TopicDef("technology", googleTopic = "TECHNOLOGY"),
        TopicDef("science", googleTopic = "SCIENCE"),
        TopicDef("sports", googleTopic = "SPORTS"),
        TopicDef("entertainment", googleTopic = "ENTERTAINMENT"),
        TopicDef("health", googleTopic = "HEALTH"),
        TopicDef("games", queryEn = "video games", queryPt = "games videogame"),
        TopicDef("cars", queryEn = "cars automotive", queryPt = "carros automóveis"),
        TopicDef("travel", queryEn = "travel tourism", queryPt = "turismo viagem"),
        TopicDef("food", queryEn = "food restaurants", queryPt = "gastronomia"),
        TopicDef("music", queryEn = "music", queryPt = "música"),
    )

    /** Pseudo-topic used by the default "Science & Tech" tab. */
    const val SCITECH = "scitech"

    fun byId(id: String): TopicDef? = all.firstOrNull { it.id == id }

    val defaultFollowed = setOf("nation", "world", "business", "technology", "science", "sports")
}

/** A quote shown in the market strip. */
data class MarketDef(
    val id: String,
    val label: String,
    val yahoo: String,
    val awesome: String? = null,
    val isCurrency: Boolean = false,
    val newsEn: String = "",
    val newsPt: String = "",
    val decimals: Int = 2,
)

object Markets {
    val all = listOf(
        MarketDef("USDBRL", "USD/BRL", "BRL=X", awesome = "USD-BRL", isCurrency = true, decimals = 4),
        MarketDef("EURBRL", "EUR/BRL", "EURBRL=X", awesome = "EUR-BRL", isCurrency = true, decimals = 4),
        MarketDef("GBPBRL", "GBP/BRL", "GBPBRL=X", awesome = "GBP-BRL", isCurrency = true, decimals = 4),
        MarketDef("EURUSD", "EUR/USD", "EURUSD=X", awesome = "EUR-USD", isCurrency = true, decimals = 4),
        MarketDef("BTC", "Bitcoin", "BTC-USD", awesome = "BTC-USD", decimals = 0),
        MarketDef("IBOV", "Ibovespa", "^BVSP", newsEn = "Ibovespa", newsPt = "Ibovespa", decimals = 0),
        MarketDef("SPX", "S&P 500", "^GSPC", newsEn = "Wall Street stocks", newsPt = "Wall Street bolsa Nova York"),
        MarketDef("NDX", "Nasdaq", "^IXIC", newsEn = "Nasdaq", newsPt = "Nasdaq"),
        MarketDef("DJI", "Dow Jones", "^DJI", newsEn = "Dow Jones", newsPt = "Dow Jones", decimals = 0),
        MarketDef("STOXX", "Euro Stoxx 50", "^STOXX50E", newsEn = "European stocks", newsPt = "bolsas europeias"),
        MarketDef("PSI", "PSI", "PSI20.LS", newsEn = "Lisbon stock exchange PSI", newsPt = "bolsa de Lisboa PSI"),
        MarketDef("FTSE", "FTSE 100", "^FTSE", newsEn = "FTSE 100", newsPt = "bolsa de Londres"),
        MarketDef("DAX", "DAX", "^GDAXI", newsEn = "DAX Frankfurt", newsPt = "bolsa de Frankfurt DAX"),
        MarketDef("N225", "Nikkei 225", "^N225", newsEn = "Nikkei", newsPt = "Nikkei bolsa de Tóquio", decimals = 0),
    )

    val defaults = listOf("USDBRL", "EURBRL", "IBOV", "SPX", "STOXX")

    fun byId(id: String): MarketDef? = all.firstOrNull { it.id == id }
}

/** Google News edition: language + country. */
data class Edition(val id: String, val hl: String, val gl: String, val ceid: String, val isPt: Boolean)

object Editions {
    val all = listOf(
        Edition("BR", "pt-BR", "BR", "BR:pt-419", true),
        Edition("PT", "pt-PT", "PT", "PT:pt-150", true),
        Edition("US", "en-US", "US", "US:en", false),
    )

    fun byId(id: String): Edition = all.firstOrNull { it.id == id } ?: all.first()

    fun forCountry(code: String, fallback: Edition): Edition =
        all.firstOrNull { it.gl.equals(code, ignoreCase = true) } ?: fallback
}

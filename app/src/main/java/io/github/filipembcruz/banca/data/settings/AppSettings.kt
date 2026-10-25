package io.github.filipembcruz.banca.data.settings

import io.github.filipembcruz.banca.data.model.BlockPref
import io.github.filipembcruz.banca.data.model.Delivery
import io.github.filipembcruz.banca.data.model.ForYouBlock
import io.github.filipembcruz.banca.data.model.Markets
import io.github.filipembcruz.banca.data.model.NotifStyle
import io.github.filipembcruz.banca.data.model.NotificationRule
import io.github.filipembcruz.banca.data.model.Place
import io.github.filipembcruz.banca.data.model.RuleKind
import io.github.filipembcruz.banca.data.model.TabDef
import io.github.filipembcruz.banca.data.model.TabType
import io.github.filipembcruz.banca.data.model.Topics
import org.json.JSONArray
import org.json.JSONObject

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val onboarded: Boolean = false,
    val edition: String = "BR",

    // Appearance
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val amoled: Boolean = false,
    val dynamicColor: Boolean = false,
    val reducedMotion: Boolean = false,

    // Feed
    val tabs: List<TabDef> = defaultTabs(),
    val blocks: List<BlockPref> = ForYouBlock.entries.map { BlockPref(it, true) },
    val topics: Set<String> = Topics.defaultFollowed,
    val terms: List<String> = emptyList(),
    val recentSearches: List<String> = emptyList(),
    /** Hide stories older than this many hours (0 = no limit). */
    val maxAgeHours: Int = 48,
    /** Last chosen place on the Local tab. */
    val localPlace: String = "",

    // Places and weather
    val places: List<Place> = listOf(DEFAULT_PLACE),
    val fahrenheit: Boolean = false,
    val mph: Boolean = false,

    // Markets
    val markets: List<String> = Markets.defaults,
    val showChangeAsPercent: Boolean = true,

    // Reading
    val readerDefault: Boolean = true,
    val textScale: Float = 1f,
    val serifReader: Boolean = true,
    val feedSummaries: Boolean = true,
    val summaryLength: Int = 4,

    // Notifications
    val notifications: Boolean = false,
    val rules: List<NotificationRule> = emptyList(),
    val digest: Boolean = true,
    val digestTimes: List<Int> = listOf(8 * 60, 18 * 60),
    val digestWeather: Boolean = true,
    val bigStoryAlerts: Boolean = false,
    val quietHours: Boolean = true,
    val quietStart: Int = 22 * 60,
    val quietEnd: Int = 7 * 60,
    val quietAllowUrgent: Boolean = false,
    val dailyLimit: Int = 10,
    val refreshMinutes: Int = 60,
    val wifiOnly: Boolean = false,
    val chargingOnly: Boolean = false,
    val pauseOnSaver: Boolean = true,

    // Data
    val imagesWifiOnly: Boolean = false,
) {
    fun isBlockVisible(b: ForYouBlock) = blocks.firstOrNull { it.block == b }?.visible ?: true

    companion object {
        val DEFAULT_PLACE = Place("Belo Horizonte", "Minas Gerais", "Brasil", "BR", -19.9167, -43.9345)

        fun defaultTabs() = listOf(
            TabDef("foryou", TabType.FOR_YOU),
            TabDef("trending", TabType.TRENDING),
            TabDef("local", TabType.LOCAL),
            TabDef("scitech", TabType.TOPIC, Topics.SCITECH),
        )
    }
}

/** Hand-written JSON (org.json) so no serialization plugin is needed. */
object SettingsJson {

    fun encode(s: AppSettings): String = JSONObject().apply {
        put("v", 1)
        put("onboarded", s.onboarded)
        put("edition", s.edition)
        put("theme", s.theme.name)
        put("amoled", s.amoled)
        put("dynamicColor", s.dynamicColor)
        put("reducedMotion", s.reducedMotion)
        put("tabs", JSONArray(s.tabs.map { t ->
            JSONObject().put("id", t.id).put("type", t.type.name).put("value", t.value).put("title", t.title)
        }))
        put("blocks", JSONArray(s.blocks.map { JSONObject().put("block", it.block.name).put("visible", it.visible) }))
        put("topics", JSONArray(s.topics.toList()))
        put("terms", JSONArray(s.terms))
        put("recentSearches", JSONArray(s.recentSearches))
        put("maxAgeHours", s.maxAgeHours)
        put("localPlace", s.localPlace)
        put("places", JSONArray(s.places.map { placeToJson(it) }))
        put("fahrenheit", s.fahrenheit)
        put("mph", s.mph)
        put("markets", JSONArray(s.markets))
        put("showChangeAsPercent", s.showChangeAsPercent)
        put("readerDefault", s.readerDefault)
        put("textScale", s.textScale.toDouble())
        put("serifReader", s.serifReader)
        put("feedSummaries", s.feedSummaries)
        put("summaryLength", s.summaryLength)
        put("notifications", s.notifications)
        put("rules", JSONArray(s.rules.map { r ->
            JSONObject().put("id", r.id).put("kind", r.kind.name).put("value", r.value).put("label", r.label)
                .put("delivery", r.delivery.name).put("bigOnly", r.bigOnly).put("style", r.style.name)
        }))
        put("digest", s.digest)
        put("digestTimes", JSONArray(s.digestTimes))
        put("digestWeather", s.digestWeather)
        put("bigStoryAlerts", s.bigStoryAlerts)
        put("quietHours", s.quietHours)
        put("quietStart", s.quietStart)
        put("quietEnd", s.quietEnd)
        put("quietAllowUrgent", s.quietAllowUrgent)
        put("dailyLimit", s.dailyLimit)
        put("refreshMinutes", s.refreshMinutes)
        put("wifiOnly", s.wifiOnly)
        put("chargingOnly", s.chargingOnly)
        put("pauseOnSaver", s.pauseOnSaver)
        put("imagesWifiOnly", s.imagesWifiOnly)
    }.toString()

    fun decode(json: String?): AppSettings {
        if (json.isNullOrBlank()) return AppSettings()
        val d = AppSettings()
        return runCatching {
            val o = JSONObject(json)
            AppSettings(
                onboarded = o.optBoolean("onboarded", d.onboarded),
                edition = o.optString("edition", d.edition),
                theme = enumOr(o.optString("theme"), d.theme),
                amoled = o.optBoolean("amoled", d.amoled),
                dynamicColor = o.optBoolean("dynamicColor", d.dynamicColor),
                reducedMotion = o.optBoolean("reducedMotion", d.reducedMotion),
                tabs = o.optJSONArray("tabs")?.let { a ->
                    a.objects().mapNotNull { t ->
                        val type = runCatching { TabType.valueOf(t.optString("type")) }.getOrNull() ?: return@mapNotNull null
                        TabDef(t.optString("id"), type, t.optString("value"), t.optString("title"))
                    }
                }?.takeIf { it.isNotEmpty() } ?: d.tabs,
                blocks = o.optJSONArray("blocks")?.let { a ->
                    val parsed = a.objects().mapNotNull { b ->
                        val block = runCatching { ForYouBlock.valueOf(b.optString("block")) }.getOrNull() ?: return@mapNotNull null
                        BlockPref(block, b.optBoolean("visible", true))
                    }
                    // Keep any block added in a later version.
                    parsed + ForYouBlock.entries.filter { e -> parsed.none { it.block == e } }.map { BlockPref(it) }
                } ?: d.blocks,
                topics = o.optJSONArray("topics")?.strings()?.toSet() ?: d.topics,
                terms = o.optJSONArray("terms")?.strings() ?: d.terms,
                recentSearches = o.optJSONArray("recentSearches")?.strings() ?: d.recentSearches,
                maxAgeHours = o.optInt("maxAgeHours", d.maxAgeHours),
                localPlace = o.optString("localPlace", d.localPlace),
                places = o.optJSONArray("places")?.objects()?.mapNotNull { placeFromJson(it) } ?: d.places,
                fahrenheit = o.optBoolean("fahrenheit", d.fahrenheit),
                mph = o.optBoolean("mph", d.mph),
                markets = o.optJSONArray("markets")?.strings() ?: d.markets,
                showChangeAsPercent = o.optBoolean("showChangeAsPercent", d.showChangeAsPercent),
                readerDefault = o.optBoolean("readerDefault", d.readerDefault),
                textScale = o.optDouble("textScale", d.textScale.toDouble()).toFloat(),
                serifReader = o.optBoolean("serifReader", d.serifReader),
                feedSummaries = o.optBoolean("feedSummaries", d.feedSummaries),
                summaryLength = o.optInt("summaryLength", d.summaryLength),
                notifications = o.optBoolean("notifications", d.notifications),
                rules = o.optJSONArray("rules")?.objects()?.mapNotNull { r ->
                    val kind = runCatching { RuleKind.valueOf(r.optString("kind")) }.getOrNull() ?: return@mapNotNull null
                    NotificationRule(
                        id = r.optString("id"),
                        kind = kind,
                        value = r.optString("value"),
                        label = r.optString("label"),
                        delivery = enumOr(r.optString("delivery"), Delivery.DIGEST),
                        bigOnly = r.optBoolean("bigOnly", false),
                        style = enumOr(r.optString("style"), NotifStyle.LARGE),
                    )
                } ?: d.rules,
                digest = o.optBoolean("digest", d.digest),
                digestTimes = o.optJSONArray("digestTimes")?.ints() ?: d.digestTimes,
                digestWeather = o.optBoolean("digestWeather", d.digestWeather),
                bigStoryAlerts = o.optBoolean("bigStoryAlerts", d.bigStoryAlerts),
                quietHours = o.optBoolean("quietHours", d.quietHours),
                quietStart = o.optInt("quietStart", d.quietStart),
                quietEnd = o.optInt("quietEnd", d.quietEnd),
                quietAllowUrgent = o.optBoolean("quietAllowUrgent", d.quietAllowUrgent),
                dailyLimit = o.optInt("dailyLimit", d.dailyLimit),
                refreshMinutes = o.optInt("refreshMinutes", d.refreshMinutes),
                wifiOnly = o.optBoolean("wifiOnly", d.wifiOnly),
                chargingOnly = o.optBoolean("chargingOnly", d.chargingOnly),
                pauseOnSaver = o.optBoolean("pauseOnSaver", d.pauseOnSaver),
                imagesWifiOnly = o.optBoolean("imagesWifiOnly", d.imagesWifiOnly),
            )
        }.getOrElse { d }
    }

    fun placeToJson(p: Place): JSONObject = JSONObject()
        .put("name", p.name).put("admin", p.admin).put("country", p.country)
        .put("countryCode", p.countryCode).put("lat", p.lat).put("lon", p.lon)

    fun placeFromJson(o: JSONObject): Place? {
        if (!o.has("lat") || !o.has("lon")) return null
        return Place(
            name = o.optString("name"),
            admin = o.optString("admin"),
            country = o.optString("country"),
            countryCode = o.optString("countryCode"),
            lat = o.optDouble("lat"),
            lon = o.optDouble("lon"),
        )
    }

    private inline fun <reified T : Enum<T>> enumOr(name: String?, fallback: T): T =
        runCatching { enumValueOf<T>(name ?: "") }.getOrDefault(fallback)
}

fun JSONArray.objects(): List<JSONObject> = (0 until length()).mapNotNull { optJSONObject(it) }
fun JSONArray.strings(): List<String> = (0 until length()).mapNotNull { optString(it, null) }
fun JSONArray.ints(): List<Int> = (0 until length()).map { optInt(it) }

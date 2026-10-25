package io.github.filipembcruz.banca.data.net

import io.github.filipembcruz.banca.data.model.MarketDef
import io.github.filipembcruz.banca.data.model.Quote
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

/**
 * Quotes from Yahoo Finance's public chart endpoint (unofficial, ~15 min delay),
 * with AwesomeAPI as a fallback for currencies.
 */
class MarketApi(private val http: Http) {

    fun quote(m: MarketDef, range: String = "1mo", interval: String = "1d"): Quote {
        return runCatching { yahoo(m, range, interval) }.getOrElse { e ->
            val awesome = m.awesome ?: throw e
            awesome(m, awesome)
        }
    }

    private fun yahoo(m: MarketDef, range: String, interval: String): Quote {
        val sym = URLEncoder.encode(m.yahoo, "UTF-8")
        val url = "https://query1.finance.yahoo.com/v8/finance/chart/$sym?range=$range&interval=$interval"
        val o = JSONObject(http.getText(url))
        val res = o.getJSONObject("chart").getJSONArray("result").getJSONObject(0)
        val meta = res.getJSONObject("meta")
        val closes = res.optJSONObject("indicators")?.optJSONArray("quote")?.optJSONObject(0)?.optJSONArray("close")
        val history = closes?.let { a -> (0 until a.length()).mapNotNull { i -> if (a.isNull(i)) null else a.optDouble(i) } }.orEmpty()
        val price = meta.optDouble("regularMarketPrice", history.lastOrNull() ?: Double.NaN)
        // Previous day close: second to last daily close when available.
        val prev = if (history.size >= 2) history[history.size - 2] else meta.optDouble("chartPreviousClose", price)
        val change = price - prev
        return Quote(
            symbol = m.id,
            price = price,
            change = change,
            changePct = if (prev != 0.0) change / prev * 100 else 0.0,
            history = history,
            updated = System.currentTimeMillis(),
        )
    }

    private fun awesome(m: MarketDef, pair: String): Quote {
        val last = JSONObject(http.getText("https://economia.awesomeapi.com.br/json/last/$pair"))
        val q = last.getJSONObject(pair.replace("-", ""))
        val daily = runCatching { JSONArray(http.getText("https://economia.awesomeapi.com.br/json/daily/$pair/30")) }.getOrNull()
        val history = daily?.let { a -> (0 until a.length()).mapNotNull { a.optJSONObject(it)?.optString("bid")?.toDoubleOrNull() }.reversed() }
            .orEmpty()
        val price = q.optString("bid").toDoubleOrNull() ?: 0.0
        val pct = q.optString("pctChange").toDoubleOrNull() ?: 0.0
        return Quote(
            symbol = m.id,
            price = price,
            change = q.optString("varBid").toDoubleOrNull() ?: 0.0,
            changePct = pct,
            history = history,
            updated = System.currentTimeMillis(),
        )
    }

    fun toJson(q: Quote): String = JSONObject()
        .put("s", q.symbol).put("p", q.price).put("c", q.change).put("pc", q.changePct)
        .put("h", JSONArray(q.history)).put("u", q.updated).toString()

    fun fromJson(s: String): Quote? = runCatching {
        val o = JSONObject(s)
        val h = o.getJSONArray("h")
        Quote(o.getString("s"), o.getDouble("p"), o.getDouble("c"), o.getDouble("pc"),
            (0 until h.length()).map { h.getDouble(it) }, o.getLong("u"))
    }.getOrNull()
}

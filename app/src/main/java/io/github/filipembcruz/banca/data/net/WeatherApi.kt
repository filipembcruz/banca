package io.github.filipembcruz.banca.data.net

import io.github.filipembcruz.banca.data.model.Place
import io.github.filipembcruz.banca.data.model.Weather
import io.github.filipembcruz.banca.data.model.WeatherDay
import io.github.filipembcruz.banca.data.settings.objects
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Locale

/** Open-Meteo: free, no API key. */
class WeatherApi(private val http: Http) {

    fun forecast(p: Place): Weather {
        val url = "https://api.open-meteo.com/v1/forecast?latitude=${fmt(p.lat)}&longitude=${fmt(p.lon)}" +
            "&current=temperature_2m,apparent_temperature,relative_humidity_2m,weather_code,wind_speed_10m,is_day" +
            "&daily=weather_code,temperature_2m_max,temperature_2m_min&timezone=auto&forecast_days=6"
        val o = JSONObject(http.getText(url))
        val c = o.getJSONObject("current")
        val d = o.getJSONObject("daily")
        val dates = d.getJSONArray("time")
        val codes = d.getJSONArray("weather_code")
        val max = d.getJSONArray("temperature_2m_max")
        val min = d.getJSONArray("temperature_2m_min")
        val days = (0 until dates.length()).map { i ->
            WeatherDay(dates.getString(i), codes.optInt(i), max.optDouble(i), min.optDouble(i))
        }
        return Weather(
            place = p,
            temp = c.optDouble("temperature_2m"),
            feelsLike = c.optDouble("apparent_temperature"),
            code = c.optInt("weather_code"),
            isDay = c.optInt("is_day", 1) == 1,
            wind = c.optDouble("wind_speed_10m"),
            humidity = c.optInt("relative_humidity_2m"),
            days = days,
            updated = System.currentTimeMillis(),
        )
    }

    fun searchPlaces(query: String, language: String): List<Place> {
        val url = "https://geocoding-api.open-meteo.com/v1/search?name=${URLEncoder.encode(query, "UTF-8")}" +
            "&count=8&language=$language&format=json"
        val o = JSONObject(http.getText(url))
        val results = o.optJSONArray("results") ?: return emptyList()
        return results.objects().map { r ->
            Place(
                name = r.optString("name"),
                admin = r.optString("admin1"),
                country = r.optString("country"),
                countryCode = r.optString("country_code"),
                lat = r.optDouble("latitude"),
                lon = r.optDouble("longitude"),
            )
        }
    }

    private fun fmt(v: Double) = String.format(Locale.US, "%.4f", v)

    fun toJson(w: Weather): String = JSONObject()
        .put("temp", w.temp).put("feels", w.feelsLike).put("code", w.code).put("day", w.isDay)
        .put("wind", w.wind).put("hum", w.humidity).put("updated", w.updated)
        .put("days", org.json.JSONArray(w.days.map {
            JSONObject().put("d", it.date).put("c", it.code).put("x", it.max).put("n", it.min)
        })).toString()

    fun fromJson(p: Place, s: String): Weather? = runCatching {
        val o = JSONObject(s)
        Weather(
            place = p,
            temp = o.getDouble("temp"),
            feelsLike = o.optDouble("feels"),
            code = o.optInt("code"),
            isDay = o.optBoolean("day", true),
            wind = o.optDouble("wind"),
            humidity = o.optInt("hum"),
            days = o.getJSONArray("days").objects().map {
                WeatherDay(it.optString("d"), it.optInt("c"), it.optDouble("x"), it.optDouble("n"))
            },
            updated = o.optLong("updated"),
        )
    }.getOrNull()
}

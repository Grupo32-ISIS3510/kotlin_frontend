package com.app.secondserving.data.network

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

data class WeatherInfo(
    val temperature: Double,
    val humidity: Int,
    val description: String,
    val city: String
)

class WeatherService(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("weather_cache", Context.MODE_PRIVATE)

    private val apiKey = "fce5b8c63439b7718b2389e3511105db"
    private val cacheTimeMs = 24 * 60 * 60 * 1000L // 24 horas

    fun getCachedWeatherIfFresh(): WeatherInfo? {
        val cachedTime = prefs.getLong("cache_time", 0)
        val isFresh = System.currentTimeMillis() - cachedTime < cacheTimeMs
        if (!isFresh) return null
        val cached = prefs.getString("cache_data", null) ?: return null
        return parseWeather(cached)
    }

    suspend fun getWeather(lat: Double, lon: Double): WeatherInfo? {
        // Revisar cache primero
        getCachedWeatherIfFresh()?.let { return it }

        return withContext(Dispatchers.IO) {
            try {
                val url = "https://api.openweathermap.org/data/2.5/weather" +
                        "?lat=$lat&lon=$lon&appid=$apiKey&units=metric"
                val response = URL(url).readText()
                prefs.edit()
                    .putString("cache_data", response)
                    .putLong("cache_time", System.currentTimeMillis())
                    .apply()
                parseWeather(response)
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun parseWeather(json: String): WeatherInfo? {
        return try {
            val obj = JSONObject(json)
            val main = obj.getJSONObject("main")
            val weather = obj.getJSONArray("weather").getJSONObject(0)
            WeatherInfo(
                temperature = main.getDouble("temp"),
                humidity = main.getInt("humidity"),
                description = weather.getString("description"),
                city = obj.getString("name")
            )
        } catch (e: Exception) {
            null
        }
    }

    fun getStorageTip(itemName: String, weather: WeatherInfo): String {
        val name = itemName.lowercase()
        val temp = weather.temperature
        val humidity = weather.humidity

        return when {
            humidity > 70 && isFruit(name) ->
                "🌧️ Alta humedad hoy (${humidity}%) — guarda $itemName en lugar seco o refrigerado"
            temp > 28 && isFruit(name) ->
                "🌡️ Hace calor (${temp.toInt()}°C) — refrigera $itemName para que dure más"
            temp > 28 && isVegetable(name) ->
                "🌡️ Temperatura alta — mantén $itemName en el refrigerador"
            temp < 8 ->
                "❄️ Hace frío (${temp.toInt()}°C) — $itemName puede conservarse bien a temperatura ambiente"
            humidity > 70 ->
                "💧 Humedad alta hoy — asegúrate de sellar bien $itemName"
            temp in 18.0..24.0 && humidity < 60 ->
                "✅ Condiciones ideales — $itemName puede almacenarse a temperatura ambiente"
            else ->
                "🌤️ ${temp.toInt()}°C, humedad ${humidity}% — revisa el empaque de $itemName"
        }
    }

    private fun isFruit(name: String) =
        listOf("manzana", "pera", "uva", "fresa", "mango", "sandia", "melon",
            "durazno", "ciruela", "cereza", "aguacate", "limon", "naranja").any { name.contains(it) }

    private fun isVegetable(name: String) =
        listOf("tomate", "lechuga", "zanahoria", "brocoli", "espinaca",
            "cebolla", "papa", "yuca", "pepino", "apio").any { name.contains(it) }
}
package com.app.secondserving.data.network

import android.content.Context
import android.content.SharedPreferences
import android.location.Geocoder
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.util.Locale

data class WeatherInfo(
    val temperature: Double,
    val humidity: Int,
    val description: String,
    val city: String
)

class WeatherService(context: Context) {

    private val appContext = context.applicationContext
    private val prefs: SharedPreferences =
        context.getSharedPreferences("weather_cache", Context.MODE_PRIVATE)

    private val gson = Gson()
    private val apiKey = "fce5b8c63439b7718b2389e3511105db"
    private val cacheTimeMs = 24 * 60 * 60 * 1000L // 24 horas

    fun getCachedWeatherIfFresh(): WeatherInfo? {
        val cachedTime = prefs.getLong("cache_time", 0)
        val isFresh = System.currentTimeMillis() - cachedTime < cacheTimeMs
        if (!isFresh) return null
        val cached = prefs.getString("cache_weather", null) ?: return null
        return try {
            gson.fromJson(cached, WeatherInfo::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getWeather(lat: Double, lon: Double): WeatherInfo? {
        // Cache primero (ya trae city resuelta vía Geocoder).
        getCachedWeatherIfFresh()?.let { return it }

        return withContext(Dispatchers.IO) {
            try {
                val url = "https://api.openweathermap.org/data/2.5/weather" +
                        "?lat=$lat&lon=$lon&appid=$apiKey&units=metric"
                val response = URL(url).readText()
                val raw = parseWeather(response) ?: return@withContext null
                // OpenWeatherMap.name a veces devuelve el nombre del barrio o de
                // la estación meteorológica más cercana ("Estanzuela") en lugar
                // de la ciudad ("Bogotá"). Pasamos por Geocoder de Android para
                // resolver lat/lon a una localidad real; si Geocoder falla,
                // dejamos el name original como fallback.
                val resolvedCity = reverseGeocodeCity(lat, lon, raw.city)
                val weather = raw.copy(city = resolvedCity)
                prefs.edit()
                    .putString("cache_weather", gson.toJson(weather))
                    .putLong("cache_time", System.currentTimeMillis())
                    .apply()
                weather
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

    private fun reverseGeocodeCity(lat: Double, lon: Double, fallback: String): String {
        return try {
            if (!Geocoder.isPresent()) return fallback
            val geocoder = Geocoder(appContext, Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            val first = addresses?.firstOrNull()
            // Preferimos `locality` (Bogotá), después `subAdminArea` (provincia)
            // y como último recurso `adminArea` (departamento). Si todo falla
            // se mantiene el name de OpenWeather como fallback.
            first?.locality?.takeIf { it.isNotBlank() }
                ?: first?.subAdminArea?.takeIf { it.isNotBlank() }
                ?: first?.adminArea?.takeIf { it.isNotBlank() }
                ?: fallback
        } catch (e: Exception) {
            fallback
        }
    }

    /**
     * Genera un tip de almacenamiento según la categoría del producto
     * (Frutas, Verduras, Lácteos, Carnes, Otros) y las condiciones climáticas
     * actuales. La categoría es el discriminador principal porque el nombre
     * del producto por sí solo no es suficiente: "kiwi" o "pollo" no aparecen
     * en una lista cerrada de keywords y caen siempre al branch genérico.
     */
    fun getStorageTip(itemName: String, category: String, weather: WeatherInfo): String {
        val cat = category.trim().lowercase(Locale.ROOT)
        val temp = weather.temperature
        val humidity = weather.humidity
        val tempInt = temp.toInt()

        return when (cat) {
            "carnes" -> when {
                temp > 25 -> "🌡 Hace calor (${tempInt}°C) — refrigera $itemName de inmediato; las carnes se descomponen rápido"
                humidity > 70 -> "💧 Humedad ${humidity}% — guarda $itemName en envase hermético en la nevera"
                else -> "🥩 Mantén $itemName refrigerado a 0–4 °C para preservar la frescura"
            }
            "lácteos", "lacteos" -> when {
                temp > 25 -> "🌡 ${tempInt}°C — refrigera $itemName cuanto antes; los lácteos se cortan con el calor"
                humidity > 70 -> "💧 Humedad ${humidity}% — conserva $itemName cerrado en la nevera"
                else -> "🥛 Guarda $itemName en la parte central del refrigerador (no en la puerta)"
            }
            "frutas" -> when {
                humidity > 70 -> "💧 Humedad ${humidity}% — guarda $itemName en lugar ventilado para evitar moho"
                temp > 28 -> "🌡 Hace calor (${tempInt}°C) — refrigera $itemName para retrasar la maduración"
                temp < 8 -> "❄ ${tempInt}°C — $itemName puede estar a temperatura ambiente sin problema"
                else -> "🍎 $itemName a temperatura ambiente; refrigéralo cuando madure"
            }
            "verduras" -> when {
                humidity > 70 -> "💧 Humedad ${humidity}% — guarda $itemName con un papel absorbente para no acumular agua"
                temp > 25 -> "🌡 Hace calor (${tempInt}°C) — refrigera $itemName en el cajón de verduras"
                else -> "🥬 Conserva $itemName en el cajón de verduras del refrigerador"
            }
            else -> when {
                humidity > 70 -> "💧 Humedad ${humidity}% — sella bien el empaque de $itemName y guárdalo en lugar seco"
                temp > 28 -> "🌡 ${tempInt}°C — guarda $itemName en lugar fresco y seco"
                temp < 8 -> "❄ ${tempInt}°C — $itemName conserva bien a temperatura ambiente"
                else -> "📦 Guarda $itemName en lugar fresco y seco, bien sellado"
            }
        }
    }
}

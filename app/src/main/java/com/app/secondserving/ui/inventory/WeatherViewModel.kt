package com.app.secondserving.ui.inventory

import android.annotation.SuppressLint
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.app.secondserving.data.network.WeatherInfo
import com.app.secondserving.data.network.WeatherService
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class WeatherUiState {
    object Idle : WeatherUiState()
    object Loading : WeatherUiState()
    data class Success(val weather: WeatherInfo) : WeatherUiState()
    data class Error(val message: String) : WeatherUiState()
}

class WeatherViewModel(application: Application) : AndroidViewModel(application) {

    private val weatherService = WeatherService(application)
    private val fusedLocationClient =
        LocationServices.getFusedLocationProviderClient(application)

    private val _weatherState = MutableStateFlow<WeatherUiState>(WeatherUiState.Idle)
    val weatherState: StateFlow<WeatherUiState> = _weatherState.asStateFlow()

    fun loadWeather() {
        weatherService.getCachedWeatherIfFresh()?.let { cached ->
            _weatherState.value = WeatherUiState.Success(cached)
            return
        }

        val hasPermission = ContextCompat.checkSelfPermission(
            getApplication(),
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            _weatherState.value = WeatherUiState.Error("Permiso de ubicación no concedido")
            return
        }
        fetchLocation()
    }

    @SuppressLint("MissingPermission")
    private fun fetchLocation() {
        viewModelScope.launch {
            _weatherState.value = WeatherUiState.Loading
            try {
                // Intentar con lastLocation primero
                val lastLocation = fusedLocationClient.lastLocation.await()
                if (lastLocation != null) {
                    val weather = weatherService.getWeather(lastLocation.latitude, lastLocation.longitude)
                    if (weather != null) {
                        _weatherState.value = WeatherUiState.Success(weather)
                    } else {
                        _weatherState.value = WeatherUiState.Error("No se pudo obtener el clima")
                    }
                } else {
                    // Fallback: usar coordenadas de Bogotá si no hay ubicación
                    val weather = weatherService.getWeather(4.7110, -74.0721)
                    if (weather != null) {
                        _weatherState.value = WeatherUiState.Success(weather)
                    } else {
                        _weatherState.value = WeatherUiState.Error("No se pudo obtener el clima")
                    }
                }
            } catch (e: Exception) {
                _weatherState.value = WeatherUiState.Error("Error: ${e.message}")
            }
        }
    }

    fun getStorageTip(itemName: String, category: String): String {
        val state = _weatherState.value
        return if (state is WeatherUiState.Success) {
            weatherService.getStorageTip(itemName, category, state.weather)
        } else {
            " Cargando recomendación..."
        }
    }
}
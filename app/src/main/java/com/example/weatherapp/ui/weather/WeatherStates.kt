package com.example.weatherapp.ui.weather

import com.example.weatherapp.ui.weather.WeatherTab
import com.example.weatherapp.domain.weather.WeatherData
import com.example.weatherapp.domain.weather.WeatherHistoryItem

sealed class PermissionState {
    object Checking : PermissionState()
    object Granted : PermissionState()
    object DeniedCanAskAgain : PermissionState()
    object DeniedPermanently : PermissionState()
}

sealed class LocationState {
    object Idle : LocationState()
    object Loading : LocationState()
    data class Ready(val lat: Double, val lon: Double) : LocationState()
    object ServiceOff : LocationState()
    data class Error(val message: String) : LocationState()
}

sealed class WeatherState {
    object Idle : WeatherState()
    object Loading : WeatherState()
    data class Loaded(val data: WeatherData) : WeatherState()
    data class Error(val message: String) : WeatherState()
}

data class WeatherUiState(
    val username: String = "",
    val permissionState: PermissionState = PermissionState.Checking,
    val locationState: LocationState = LocationState.Idle,
    val weatherState: WeatherState = WeatherState.Idle,
    val selectedTab: WeatherTab = WeatherTab.Current,
    val historyItems: List<WeatherHistoryItem> = emptyList()
)

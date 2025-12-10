package com.example.weatherapp.domain.location

sealed class LocationResult {
    data class Success(val lat: Double, val lon: Double) : LocationResult()
    object ServiceOff : LocationResult()
    data class Error(val message: String) : LocationResult()
}
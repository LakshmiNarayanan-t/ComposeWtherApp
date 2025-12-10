package com.example.weatherapp.domain.weather

data class WeatherHistoryItem(
    val id: Long,
    val city: String,
    val country: String,
    val tempC: Double,
    val description: String,
    val fetchedAt: Long,
    val sunrise: Long,
    val sunset: Long,
)
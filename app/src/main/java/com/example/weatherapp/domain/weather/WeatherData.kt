package com.example.weatherapp.domain.weather

data class WeatherData(

    val city: String,
    val country: String,
    val lat: Double,
    val lon: Double,
    val tempC: Double,
    val main:String,
    val description: String,
    val iconCode: String,
    val sunrise: Long,
    val sunset: Long,
    val fetchedAt: Long
)
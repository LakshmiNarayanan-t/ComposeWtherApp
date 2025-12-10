package com.example.weatherapp.domain.weather

import com.example.weatherapp.domain.weather.WeatherData
import com.example.weatherapp.domain.weather.WeatherHistoryItem
import kotlinx.coroutines.flow.Flow

interface WeatherRepository {
    suspend fun fetchCurrentWeather(lat: Double, lon: Double): Result<WeatherData>
    fun getWeatherHistory(): Flow<List<WeatherHistoryItem>>
}
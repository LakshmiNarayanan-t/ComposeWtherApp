package com.example.weatherapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weather_history")
data class WeatherHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val city: String,
    val country: String,
    val lat: Double,
    val lon: Double,
    val tempC: Double,
    val description: String,
    val iconCode: String,
    val sunrise: Long,
    val sunset: Long,
    val fetchedAt: Long
)
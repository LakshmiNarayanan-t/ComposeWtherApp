package com.example.weatherapp.data.remote.dto

data class WeatherResponseDto(
    val coord: CoordDto?,
    val weather: List<WeatherDto>?,
    val main: MainDto?,
    val sys: SysDto?,
    val dt: Long?,
    val name: String?,
    val timezone: Int?
)

data class CoordDto(
    val lon: Double?,
    val lat: Double?
)

data class WeatherDto(
    val id: Int?,
    val main: String?,
    val description: String?,
    val icon: String?
)

data class MainDto(
    val temp: Double?
)

data class SysDto(
    val country: String?,
    val sunrise: Long?,
    val sunset: Long?
)

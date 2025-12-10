package com.example.weatherapp.domain.location

interface LocationRepository {
    suspend fun getCurrentLocation(): LocationResult
}
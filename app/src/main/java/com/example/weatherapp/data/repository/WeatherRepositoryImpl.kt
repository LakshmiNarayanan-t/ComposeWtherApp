package com.example.weatherapp.data.repository

import com.example.weatherapp.domain.weather.WeatherData
import com.example.weatherapp.domain.weather.WeatherHistoryItem
import com.example.weatherapp.domain.weather.WeatherRepository
import com.example.weatherapp.data.local.WeatherHistoryEntity
import com.example.weatherapp.data.local.WeatherDao
import com.example.weatherapp.data.remote.OpenWeatherApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class WeatherRepositoryImpl @Inject constructor(
    private val api: OpenWeatherApi,
    private val weatherDao: WeatherDao
) : WeatherRepository {

    override suspend fun fetchCurrentWeather(lat: Double, lon: Double): Result<WeatherData> {
        return try {
            val dto = api.getCurrentWeather(lat, lon)

            val city = dto.name ?: "Unknown"
            val country = dto.sys?.country ?: ""
            val tempC = dto.main?.temp ?: 0.0
            val description = dto.weather?.firstOrNull()?.description ?: "N/A"
            val main = dto.weather?.firstOrNull()?.main ?: "N/A"
            val iconCode = dto.weather?.firstOrNull()?.icon ?: ""
            val sunrise = dto.sys?.sunrise ?: 0L
            val sunset = dto.sys?.sunset ?: 0L
            val fetchedAt = System.currentTimeMillis()

            val data = WeatherData(
                city = city,
                country = country,
                lat = lat,
                lon = lon,
                tempC = tempC,
                main = main,
                description = description,
                iconCode = iconCode,
                sunrise = sunrise,
                sunset = sunset,
                fetchedAt = fetchedAt
            )

            val entity = WeatherHistoryEntity(
                city = city,
                country = country,
                lat = lat,
                lon = lon,
                tempC = tempC,
                description = description,
                iconCode = iconCode,
                sunrise = sunrise,
                sunset = sunset,
                fetchedAt = fetchedAt
            )

            weatherDao.insertWeather(entity)

            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getWeatherHistory(): Flow<List<WeatherHistoryItem>> {
        return weatherDao.getWeatherHistory().map { list ->
            list.map { entity ->
                WeatherHistoryItem(
                    id = entity.id,
                    city = entity.city,
                    country = entity.country,
                    tempC = entity.tempC,
                    description = entity.description,
                    fetchedAt = entity.fetchedAt,
                    sunrise = entity.sunrise,
                    sunset = entity.sunset
                )
            }
        }
    }
}
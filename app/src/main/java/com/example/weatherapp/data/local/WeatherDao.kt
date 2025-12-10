package com.example.weatherapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.weatherapp.data.local.WeatherHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeatherDao {

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertWeather(history: WeatherHistoryEntity)

    @Query("SELECT * FROM weather_history ORDER BY fetchedAt DESC")
    fun getWeatherHistory(): Flow<List<WeatherHistoryEntity>>
}
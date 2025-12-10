package com.example.weatherapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.weatherapp.data.local.UserDao
import com.example.weatherapp.data.local.UserEntity
import com.example.weatherapp.data.local.WeatherDao
import com.example.weatherapp.data.local.WeatherHistoryEntity

@Database(
    entities = [
        UserEntity::class,
        WeatherHistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun weatherDao(): WeatherDao
}
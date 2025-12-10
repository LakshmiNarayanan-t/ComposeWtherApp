package com.example.weatherapp.di

import android.content.Context
import androidx.room.Room
import com.example.weatherapp.domain.auth.AuthRepository
import com.example.weatherapp.data.local.AppDatabase
import com.example.weatherapp.data.local.UserDao
import com.example.weatherapp.data.local.WeatherDao
import com.example.weatherapp.data.location.LocationRepositoryImpl
import com.example.weatherapp.data.remote.OpenWeatherApi
import com.example.weatherapp.data.repository.AuthRepositoryImpl
import com.example.weatherapp.data.repository.WeatherRepositoryImpl
import com.example.weatherapp.domain.location.LocationRepository
import com.example.weatherapp.domain.weather.WeatherRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "weather_app_db"
        )
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideUserDao(db: AppDatabase): UserDao = db.userDao()

    @Provides
    fun provideWeatherDao(db: AppDatabase): WeatherDao = db.weatherDao()

    @Provides
    @Singleton
    fun provideAuthRepository(
        userDao: UserDao
    ): AuthRepository = AuthRepositoryImpl(userDao)

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder().build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideOpenWeatherApi(retrofit: Retrofit): OpenWeatherApi =
        retrofit.create(OpenWeatherApi::class.java)

    @Provides
    @Singleton
    fun provideWeatherRepository(
        api: OpenWeatherApi,
        weatherDao: WeatherDao
    ): WeatherRepository = WeatherRepositoryImpl(api, weatherDao)

    @Provides
    @Singleton
    fun provideFusedLocationProviderClient(
        @ApplicationContext context: Context
    ): FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    @Provides
    @Singleton
    fun provideLocationRepository(
        fusedClient: FusedLocationProviderClient,
        @ApplicationContext context: Context
    ): LocationRepository = LocationRepositoryImpl(fusedClient, context)
}
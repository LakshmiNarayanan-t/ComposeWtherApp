package com.example.weatherapp.ui.weather

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weatherapp.domain.auth.AuthRepository
import com.example.weatherapp.domain.location.LocationRepository
import com.example.weatherapp.domain.location.LocationResult
import com.example.weatherapp.domain.weather.WeatherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val weatherRepository: WeatherRepository,
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeatherUiState())
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private var lastLatLon: Pair<Double, Double>? = null

    init {
        loadUsername()
        observeHistory()
    }

    private fun loadUsername() {
        viewModelScope.launch {
            val user = authRepository.getLoggedInUser()
            _uiState.update { it.copy(username = user?.username.orEmpty()) }
        }
    }

    private fun observeHistory() {
        viewModelScope.launch {
            weatherRepository.getWeatherHistory().collect { items ->
                _uiState.update { it.copy(historyItems = items) }
            }
        }
    }

    /**
     * Used from Weather screen whenever we *check* current permission status
     * (initially + onResume).
     */
    fun onPermissionStatusChanged(
        permissionGranted: Boolean,
        permanentlyDenied: Boolean
    ) {
        if (permissionGranted) {
            // Only react when we move from non-Granted to Granted
            val current = _uiState.value
            if (current.permissionState !is PermissionState.Granted) {
                _uiState.update {
                    it.copy(
                        permissionState = PermissionState.Granted,
                        // allow new location/weather fetch
                        locationState = LocationState.Idle,
                        weatherState = WeatherState.Idle
                    )
                }
                fetchLocationAndWeather()
            }
        } else {
            val newState = if (permanentlyDenied) {
                PermissionState.DeniedPermanently
            } else {
                PermissionState.DeniedCanAskAgain
            }

            _uiState.update {
                it.copy(
                    permissionState = newState,
                    locationState = LocationState.Idle,
                    weatherState = WeatherState.Idle
                )
            }
        }
    }

    /**
     * Called after the permission dialog result in WeatherScreen.
     * Just delegates to [onPermissionStatusChanged].
     */
    fun onLocationPermissionResult(granted: Boolean, permanentlyDenied: Boolean) {
        onPermissionStatusChanged(granted, permanentlyDenied)
    }

    private fun fetchLocationAndWeather() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    locationState = LocationState.Loading,
                    weatherState = WeatherState.Idle
                )
            }

            when (val result = locationRepository.getCurrentLocation()) {
                is LocationResult.Success -> {
                    lastLatLon = result.lat to result.lon
                    _uiState.update {
                        it.copy(locationState = LocationState.Ready(result.lat, result.lon))
                    }
                    fetchWeather(result.lat, result.lon)
                }
                is LocationResult.ServiceOff -> {
                    _uiState.update {
                        it.copy(
                            locationState = LocationState.ServiceOff,
                            weatherState = WeatherState.Idle
                        )
                    }
                }
                is LocationResult.Error -> {
                    _uiState.update {
                        it.copy(
                            locationState = LocationState.Error(result.message),
                            weatherState = WeatherState.Idle
                        )
                    }
                }
            }
        }
    }

    private fun fetchWeather(lat: Double, lon: Double) {
        viewModelScope.launch {
            _uiState.update { it.copy(weatherState = WeatherState.Loading) }
            val result = weatherRepository.fetchCurrentWeather(lat, lon)
            result.fold(
                onSuccess = { data ->
                    _uiState.update {
                        it.copy(weatherState = WeatherState.Loaded(data))
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            weatherState = WeatherState.Error(
                                "Unable to fetch weather, please check the internet connection / location access and try again."
                            )

                        )
                    }
                }
            )
        }
    }

    fun onFetchLocationAgain() {
        val permission = _uiState.value.permissionState
        if (permission is PermissionState.Granted) {
            fetchLocationAndWeather()
        }
    }

    fun onRetryWeather() {
        val coords = lastLatLon
        if (coords != null) {
            fetchWeather(coords.first, coords.second)
        }
    }

    fun onTabSelected(tab: WeatherTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onLoggedOut()
        }
    }
}
package com.example.weatherapp.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weatherapp.domain.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

sealed class SplashDestination {
    object Login : SplashDestination()
    object Weather : SplashDestination()
}

sealed class SplashPermissionState {
    object Checking : SplashPermissionState()
    object Granted : SplashPermissionState()
    object DeniedCanAskAgain : SplashPermissionState()
    object DeniedPermanently : SplashPermissionState()
}

data class SplashUiState(
    val isChecking: Boolean = true,
    val permissionState: SplashPermissionState = SplashPermissionState.Checking,
    val isGpsOn: Boolean = false,
    val errorMessage: String? = null,
    val destination: SplashDestination? = null
)

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    /**
     * Called by SplashScreen whenever it checks the current permission + GPS status.
     *
     * @param permissionGranted true if ACCESS_FINE_LOCATION is granted
     * @param permanentlyDenied true if user selected "Don't ask again"
     * @param gpsOn true if GPS / location service is enabled
     */
    fun updatePermissionAndGps(
        permissionGranted: Boolean,
        permanentlyDenied: Boolean,
        gpsOn: Boolean
    ) {
        if (!permissionGranted) {
            val permState = if (permanentlyDenied) {
                SplashPermissionState.DeniedPermanently
            } else {
                SplashPermissionState.DeniedCanAskAgain
            }

            _uiState.update {
                it.copy(
                    isChecking = false,
                    permissionState = permState,
                    isGpsOn = false,
                    errorMessage = "Location permission is required to continue."
                )
            }
            return
        }

        if (!gpsOn) {
            _uiState.update {
                it.copy(
                    isChecking = false,
                    permissionState = SplashPermissionState.Granted,
                    isGpsOn = false,
                    errorMessage = "Location service (GPS) is turned off. Please enable it to continue."
                )
            }
            return
        }

        // Permission granted + GPS on → decide where to send the user.
        if (_uiState.value.destination == null) {
            _uiState.update {
                it.copy(
                    isChecking = true,
                    permissionState = SplashPermissionState.Granted,
                    isGpsOn = true,
                    errorMessage = null
                )
            }
            decideAuthDestination()
        }
    }

    private fun decideAuthDestination() {
        viewModelScope.launch {
            val user = authRepository.getLoggedInUser()
            val dest = if (user != null) {
                SplashDestination.Weather
            } else {
                SplashDestination.Login
            }
            _uiState.update { it.copy(isChecking = false, destination = dest) }
        }
    }

    fun clearDestination() {
        _uiState.update { it.copy(destination = null) }
    }
}


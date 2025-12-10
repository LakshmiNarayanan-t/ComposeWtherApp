package com.example.weatherapp.ui.splash

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.weatherapp.Screen   // your sealed Screen(Login/Weather)

@Composable
fun SplashScreen(
    onRouteDecided: (String) -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current

    // Helper: is GPS / location service enabled?
    fun isLocationServiceEnabled(): Boolean {
        val lm = context.getSystemService(Activity.LOCATION_SERVICE) as LocationManager
        val gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val networkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        return gpsEnabled || networkEnabled
    }

    // Permission launcher for ACCESS_FINE_LOCATION
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (activity == null) return@rememberLauncherForActivityResult

        val shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        val permanentlyDenied = !granted && !shouldShowRationale
        val gpsOn = if (granted) isLocationServiceEnabled() else false

        viewModel.updatePermissionAndGps(
            permissionGranted = granted,
            permanentlyDenied = permanentlyDenied,
            gpsOn = gpsOn
        )
    }


    // Initial check on first composition: request permission if not granted
    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            // First time: directly ask for permission on Splash
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            val gpsOn = isLocationServiceEnabled()
            viewModel.updatePermissionAndGps(
                permissionGranted = true,
                permanentlyDenied = false,
                gpsOn = gpsOn
            )
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (activity == null) return@LifecycleEventObserver

                val hasPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

                val shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
                val permanentlyDenied = !hasPermission && !shouldShowRationale

                val gpsOn = if (hasPermission) isLocationServiceEnabled() else false

                viewModel.updatePermissionAndGps(
                    permissionGranted = hasPermission,
                    permanentlyDenied = permanentlyDenied,
                    gpsOn = gpsOn
                )
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }


    // Navigate once permission + GPS OK and auth destination decided
    LaunchedEffect(state.destination) {
        when (state.destination) {
            SplashDestination.Login -> {
                onRouteDecided(Screen.Login.route)
                viewModel.clearDestination()
            }
            SplashDestination.Weather -> {
                onRouteDecided(Screen.Weather.route)
                viewModel.clearDestination()
            }
            null -> Unit
        }
    }

    // UI: block user on Splash until requirements are met
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        when (state.permissionState) {
            SplashPermissionState.Checking -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Checking permissions…")
                }
            }

            SplashPermissionState.Granted -> {
                if (!state.isGpsOn) {
                    // GPS off
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = state.errorMessage
                                ?: "Location service (GPS) is turned off. Please enable it to continue.",
                            modifier = Modifier.padding(8.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                                context.startActivity(intent)
                            }
                        ) {
                            Text("Open Location Settings")
                        }
                    }
                } else {
                    // Permission + GPS OK but maybe still checking auth → show simple loader
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Loading…")
                    }
                }
            }

            SplashPermissionState.DeniedCanAskAgain -> {
                // User denied but we can still ask again
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = state.errorMessage
                            ?: "Location permission is required to continue.",
                        modifier = Modifier.padding(8.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                    ) {
                        Text("Allow Location Permission")
                    }
                }
            }

            SplashPermissionState.DeniedPermanently -> {
                // User selected "Don't ask again" → send them to app settings
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = state.errorMessage
                            ?: "Location permission is required. Enable it in app settings.",
                        modifier = Modifier.padding(8.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val intent = Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null)
                            )
                            context.startActivity(intent)
                        }
                    ) {
                        Text("Open App Settings")
                    }
                }
            }
        }
    }
}

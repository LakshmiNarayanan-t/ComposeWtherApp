package com.example.weatherapp.ui.weather

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.weatherapp.ui.weather.WeatherTab
import com.example.weatherapp.domain.weather.WeatherData
import com.example.weatherapp.domain.weather.WeatherHistoryItem
import java.text.DateFormat
import java.util.*

@Composable
fun WeatherHomeScreen(
    paddingValues: PaddingValues,
    onLogout: () -> Unit,
    viewModel: WeatherViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current

    // Permission launcher for this screen (ACCESS_FINE_LOCATION)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (activity == null) return@rememberLauncherForActivityResult

        val shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        val permanentlyDenied = !granted && !shouldShowRationale

        viewModel.onLocationPermissionResult(
            granted = granted,
            permanentlyDenied = permanentlyDenied
        )
    }

    // Initial permission check when entering Weather screen
    LaunchedEffect(Unit) {
        if (activity == null) return@LaunchedEffect

        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        val permanentlyDenied = !hasPermission && !shouldShowRationale

        viewModel.onPermissionStatusChanged(
            permissionGranted = hasPermission,
            permanentlyDenied = permanentlyDenied
        )
    }

    // Re-check permission status when coming back from Settings / background
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

                viewModel.onPermissionStatusChanged(
                    permissionGranted = hasPermission,
                    permanentlyDenied = permanentlyDenied
                )
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // UI
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .padding(paddingValues)
    ) {
        // Top row: welcome + logout
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Welcome ${state.username}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = { viewModel.logout(onLogout) }) {
                Text("Logout")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tabs
        val tabs = listOf(WeatherTab.Current, WeatherTab.History)
        TabRow(selectedTabIndex = tabs.indexOf(state.selectedTab)) {
            tabs.forEach { tab ->
                Tab(
                    selected = tab == state.selectedTab,
                    onClick = { viewModel.onTabSelected(tab) },
                    text = {
                        Text(
                            when (tab) {
                                WeatherTab.Current -> "Current Weather"
                                WeatherTab.History -> "Weather List"
                            }
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (state.selectedTab) {
            WeatherTab.Current -> CurrentWeatherTab(
                state = state,
                onRequestPermission = {
                    // Only used in DeniedCanAskAgain UI
                    permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                },
                onOpenAppSettings = {
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null)
                    )
                    context.startActivity(intent)
                },
                onOpenLocationSettings = {
                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    context.startActivity(intent)
                },
                onFetchLocationAgain = viewModel::onFetchLocationAgain,
                onRetryWeather = viewModel::onRetryWeather
            )

            WeatherTab.History -> HistoryTab(historyItems = state.historyItems)
        }
    }
}

@Composable
private fun CurrentWeatherTab(
    state: WeatherUiState,
    onRequestPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenLocationSettings: () -> Unit,
    onFetchLocationAgain: () -> Unit,
    onRetryWeather: () -> Unit
) {
    when (state.permissionState) {
        PermissionState.Checking -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Checking location permission…")
                }
            }
        }

        PermissionState.Granted -> {
            // Handle location + weather based on LocationState / WeatherState
            when (val locState = state.locationState) {
                LocationState.Idle,
                LocationState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Fetching your location…")
                        }
                    }
                }

                is LocationState.Ready -> {
                    when (val weatherState = state.weatherState) {
                        WeatherState.Idle,
                        WeatherState.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator()
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("Fetching weather…")
                                }
                            }
                        }

                        is WeatherState.Error -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(weatherState.message)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(onClick = onRetryWeather) {
                                        Text("Retry")
                                    }
                                }
                            }
                        }

                        is WeatherState.Loaded -> {
                            CurrentWeatherContent(data = weatherState.data)
                        }
                    }
                }

                LocationState.ServiceOff -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Location service is turned off. Please enable GPS / location services.",
                                modifier = Modifier.padding(16.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = onOpenLocationSettings) {
                                Text("Open Location Settings")
                            }
                        }
                    }
                }

                is LocationState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                locState.message,
                                modifier = Modifier.padding(16.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = onFetchLocationAgain) {
                                Text("Fetch Location Again")
                            }
                        }
                    }
                }
            }
        }

        PermissionState.DeniedCanAskAgain -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Location permission is required to show current weather.",
                        modifier = Modifier.padding(16.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = onRequestPermission) {
                        Text("Allow Location Permission")
                    }
                }
            }
        }

        PermissionState.DeniedPermanently -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Location permission is required so enable it on app setting screen.",
                        modifier = Modifier.padding(16.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = onOpenAppSettings) {
                        Text("Click to go to setting screen")
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrentWeatherContent(data: WeatherData) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {


        val timeFormatter = remember {
            DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault())
        }
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val isNight = hour >= 18 || hour < 6
        val iconText = when {
            data.description.contains("rain", ignoreCase = true) -> "🌧️"
            isNight -> "🌙"
            else -> "☀️"
        }
        Text(text = "Current City and Country : ${data.city}, ${data.country}")
        Text( text = "Current temperature in Celsius : ${"%.1f".format(data.tempC)} °C")
        val sunriseMillis = data.sunrise * 1000
        val sunsetMillis = data.sunset * 1000
        Text("Time of Sunrise: ${timeFormatter.format(Date(sunriseMillis))}")
        Text("Time of Sunset: ${timeFormatter.format(Date(sunsetMillis))}")
        Text(text = "Current weather: ${data.main} : $iconText")
    }
}

@Composable
private fun HistoryTab(historyItems: List<WeatherHistoryItem>) {
    val timeFormatter = remember {
        DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault())
    }
    if (historyItems.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No weather history yet.")
        }
    } else {
        val dateTimeFormatter = remember {
            DateFormat.getDateTimeInstance(
                DateFormat.SHORT,
                DateFormat.SHORT,
                Locale.getDefault()
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(historyItems) { item ->

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {

                        Text(
                            text = "City and Country : ${item.city}, ${item.country}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Temperature in Celsius : ${"%.1f".format(item.tempC)} °C",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val sunriseMillis = item.sunrise * 1000
                        val sunsetMillis = item.sunset * 1000
                        Text("Time of Sunrise: ${timeFormatter.format(Date(sunriseMillis))}",style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Time of Sunset: ${timeFormatter.format(Date(sunsetMillis))}",style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Fetched: ${
                                dateTimeFormatter.format(
                                    Date(item.fetchedAt)
                                )
                            }",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

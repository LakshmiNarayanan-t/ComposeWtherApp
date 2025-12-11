package com.example.weatherapp.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import android.util.Log
import com.example.weatherapp.domain.location.LocationRepository
import com.example.weatherapp.domain.location.LocationResult
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class LocationRepositoryImpl @Inject constructor(
    private val fusedLocationClient: FusedLocationProviderClient,
    @ApplicationContext private val context: Context,
) : LocationRepository {

    private val locationManager: LocationManager by lazy {
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    override suspend fun getCurrentLocation(): LocationResult =
        suspendCancellableCoroutine { cont ->

            // 1) Check if any provider is enabled (GPS or Network)
            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetworkEnabled =
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            if (!isGpsEnabled && !isNetworkEnabled) {
                // Location services are OFF → let UI show "Open Location Settings"
                Log.w("LocationRepository", "Location services are OFF")
                cont.resume(LocationResult.ServiceOff)
                return@suspendCancellableCoroutine
            }

            @SuppressLint("MissingPermission")
            fun requestLocation() {
                try {
                    val cancellationTokenSource = CancellationTokenSource()

                    // Cancel underlying location request when coroutine is cancelled
                    cont.invokeOnCancellation {
                        cancellationTokenSource.cancel()
                    }

                    // 2) First try: actively request a fresh current location
                    fusedLocationClient
                        .getCurrentLocation(
                            Priority.PRIORITY_HIGH_ACCURACY,
                            cancellationTokenSource.token
                        )
                        .addOnSuccessListener { location ->
                            if (cont.isCompleted) return@addOnSuccessListener

                            if (location != null) {
                                Log.d(
                                    "LocationRepository",
                                    "getCurrentLocation success: ${location.latitude}, ${location.longitude}"
                                )
                                cont.resume(
                                    LocationResult.Success(
                                        lat = location.latitude,
                                        lon = location.longitude
                                    )
                                )
                            } else {
                                // 3) Fallback: use lastLocation if getCurrentLocation returned null
                                Log.w(
                                    "LocationRepository",
                                    "getCurrentLocation returned null, falling back to lastLocation"
                                )
                                fusedLocationClient
                                    .lastLocation
                                    .addOnSuccessListener { lastLoc ->
                                        if (cont.isCompleted) return@addOnSuccessListener

                                        if (lastLoc != null) {
                                            Log.d(
                                                "LocationRepository",
                                                "lastLocation success: ${lastLoc.latitude}, ${lastLoc.longitude}"
                                            )
                                            cont.resume(
                                                LocationResult.Success(
                                                    lat = lastLoc.latitude,
                                                    lon = lastLoc.longitude
                                                )
                                            )
                                        } else {
                                            Log.e(
                                                "LocationRepository",
                                                "Both getCurrentLocation and lastLocation returned null"
                                            )
                                            cont.resume(
                                                LocationResult.Error(
                                                    "Cant able to fetch your location please click fetch location again"
                                                )
                                            )
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        if (cont.isCompleted) return@addOnFailureListener
                                        Log.e(
                                            "LocationRepository",
                                            "Error getting lastLocation",
                                            e
                                        )
                                        cont.resume(
                                            LocationResult.Error(
                                                e.message
                                                    ?: "Cant able to fetch your location please click fetch location again"
                                            )
                                        )
                                    }
                            }
                        }
                        .addOnFailureListener { e ->
                            if (cont.isCompleted) return@addOnFailureListener
                            Log.e(
                                "LocationRepository",
                                "Error in getCurrentLocation",
                                e
                            )
                            cont.resume(
                                LocationResult.Error(
                                    e.message
                                        ?: "Cant able to fetch your location please click fetch location again"
                                )
                            )
                        }
                } catch (se: SecurityException) {
                    // Permission missing at the moment of call
                    if (!cont.isCompleted) {
                        Log.e("LocationRepository", "SecurityException getting location", se)
                        cont.resume(
                            LocationResult.Error(
                                "Location permission missing. Please allow location permission and try again."
                            )
                        )
                    }
                } catch (t: Throwable) {
                    if (!cont.isCompleted) {
                        Log.e("LocationRepository", "Unexpected error getting location", t)
                        cont.resume(
                            LocationResult.Error(
                                t.message
                                    ?: "Cant able to fetch your location please click fetch location again"
                            )
                        )
                    }
                }
            }

            // Kick off the request
            requestLocation()
        }
}

package com.example.weatherapp.data.location

import android.content.Context
import android.location.LocationManager
import com.example.weatherapp.domain.location.LocationRepository
import com.example.weatherapp.domain.location.LocationResult
import com.google.android.gms.location.FusedLocationProviderClient
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class LocationRepositoryImpl @Inject constructor(
    private val fusedClient: FusedLocationProviderClient,
    private val context: Context
) : LocationRepository {

    override suspend fun getCurrentLocation(): LocationResult {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val networkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!gpsEnabled && !networkEnabled) {
            return LocationResult.ServiceOff
        }

        return suspendCancellableCoroutine { cont ->
            fusedClient.lastLocation
                .addOnSuccessListener { loc ->
                    if (loc != null) {
                        cont.resume(LocationResult.Success(loc.latitude, loc.longitude))
                    } else {
                        cont.resume(
                            LocationResult.Error(
                                "Cant able to fetch your location please click fetch location again"
                            )
                        )
                    }
                }
                .addOnFailureListener { e ->
                    cont.resume(
                        LocationResult.Error(
                            e.localizedMessage
                                ?: "Cant able to fetch your location please click fetch location again"
                        )
                    )
                }
        }
    }
}
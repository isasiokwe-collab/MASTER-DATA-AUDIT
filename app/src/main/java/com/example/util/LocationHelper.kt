package com.example.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Location
import android.net.Uri
import android.os.Looper
import android.widget.Toast
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class GpsStatus(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val accuracy: Float = Float.MAX_VALUE,
    val isFetching: Boolean = false,
    val hasLocation: Boolean = false,
    val error: String? = null
) {
    val isAccurateEnough: Boolean
        get() = hasLocation && accuracy <= 25.0f
}

class LocationHelper(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val _gpsStatus = MutableStateFlow(GpsStatus())
    val gpsStatus: StateFlow<GpsStatus> = _gpsStatus.asStateFlow()

    private var locationCallback: LocationCallback? = null

    @SuppressLint("MissingPermission")
    fun startLocationUpdates(onUpdate: ((Double, Double, Float) -> Unit)? = null) {
        stopLocationUpdates()
        _gpsStatus.value = _gpsStatus.value.copy(isFetching = true, error = null)

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
            .setMinUpdateIntervalMillis(1000L)
            .setMinUpdateDistanceMeters(0f)
            .setWaitForAccurateLocation(true)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc: Location? = result.lastLocation
                if (loc != null) {
                    _gpsStatus.value = GpsStatus(
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        accuracy = loc.accuracy,
                        isFetching = false,
                        hasLocation = true,
                        error = null
                    )
                    onUpdate?.invoke(loc.latitude, loc.longitude, loc.accuracy)
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            ).addOnFailureListener { exc ->
                _gpsStatus.value = _gpsStatus.value.copy(
                    isFetching = false,
                    error = exc.localizedMessage ?: "Failed to get location"
                )
            }
        } catch (e: SecurityException) {
            _gpsStatus.value = _gpsStatus.value.copy(
                isFetching = false,
                error = "Location permission not granted"
            )
        }
    }

    fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
            locationCallback = null
        }
        _gpsStatus.value = _gpsStatus.value.copy(isFetching = false)
    }

    companion object {
        fun openInGoogleMaps(context: Context, latitude: Double, longitude: Double, label: String = "Outlet Location") {
            if (latitude == 0.0 && longitude == 0.0) {
                Toast.makeText(context, "No valid GPS coordinates available", Toast.LENGTH_SHORT).show()
                return
            }
            val uri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude($label)")
            val mapIntent = Intent(Intent.ACTION_VIEW, uri)
            mapIntent.setPackage("com.google.android.apps.maps")
            if (mapIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(mapIntent)
            } else {
                // Fallback to browser or any map handler
                val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$latitude,$longitude")
                val fallbackIntent = Intent(Intent.ACTION_VIEW, webUri)
                try {
                    context.startActivity(fallbackIntent)
                } catch (e: Exception) {
                    Toast.makeText(context, "Unable to open maps", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

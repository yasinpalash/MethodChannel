package com.example.methodchannel.handlers

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import io.flutter.plugin.common.EventChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class LocationHandler(private val context: Context) {
    private var locationManager: LocationManager? = null
    private var locationListener: LocationListener? = null
    private var locationEventSink: EventChannel.EventSink? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    fun startLocationUpdates(eventSink: EventChannel.EventSink?) {
        locationEventSink = eventSink

        try {
            locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

            // Create a non-nullable LocationListener
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    val latitude = location.latitude
                    val longitude = location.longitude

                    // Create initial location data without address
                    val locationData = mutableMapOf<String, Any>(
                        "latitude" to latitude,
                        "longitude" to longitude,
                        "altitude" to location.altitude,
                        "accuracy" to location.accuracy,
                        "speed" to location.speed,
                        "time" to location.time
                    )

                    // Send initial location data immediately
                    locationEventSink?.success(locationData)

                    // Get address asynchronously and send updated data when available
                    getAddressFromLocation(latitude, longitude) { address ->
                        if (address != null) {
                            val updatedData = HashMap<String, Any>(locationData)
                            updatedData["address"] = address
                            locationEventSink?.success(updatedData)
                        }
                    }
                }

                // Implement required methods for older Android versions
                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

                override fun onProviderEnabled(provider: String) {}

                override fun onProviderDisabled(provider: String) {
                    locationEventSink?.error("LOCATION_DISABLED", "Location provider disabled", null)
                }
            }

            // Save the listener for later removal
            locationListener = listener

            // Request location updates with the non-nullable listener
            locationManager?.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000, // 1 second
                1f,    // 1 meter
                listener
            )

            // Also try to get network provider for faster initial position
            try {
                locationManager?.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    1000,
                    1f,
                    listener
                )
            } catch (e: Exception) {
                Log.e("Location", "Network provider not available: ${e.message}")
            }

        } catch (e: SecurityException) {
            locationEventSink?.error("PERMISSION_DENIED", "Location permission denied", null)
        } catch (e: Exception) {
            locationEventSink?.error("LOCATION_ERROR", "Error starting location updates: ${e.message}", null)
        }
    }

    fun stopLocationUpdates() {
        locationListener?.let { listener ->
            locationManager?.removeUpdates(listener)
        }
        locationEventSink = null
    }

    private fun getAddressFromLocation(latitude: Double, longitude: Double, callback: (String?) -> Unit) {
        coroutineScope.launch {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())

                // Use different methods based on API level
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    // For Android 13+ (API 33+)
                    geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                        val address = if (addresses.isNotEmpty()) formatAddress(addresses[0]) else null
                        callback(address)
                    }
                } else {
                    // For older Android versions
                    withContext(Dispatchers.IO) {
                        val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                        val address = if (!addresses.isNullOrEmpty()) formatAddress(addresses[0]) else null
                        withContext(Dispatchers.Main) {
                            callback(address)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("Geocoding", "Error getting address: ${e.message}")
                callback(null)
            }
        }
    }

    private fun formatAddress(address: Address): String {
        val addressParts = mutableListOf<String>()

        // Add the most specific information first
        if (address.thoroughfare != null) {
            addressParts.add(address.thoroughfare)
        }

        if (address.subLocality != null) {
            addressParts.add(address.subLocality)
        }

        if (address.locality != null) {
            addressParts.add(address.locality)
        }

        if (address.adminArea != null) {
            addressParts.add(address.adminArea)
        }

        if (address.countryName != null) {
            addressParts.add(address.countryName)
        }

        return addressParts.joinToString(", ")
    }
}

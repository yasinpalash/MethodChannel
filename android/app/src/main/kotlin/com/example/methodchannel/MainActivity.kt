package com.example.methodchannel

import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.content.Context
import android.content.IntentFilter
import android.content.BroadcastReceiver
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.provider.MediaStore
import android.hardware.camera2.CameraManager
import android.util.Log
import androidx.annotation.NonNull
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity: FlutterActivity() {
    private val METHOD_CHANNEL = "native_bridge/method"
    private val ACCELEROMETER_EVENT_CHANNEL = "native_bridge/event"
    private val LOCATION_EVENT_CHANNEL = "native_bridge/location_event"
    private val CHARGING_EVENT_CHANNEL = "native_bridge/charging_event"

    private var sensorManager: SensorManager? = null
    private var accelerometerListener: SensorEventListener? = null
    private var locationManager: LocationManager? = null
    private var locationListener: LocationListener? = null
    private var batteryReceiver: BroadcastReceiver? = null

    private var accelerometerEventSink: EventChannel.EventSink? = null
    private var locationEventSink: EventChannel.EventSink? = null
    private var chargingEventSink: EventChannel.EventSink? = null

    private var isFlashlightOn = false
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun configureFlutterEngine(@NonNull flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, METHOD_CHANNEL).setMethodCallHandler {
                call, result ->
            when (call.method) {
                "getBatteryLevel" -> {
                    val batteryLevel = getBatteryLevel()
                    result.success(batteryLevel)
                }
                "getNetworkType" -> {
                    val networkType = getNetworkType()
                    result.success(networkType)
                }
                "openCamera" -> {
                    val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    if (intent.resolveActivity(packageManager) != null) {
                        startActivity(intent)
                        result.success("Camera opened")
                    } else {
                        result.error("UNAVAILABLE", "Camera not available", null)
                    }
                }
                "openGallery" -> {
                    val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    intent.type = "image/*"
                    if (intent.resolveActivity(packageManager) != null) {
                        startActivity(intent)
                        result.success("Gallery opened")
                    } else {
                        result.error("UNAVAILABLE", "Gallery not available", null)
                    }
                }
                "toggleFlashlight" -> {
                    val success = toggleFlashlight()
                    if (success) {
                        result.success("Flashlight toggled")
                    } else {
                        result.error("FLASH_ERROR", "Flashlight not available", null)
                    }
                }
                else -> result.notImplemented()
            }
        }

        // Accelerometer Event Channel
        EventChannel(flutterEngine.dartExecutor.binaryMessenger, ACCELEROMETER_EVENT_CHANNEL).setStreamHandler(
            object : EventChannel.StreamHandler {
                override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
                    accelerometerEventSink = events
                    startAccelerometer()
                }

                override fun onCancel(arguments: Any?) {
                    stopAccelerometer()
                }
            }
        )

        // Location Event Channel
        EventChannel(flutterEngine.dartExecutor.binaryMessenger, LOCATION_EVENT_CHANNEL).setStreamHandler(
            object : EventChannel.StreamHandler {
                override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
                    locationEventSink = events
                    startLocationUpdates()
                }

                override fun onCancel(arguments: Any?) {
                    stopLocationUpdates()
                }
            }
        )

        // Charging Status Event Channel
        EventChannel(flutterEngine.dartExecutor.binaryMessenger, CHARGING_EVENT_CHANNEL).setStreamHandler(
            object : EventChannel.StreamHandler {
                override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
                    chargingEventSink = events
                    startChargingMonitor()
                }

                override fun onCancel(arguments: Any?) {
                    stopChargingMonitor()
                }
            }
        )
    }

    private fun getBatteryLevel(): Int {
        val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    private fun getNetworkType(): String {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return "No internet"
        val capabilities = cm.getNetworkCapabilities(network) ?: return "Unknown"
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
            else -> "Other"
        }
    }

    private fun startAccelerometer() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        accelerometerListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                val x = event?.values?.get(0) ?: 0f
                val y = event?.values?.get(1) ?: 0f
                val z = event?.values?.get(2) ?: 0f
                val map = mapOf("x" to x, "y" to y, "z" to z)
                accelerometerEventSink?.success(map)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager?.registerListener(accelerometerListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
    }

    private fun stopAccelerometer() {
        sensorManager?.unregisterListener(accelerometerListener)
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

    private fun startLocationUpdates() {
        try {
            locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

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

    private fun stopLocationUpdates() {
        locationListener?.let { listener ->
            locationManager?.removeUpdates(listener)
        }
    }

    private fun startChargingMonitor() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
        }

        batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    Intent.ACTION_POWER_CONNECTED -> {
                        chargingEventSink?.success(mapOf("status" to "connected"))
                    }
                    Intent.ACTION_POWER_DISCONNECTED -> {
                        chargingEventSink?.success(mapOf("status" to "disconnected"))
                    }
                    Intent.ACTION_BATTERY_CHANGED -> {
                        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                                status == BatteryManager.BATTERY_STATUS_FULL

                        val chargePlug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
                        val usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB
                        val acCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC
                        val wirelessCharge = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            chargePlug == BatteryManager.BATTERY_PLUGGED_WIRELESS
                        } else {
                            false
                        }

                        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                        val batteryPct = level * 100 / scale.toFloat()

                        val data = mapOf(
                            "status" to if (isCharging) "charging" else "discharging",
                            "level" to batteryPct,
                            "source" to when {
                                usbCharge -> "usb"
                                acCharge -> "ac"
                                wirelessCharge -> "wireless"
                                else -> "unknown"
                            }
                        )

                        chargingEventSink?.success(data)
                    }
                }
            }
        }

        context.registerReceiver(batteryReceiver, filter)

        // Send initial battery state
        val batteryStatus = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        if (batteryStatus != null) {
            val status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL

            val chargePlug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
            val usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB
            val acCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC

            val level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val batteryPct = level * 100 / scale.toFloat()

            val data = mapOf(
                "status" to if (isCharging) "charging" else "discharging",
                "level" to batteryPct,
                "source" to when {
                    usbCharge -> "usb"
                    acCharge -> "ac"
                    else -> "unknown"
                }
            )

            chargingEventSink?.success(data)
        }
    }

    private fun stopChargingMonitor() {
        batteryReceiver?.let {
            context.unregisterReceiver(it)
            batteryReceiver = null
        }
    }

    private fun toggleFlashlight(): Boolean {
        return try {
            val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }

            if (cameraId != null) {
                isFlashlightOn = !isFlashlightOn
                cameraManager.setTorchMode(cameraId, isFlashlightOn)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("Flashlight", "Error toggling flashlight: ${e.message}")
            false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAccelerometer()
        stopLocationUpdates()
        stopChargingMonitor()
    }
}

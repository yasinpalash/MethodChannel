package com.example.methodchannel

import android.content.Context
import android.os.Bundle
import androidx.annotation.NonNull
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodChannel
import com.example.methodchannel.handlers.*

class MainActivity: FlutterActivity() {
    // Channel names
    private val METHOD_CHANNEL = "native_bridge/method"
    private val ACCELEROMETER_EVENT_CHANNEL = "native_bridge/event"
    private val LOCATION_EVENT_CHANNEL = "native_bridge/location_event"
    private val CHARGING_EVENT_CHANNEL = "native_bridge/charging_event"

    // Feature handlers
    private lateinit var deviceHandler: DeviceHandler
    private lateinit var sensorHandler: SensorHandler
    private lateinit var locationHandler: LocationHandler
    private lateinit var batteryHandler: BatteryHandler
    private lateinit var mediaHandler: MediaHandler

    override fun configureFlutterEngine(@NonNull flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        // Initialize handlers
        deviceHandler = DeviceHandler(context)
        sensorHandler = SensorHandler(context)
        locationHandler = LocationHandler(context)
        batteryHandler = BatteryHandler(context)
        mediaHandler = MediaHandler(context)

        // Set up method channel
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, METHOD_CHANNEL).setMethodCallHandler {
                call, result ->
            when (call.method) {
                "getBatteryLevel" -> {
                    val batteryLevel = batteryHandler.getBatteryLevel()
                    result.success(batteryLevel)
                }
                "getNetworkType" -> {
                    val networkType = deviceHandler.getNetworkType()
                    result.success(networkType)
                }
                "openCamera" -> {
                    val success = mediaHandler.openCamera()
                    if (success) {
                        result.success("Camera opened")
                    } else {
                        result.error("UNAVAILABLE", "Camera not available", null)
                    }
                }
                "openGallery" -> {
                    val success = mediaHandler.openGallery()
                    if (success) {
                        result.success("Gallery opened")
                    } else {
                        result.error("UNAVAILABLE", "Gallery not available", null)
                    }
                }
                "toggleFlashlight" -> {
                    val success = mediaHandler.toggleFlashlight()
                    if (success) {
                        result.success("Flashlight toggled")
                    } else {
                        result.error("FLASH_ERROR", "Flashlight not available", null)
                    }
                }
                else -> result.notImplemented()
            }
        }

        // Set up event channels
        setupAccelerometerEventChannel(flutterEngine)
        setupLocationEventChannel(flutterEngine)
        setupChargingEventChannel(flutterEngine)
    }

    private fun setupAccelerometerEventChannel(flutterEngine: FlutterEngine) {
        EventChannel(flutterEngine.dartExecutor.binaryMessenger, ACCELEROMETER_EVENT_CHANNEL).setStreamHandler(
            object : EventChannel.StreamHandler {
                override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
                    sensorHandler.startAccelerometer(events)
                }

                override fun onCancel(arguments: Any?) {
                    sensorHandler.stopAccelerometer()
                }
            }
        )
    }

    private fun setupLocationEventChannel(flutterEngine: FlutterEngine) {
        EventChannel(flutterEngine.dartExecutor.binaryMessenger, LOCATION_EVENT_CHANNEL).setStreamHandler(
            object : EventChannel.StreamHandler {
                override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
                    locationHandler.startLocationUpdates(events)
                }

                override fun onCancel(arguments: Any?) {
                    locationHandler.stopLocationUpdates()
                }
            }
        )
    }

    private fun setupChargingEventChannel(flutterEngine: FlutterEngine) {
        EventChannel(flutterEngine.dartExecutor.binaryMessenger, CHARGING_EVENT_CHANNEL).setStreamHandler(
            object : EventChannel.StreamHandler {
                override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
                    batteryHandler.startChargingMonitor(events)
                }

                override fun onCancel(arguments: Any?) {
                    batteryHandler.stopChargingMonitor()
                }
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorHandler.stopAccelerometer()
        locationHandler.stopLocationUpdates()
        batteryHandler.stopChargingMonitor()
    }
}

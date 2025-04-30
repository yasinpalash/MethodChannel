package com.example.methodchannel.handlers

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import io.flutter.plugin.common.EventChannel

class SensorHandler(private val context: Context) {
    private var sensorManager: SensorManager? = null
    private var accelerometerListener: SensorEventListener? = null
    private var accelerometerEventSink: EventChannel.EventSink? = null

    fun startAccelerometer(eventSink: EventChannel.EventSink?) {
        accelerometerEventSink = eventSink
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
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

    fun stopAccelerometer() {
        sensorManager?.unregisterListener(accelerometerListener)
        accelerometerEventSink = null
    }
}

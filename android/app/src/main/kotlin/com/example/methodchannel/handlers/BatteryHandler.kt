package com.example.methodchannel.handlers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import io.flutter.plugin.common.EventChannel

class BatteryHandler(private val context: Context) {
    private var batteryReceiver: BroadcastReceiver? = null
    private var chargingEventSink: EventChannel.EventSink? = null

    fun getBatteryLevel(): Int {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    fun startChargingMonitor(eventSink: EventChannel.EventSink?) {
        chargingEventSink = eventSink
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
        sendInitialBatteryState()
    }

    private fun sendInitialBatteryState() {
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

    fun stopChargingMonitor() {
        batteryReceiver?.let {
            context.unregisterReceiver(it)
            batteryReceiver = null
        }
        chargingEventSink = null
    }
}

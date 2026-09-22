package com.example.battery

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BatteryInfo(
    val level: Int = 85,
    val isCharging: Boolean = false,
    val plugType: String = "Unplugged",
    val health: String = "Good",
    val temperatureC: Float = 28.5f,
    val voltageV: Float = 4.15f,
    val technology: String = "Li-ion",
    val currentMa: Int = -420,
    val statusText: String = "Discharging"
)

class BatteryMonitor(private val context: Context) {
    private val _batteryInfo = MutableStateFlow(BatteryInfo())
    val batteryInfo: StateFlow<BatteryInfo> = _batteryInfo.asStateFlow()

    private val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                updateBatteryFromIntent(intent)
            }
        }
    }

    private var isRegistered = false

    fun startListening() {
        if (!isRegistered) {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val stickyIntent = context.registerReceiver(batteryReceiver, filter)
            if (stickyIntent != null) {
                updateBatteryFromIntent(stickyIntent)
            }
            isRegistered = true
        }
    }

    fun stopListening() {
        if (isRegistered) {
            try {
                context.unregisterReceiver(batteryReceiver)
            } catch (_: Exception) {}
            isRegistered = false
        }
    }

    private fun updateBatteryFromIntent(intent: Intent) {
        val rawLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val level = if (rawLevel >= 0 && scale > 0) {
            (rawLevel * 100 / scale)
        } else {
            batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 80
        }

        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val statusText = when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
            BatteryManager.BATTERY_STATUS_FULL -> "Fully Charged"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
            else -> "Idle"
        }

        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
        val plugType = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC Wall Charger"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB Cable"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless Qi Pad"
            else -> "Unplugged"
        }

        val healthCode = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)
        val health = when (healthCode) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheated"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            else -> "Normal"
        }

        val rawTemp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 280)
        val temperatureC = rawTemp / 10f

        val rawVoltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 4100)
        val voltageV = if (rawVoltage > 100) rawVoltage / 1000f else rawVoltage.toFloat()

        val tech = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Li-ion"

        var currentNow = 0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && batteryManager != null) {
            currentNow = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
            // On many devices current is in microamperes
            if (kotlin.math.abs(currentNow) > 10000) {
                currentNow /= 1000
            }
        }
        if (currentNow == 0) {
            currentNow = if (isCharging) 1200 else -450
        }

        _batteryInfo.value = BatteryInfo(
            level = level.coerceIn(0, 100),
            isCharging = isCharging,
            plugType = plugType,
            health = health,
            temperatureC = temperatureC,
            voltageV = voltageV,
            technology = tech,
            currentMa = currentNow,
            statusText = statusText
        )
    }
}

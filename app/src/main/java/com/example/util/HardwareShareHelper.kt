package com.example.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast

object HardwareShareHelper {

    fun openSystemBatteryShareSettings(context: Context): Boolean {
        // Attempt OEM specific Battery Share / Reverse Wireless Charging intents
        val intents = listOf(
            // Google Pixel Battery Share
            Intent().setComponent(
                ComponentName(
                    "com.android.settings",
                    "com.android.settings.fuelgauge.batteryshare.BatteryShareActivity"
                )
            ),
            // Generic Reverse Charging intent (AOSP / Nothing / Motorola)
            Intent("android.settings.REVERSE_CHARGING_SETTINGS"),
            // Samsung Wireless PowerShare
            Intent().setComponent(
                ComponentName(
                    "com.android.settings",
                    "com.samsung.android.settings.powershare.PowerShareEnablerActivity"
                )
            ),
            Intent("com.samsung.android.settings.powershare.POWER_SHARE_SETTINGS"),
            // Xiaomi / MIUI Reverse Charging
            Intent().setComponent(
                ComponentName(
                    "com.android.settings",
                    "com.android.settings.Settings\$WirelessChargingSettingsActivity"
                )
            ),
            // Standard Battery Saver / Battery Settings fallback
            Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS),
            Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
        )

        for (intent in intents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                    return true
                }
            } catch (_: Exception) {}
        }

        // Final fallback: General settings
        return try {
            val fallback = Intent(Settings.ACTION_SETTINGS)
            fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(fallback)
            true
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open system settings", Toast.LENGTH_SHORT).show()
            false
        }
    }
}

package com.example.data

data class BatteryHistoryPoint(
    val timestampMs: Long,
    val level: Int,
    val isCharging: Boolean = false,
    val eventLabel: String? = null
)

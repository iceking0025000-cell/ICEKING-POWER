package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transfer_logs")
data class TransferLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val partnerName: String,
    val role: String, // "DONOR" or "RECIPIENT"
    val mode: String, // "WIRELESS" or "USB_C"
    val startPercent: Int,
    val endPercent: Int,
    val transferredPercent: Int,
    val durationSeconds: Long,
    val completedSuccessfully: Boolean
)

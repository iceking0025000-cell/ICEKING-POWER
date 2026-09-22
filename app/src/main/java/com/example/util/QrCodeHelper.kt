package com.example.util

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.net.Uri
import com.example.network.PeerDevice
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object QrCodeHelper {

    fun generateQrBitmap(
        content: String,
        sizePx: Int = 400,
        foregroundColor: Int = 0xFF00E5FF.toInt(), // IceCyan
        backgroundColor: Int = 0xFF0A1420.toInt()  // IceDark
    ): Bitmap? {
        return try {
            val hints = mapOf(
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                EncodeHintType.MARGIN to 1
            )
            val bitMatrix = QRCodeWriter().encode(
                content,
                BarcodeFormat.QR_CODE,
                sizePx,
                sizePx,
                hints
            )

            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height)

            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (bitMatrix[x, y]) foregroundColor else backgroundColor
                }
            }

            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
                setPixels(pixels, 0, width, 0, 0, width, height)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun buildPairingUri(
        deviceId: String,
        deviceName: String,
        pairingPin: String,
        ipAddress: String,
        batteryLevel: Int,
        role: String
    ): String {
        val encodedName = URLEncoder.encode(deviceName, StandardCharsets.UTF_8.toString())
        return "icepower://pair?id=$deviceId&name=$encodedName&pin=$pairingPin&ip=$ipAddress&level=$batteryLevel&role=$role"
    }

    fun parsePairingUri(raw: String): PeerDevice? {
        return try {
            val trimmed = raw.trim()
            if (!trimmed.startsWith("icepower://pair")) {
                // If the QR is just a 4-digit code or JSON fallback
                return null
            }
            val uri = Uri.parse(trimmed)
            val id = uri.getQueryParameter("id") ?: return null
            val nameRaw = uri.getQueryParameter("name") ?: "ICEPOWER Device"
            val name = try {
                URLDecoder.decode(nameRaw, StandardCharsets.UTF_8.toString())
            } catch (_: Exception) {
                nameRaw
            }
            val pin = uri.getQueryParameter("pin") ?: ""
            val ip = uri.getQueryParameter("ip") ?: ""
            val level = uri.getQueryParameter("level")?.toIntOrNull() ?: 50
            val role = uri.getQueryParameter("role") ?: "DONOR"

            PeerDevice(
                id = id,
                name = name,
                batteryLevel = level,
                isCharging = false,
                role = role,
                isTransferring = false,
                targetPercent = 10,
                currentTransferred = 0,
                lastSeenMs = System.currentTimeMillis(),
                ipAddress = ip,
                pairingPin = pin,
                isAuthorized = false
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

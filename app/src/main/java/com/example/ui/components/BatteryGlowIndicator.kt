package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.IceAmber
import com.example.ui.theme.IceBlue
import com.example.ui.theme.IceCyan
import com.example.ui.theme.IceGreen
import com.example.ui.theme.IceRed

@Composable
fun BatteryGlowIndicator(
    deviceName: String,
    level: Int,
    isCharging: Boolean,
    role: String,
    isTransferring: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 130.dp,
    subtitle: String? = null,
    isLocal: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val ringColor = when {
        level <= 15 -> IceRed
        level <= 30 -> IceAmber
        isTransferring -> IceCyan
        isCharging -> IceGreen
        else -> IceBlue
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Device Role & Name Badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (role == "DONOR") Color(0xFF3B1E05).copy(alpha = 0.6f)
                    else Color(0xFF003847).copy(alpha = 0.6f)
                )
                .border(
                    width = 1.dp,
                    color = if (role == "DONOR") IceAmber.copy(alpha = 0.5f) else IceCyan.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Icon(
                imageVector = if (role == "DONOR") Icons.Default.Bolt else Icons.Default.PhoneAndroid,
                contentDescription = null,
                tint = if (role == "DONOR") IceAmber else IceCyan,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = if (role == "DONOR") "SHARE" else "RECEIVE",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (role == "DONOR") IceAmber else IceCyan,
                letterSpacing = 1.sp
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Circular Gauge
        Box(
            modifier = Modifier.size(size),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 8.dp.toPx()
                val radius = (size.toPx() - strokeWidth) / 2
                val center = Offset(size.toPx() / 2, size.toPx() / 2)

                // Background track
                drawCircle(
                    color = Color(0xFF1E293B),
                    radius = radius,
                    center = center,
                    style = Stroke(width = strokeWidth)
                )

                // Outer subtle glow when transferring
                if (isTransferring) {
                    drawCircle(
                        color = ringColor.copy(alpha = pulseAlpha * 0.25f),
                        radius = radius + 6.dp.toPx(),
                        center = center,
                        style = Stroke(width = 3.dp.toPx())
                    )
                }

                // Progress arc
                val sweepAngle = (level.coerceIn(0, 100) / 100f) * 360f
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(
                            ringColor.copy(alpha = 0.7f),
                            ringColor,
                            ringColor
                        )
                    ),
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                    size = Size(size.toPx() - strokeWidth, size.toPx() - strokeWidth),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // Percentage Text inside
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isCharging || isTransferring) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "Charging",
                            tint = ringColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "$level%",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = if (isLocal) "This Device" else "Partner",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Device Name
        Text(
            text = deviceName,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )

        if (subtitle != null) {
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

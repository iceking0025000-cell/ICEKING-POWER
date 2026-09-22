package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.IceCyan
import com.example.ui.theme.IceGreen
import com.example.ui.theme.IceTeal

@Composable
fun EnergyTransferBeam(
    isTransferring: Boolean,
    flowFromLeftToRight: Boolean, // True if Local (left) is Donor to Partner (right)
    rateMa: Int,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "energy_flow")
    val flowPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "flowPhase"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Live rate badge when transferring
        if (isTransferring) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF003D4D))
                    .border(1.dp, IceCyan.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = IceCyan,
                        modifier = Modifier.size(10.dp)
                    )
                    Text(
                        text = "${rateMa}mA",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = IceCyan
                    )
                }
            }
        }

        // Energy Flow Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().height(36.dp)) {
                val y = size.height / 2
                val startX = 10.dp.toPx()
                val endX = size.width - 10.dp.toPx()

                if (!isTransferring) {
                    // Static subtle dashed link
                    drawLine(
                        color = Color(0xFF334155),
                        start = Offset(startX, y),
                        end = Offset(endX, y),
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                    )
                } else {
                    // Animated Electric Flow Line
                    drawLine(
                        color = IceCyan.copy(alpha = 0.35f),
                        start = Offset(startX, y),
                        end = Offset(endX, y),
                        strokeWidth = 3.dp.toPx()
                    )

                    // Draw 3 moving energy particles along the line
                    val directionSign = if (flowFromLeftToRight) 1f else -1f
                    val baseStart = if (flowFromLeftToRight) startX else endX
                    val span = (endX - startX) * directionSign

                    for (i in 0..2) {
                        val particlePhase = (flowPhase + i * 0.33f) % 1f
                        val particleX = baseStart + span * particlePhase
                        drawCircle(
                            color = IceCyan,
                            radius = 4.dp.toPx(),
                            center = Offset(particleX, y)
                        )
                        // Outer particle glow
                        drawCircle(
                            color = IceCyan.copy(alpha = 0.4f),
                            radius = 8.dp.toPx(),
                            center = Offset(particleX, y)
                        )
                    }
                }
            }

            // Direction arrow in middle
            if (isTransferring) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0F172A))
                        .border(1.dp, IceCyan, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = IceCyan,
                        modifier = Modifier
                            .size(14.dp)
                            .rotate(if (flowFromLeftToRight) 0f else 180f)
                    )
                }
            }
        }

        Text(
            text = if (isTransferring) "TRANSFERRING" else "PAIRED",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = if (isTransferring) IceCyan else Color(0xFF64748B),
            letterSpacing = 1.sp
        )
    }
}

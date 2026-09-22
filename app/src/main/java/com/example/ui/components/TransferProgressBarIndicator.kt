package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.TransferStatus
import com.example.ui.TransferUiState
import com.example.ui.theme.IceAmber
import com.example.ui.theme.IceBlue
import com.example.ui.theme.IceCyan
import com.example.ui.theme.IceGreen
import com.example.ui.theme.IceTeal
import kotlin.math.roundToInt

@Composable
fun TransferProgressBarIndicator(
    state: TransferUiState,
    modifier: Modifier = Modifier,
    barHeight: Dp = 14.dp,
    showDetailedSyncTelemetry: Boolean = true
) {
    val target = state.targetPercent.coerceAtLeast(1)
    val rawProgress = (state.transferredPercent.toFloat() / target.toFloat()).coerceIn(0f, 1f)

    // Smooth progress animation
    val animatedProgress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "animatedTransferProgress"
    )

    val isTransferring = state.status == TransferStatus.TRANSFERRING
    val isCompleted = state.status == TransferStatus.COMPLETED
    val isPaused = state.status == TransferStatus.PAUSED

    // Rotating sync icon animation during active sync
    val infiniteTransition = rememberInfiniteTransition(label = "syncPulse")
    val syncRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "syncRotation"
    )

    // Shimmer wave across the progress bar
    val shimmerTranslate by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progressShimmer"
    )

    // Beacon pulse opacity
    val beaconAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beaconAlpha"
    )

    val primaryColor by animateColorAsState(
        targetValue = when (state.status) {
            TransferStatus.COMPLETED -> IceGreen
            TransferStatus.PAUSED -> IceAmber
            TransferStatus.TRANSFERRING -> IceCyan
            TransferStatus.IDLE -> IceTeal
        },
        label = "primaryColor"
    )

    val percentInt = (animatedProgress * 100).roundToInt()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("transfer_progress_bar_indicator")
    ) {
        // Top Header: Label, Progress Ratio, and Percentage Pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (isTransferring) primaryColor.copy(alpha = beaconAlpha)
                            else primaryColor
                        )
                )

                Text(
                    text = when (state.status) {
                        TransferStatus.TRANSFERRING -> "BATTERY TRANSFER ACTIVE"
                        TransferStatus.PAUSED -> "TRANSFER PAUSED"
                        TransferStatus.COMPLETED -> "TRANSFER COMPLETE"
                        TransferStatus.IDLE -> "TRANSFER READY"
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    color = primaryColor
                )
            }

            // Ratio & Percentage Badges
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${state.transferredPercent}% / ${state.targetPercent}%",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(primaryColor.copy(alpha = 0.18f))
                        .border(1.dp, primaryColor.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "$percentInt%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = primaryColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Visual Custom Progress Bar Track with Glow and Milestones
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .clip(RoundedCornerShape(barHeight / 2))
                .background(Color(0xFF0D1826))
                .border(1.dp, Color(0xFF1E324A), RoundedCornerShape(barHeight / 2))
                .testTag("visual_progress_bar_track")
        ) {
            val totalWidthPx = constraints.maxWidth.toFloat()
            val fillWidthFraction = animatedProgress.coerceIn(0f, 1f)

            // 1. Glowing Active Progress Fill
            if (fillWidthFraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fillWidthFraction)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(barHeight / 2))
                        .background(
                            Brush.horizontalGradient(
                                colors = when (state.status) {
                                    TransferStatus.COMPLETED -> listOf(Color(0xFF059669), IceGreen)
                                    TransferStatus.PAUSED -> listOf(Color(0xFFD97706), IceAmber)
                                    else -> listOf(IceTeal, IceCyan, IceBlue)
                                }
                            )
                        )
                )
            }

            // 2. Animated Moving Shimmer Wave across active progress
            if (isTransferring && fillWidthFraction > 0.05f) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth(fillWidthFraction)
                        .fillMaxHeight()
                ) {
                    val w = size.width
                    val h = size.height
                    val shimmerX = shimmerTranslate * (w + 120f) - 60f

                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.45f),
                                Color.Transparent
                            ),
                            startX = shimmerX - 40f,
                            endX = shimmerX + 40f
                        ),
                        size = size
                    )
                }
            }

            // 3. Milestone Markers at 25%, 50%, 75%
            Canvas(modifier = Modifier.fillMaxSize()) {
                listOf(0.25f, 0.50f, 0.75f).forEach { milestone ->
                    val x = size.width * milestone
                    val reached = animatedProgress >= milestone
                    drawLine(
                        color = if (reached) Color.White.copy(alpha = 0.75f) else Color(0xFF334155),
                        start = Offset(x, 2.dp.toPx()),
                        end = Offset(x, size.height - 2.dp.toPx()),
                        strokeWidth = 1.5.dp.toPx()
                    )
                }
            }
        }

        // Detailed Data Synchronization Telemetry Bar
        if (showDetailedSyncTelemetry) {
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF09131F))
                    .border(1.dp, Color(0xFF162538), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 7.dp)
                    .testTag("sync_telemetry_indicator"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Sync Beacon & Network Feedback
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.Sync,
                        contentDescription = "Sync Indicator",
                        tint = primaryColor,
                        modifier = Modifier
                            .size(14.dp)
                            .then(if (isTransferring) Modifier.rotate(syncRotation) else Modifier)
                    )

                    Text(
                        text = when (state.status) {
                            TransferStatus.TRANSFERRING -> "P2P Stream • ${state.syncLatencyMs}ms latency"
                            TransferStatus.PAUSED -> "Sync Standby • Paused"
                            TransferStatus.COMPLETED -> "Handshake Finalized"
                            TransferStatus.IDLE -> if (state.connectedPeer != null) "P2P Link Ready" else "Awaiting Peer"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }

                // Right: Transfer Throughput / Rate / ETA
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isTransferring || isPaused) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = IceTeal,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = "${state.currentRateMa} mA",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = IceTeal
                            )
                        }

                        // ETA
                        val etaMin = state.estimatedRemainingSeconds / 60
                        val etaSec = state.estimatedRemainingSeconds % 60
                        val etaStr = String.format("%02d:%02d", etaMin, etaSec)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = IceCyan,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = "ETA $etaStr",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = IceCyan
                            )
                        }
                    } else if (isCompleted) {
                        Text(
                            text = "Target Verified",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = IceGreen
                        )
                    } else {
                        Text(
                            text = "${state.syncPacketsCount} pkts synced",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

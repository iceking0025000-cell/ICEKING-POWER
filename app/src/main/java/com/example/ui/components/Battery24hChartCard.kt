package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BatteryHistoryPoint
import com.example.ui.theme.IceAmber
import com.example.ui.theme.IceBlue
import com.example.ui.theme.IceCardDark
import com.example.ui.theme.IceCardStroke
import com.example.ui.theme.IceCyan
import com.example.ui.theme.IceGreen
import com.example.ui.theme.IceRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun Battery24hChartCard(
    history: List<BatteryHistoryPoint>,
    currentLevel: Int,
    safetyFloorPercent: Int,
    targetPercent: Int,
    role: String,
    modifier: Modifier = Modifier
) {
    if (history.isEmpty()) return

    // Metrics calculation
    val levels = history.map { it.level }
    val maxLevel = levels.maxOrNull() ?: currentLevel
    val minLevel = levels.minOrNull() ?: currentLevel
    val startLevel = history.first().level
    val netChange = currentLevel - startLevel

    // Pulsing animation for the current live point
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 5f,
        targetValue = 11f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseRadius"
    )

    // Touch inspection state (scrubber)
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("battery_24h_chart_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = IceCardDark),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(IceCardStroke)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0C2433)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShowChart,
                            contentDescription = null,
                            tint = IceCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "24-Hour Battery Timeline",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Pre-Transfer Context & Health",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = IceBlue
                        )
                    }
                }

                // Live status tag
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0F1E2C))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(IceGreen)
                    )
                    Text(
                        text = "LIVE $currentLevel%",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        fontWeight = FontWeight.Bold,
                        color = IceGreen,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Key Metrics Pill Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricChip(
                    label = "24H PEAK",
                    value = "$maxLevel%",
                    icon = Icons.Default.ArrowUpward,
                    accentColor = IceGreen,
                    modifier = Modifier.weight(1f)
                )
                MetricChip(
                    label = "24H LOW",
                    value = "$minLevel%",
                    icon = Icons.Default.ArrowDownward,
                    accentColor = if (minLevel <= 25) IceAmber else IceBlue,
                    modifier = Modifier.weight(1f)
                )
                MetricChip(
                    label = "NET 24H",
                    value = "${if (netChange >= 0) "+" else ""}$netChange%",
                    icon = if (netChange >= 0) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    accentColor = if (netChange >= 0) IceGreen else Color(0xFFEF4444),
                    modifier = Modifier.weight(1f)
                )
                MetricChip(
                    label = "SAFE ROOM",
                    value = "+${(currentLevel - safetyFloorPercent).coerceAtLeast(0)}%",
                    icon = Icons.Default.Bolt,
                    accentColor = IceCyan,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Interactive Scrubber Tooltip (if dragging/touching)
            val inspectedPoint = selectedIndex?.let { history.getOrNull(it) }
            AnimatedVisibility(
                visible = inspectedPoint != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                inspectedPoint?.let { pt ->
                    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                    val timeStr = timeFormat.format(Date(pt.timestampMs))
                    val hoursAgo = ((System.currentTimeMillis() - pt.timestampMs) / 3600000.0).roundToInt()
                    val timeAgoStr = if (hoursAgo <= 0) "Just Now" else "${hoursAgo}h ago ($timeStr)"

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF081420))
                            .border(1.dp, IceCyan.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (pt.isCharging) IceGreen else IceCyan)
                            )
                            Text(
                                text = timeAgoStr,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            if (pt.eventLabel != null) {
                                Text(
                                    text = "• ${pt.eventLabel}",
                                    fontSize = 11.sp,
                                    color = IceBlue
                                )
                            }
                        }

                        Text(
                            text = "${pt.level}%",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = IceCyan
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Canvas Chart Area with Y-axis markers
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
            ) {
                // Background grid with percentage labels
                Column(
                    modifier = Modifier.matchParentSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("100%", "75%", "50%", "25%", "0%").forEach { label ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                fontSize = 9.sp,
                                color = Color.White.copy(alpha = 0.3f),
                                modifier = Modifier.width(30.dp),
                                fontFamily = FontFamily.Monospace
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(1.dp)
                                    .background(Color.White.copy(alpha = 0.07f))
                            )
                        }
                    }
                }

                // Jetpack Compose Canvas Line Chart
                Canvas(
                    modifier = Modifier
                        .matchParentSize()
                        .padding(start = 32.dp, end = 12.dp, top = 6.dp, bottom = 6.dp)
                        .testTag("battery_timeline_canvas")
                        .pointerInput(history) {
                            detectTapGestures(
                                onPress = { offset ->
                                    val count = history.size
                                    if (count > 1) {
                                        val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                                        selectedIndex = (fraction * (count - 1)).roundToInt().coerceIn(0, count - 1)
                                    }
                                }
                            )
                        }
                        .pointerInput(history) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val count = history.size
                                    if (count > 1) {
                                        val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                                        selectedIndex = (fraction * (count - 1)).roundToInt().coerceIn(0, count - 1)
                                    }
                                },
                                onDrag = { change, _ ->
                                    val count = history.size
                                    if (count > 1) {
                                        val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                                        selectedIndex = (fraction * (count - 1)).roundToInt().coerceIn(0, count - 1)
                                    }
                                },
                                onDragEnd = {
                                    // keep inspection visible
                                }
                            )
                        }
                ) {
                    val w = size.width
                    val h = size.height

                    val pointsCount = history.size
                    if (pointsCount < 2) return@Canvas

                    // Calculate Coordinates for each history point
                    val coords = history.mapIndexed { index, pt ->
                        val x = (index.toFloat() / (pointsCount - 1)) * w
                        val y = h - ((pt.level.toFloat() / 100f) * h)
                        Offset(x, y)
                    }

                    // 1. Draw Safety Floor Threshold line (dotted Amber)
                    val safetyY = h - ((safetyFloorPercent.toFloat() / 100f) * h)
                    drawLine(
                        color = IceAmber.copy(alpha = 0.6f),
                        start = Offset(0f, safetyY),
                        end = Offset(w, safetyY),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                    )

                    // 2. Build smooth cubic bezier curve
                    val path = Path()
                    val fillPath = Path()

                    path.moveTo(coords.first().x, coords.first().y)
                    fillPath.moveTo(coords.first().x, h)
                    fillPath.lineTo(coords.first().x, coords.first().y)

                    for (i in 0 until coords.size - 1) {
                        val p0 = coords[i]
                        val p1 = coords[i + 1]
                        val cx = (p0.x + p1.x) / 2f
                        path.cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                        fillPath.cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                    }

                    fillPath.lineTo(coords.last().x, h)
                    fillPath.close()

                    // 3. Draw under-curve vertical gradient fill
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                IceCyan.copy(alpha = 0.35f),
                                IceBlue.copy(alpha = 0.15f),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = h
                        )
                    )

                    // 4. Draw outer glow line
                    drawPath(
                        path = path,
                        color = IceCyan.copy(alpha = 0.25f),
                        style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // 5. Draw main line
                    drawPath(
                        path = path,
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                IceBlue,
                                IceCyan,
                                if (currentLevel < safetyFloorPercent) IceAmber else IceGreen
                            )
                        ),
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // 6. Draw special event markers / charging points
                    coords.forEachIndexed { i, ptCoord ->
                        val pt = history[i]
                        if (pt.isCharging) {
                            drawCircle(
                                color = IceGreen,
                                radius = 4.dp.toPx(),
                                center = ptCoord
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 2.dp.toPx(),
                                center = ptCoord
                            )
                        } else if (i == selectedIndex) {
                            // Scrubber highlighted node
                            drawLine(
                                color = IceCyan.copy(alpha = 0.5f),
                                start = Offset(ptCoord.x, 0f),
                                end = Offset(ptCoord.x, h),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                            )
                            drawCircle(
                                color = IceCyan,
                                radius = 6.dp.toPx(),
                                center = ptCoord
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 3.dp.toPx(),
                                center = ptCoord
                            )
                        }
                    }

                    // 7. Draw latest live point (with pulsing halo)
                    val lastCoord = coords.last()
                    drawCircle(
                        color = IceCyan.copy(alpha = 0.3f),
                        radius = pulseRadius.dp.toPx(),
                        center = lastCoord
                    )
                    drawCircle(
                        color = IceCyan,
                        radius = 5.dp.toPx(),
                        center = lastCoord
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 2.5.dp.toPx(),
                        center = lastCoord
                    )
                }
            }

            // X-Axis Time Markers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 32.dp, end = 12.dp, top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("-24h", "-18h", "-12h", "-6h", "-1h", "Now").forEach { timeLabel ->
                    Text(
                        text = timeLabel,
                        fontSize = 9.sp,
                        color = if (timeLabel == "Now") IceCyan else Color.White.copy(alpha = 0.4f),
                        fontWeight = if (timeLabel == "Now") FontWeight.Bold else FontWeight.Normal,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Pre-Transfer Context Analysis & Safety Recommendation
            val safeMargin = currentLevel - safetyFloorPercent
            val canSupportTransfer = if (role == "DONOR") safeMargin >= targetPercent else true
            val remainingAfter = currentLevel - targetPercent

            val bannerBg = if (role == "DONOR") {
                if (canSupportTransfer) Color(0xFF0B241B) else Color(0xFF2A1B0B)
            } else {
                Color(0xFF0C2433)
            }

            val bannerBorder = if (role == "DONOR") {
                if (canSupportTransfer) IceGreen.copy(alpha = 0.4f) else IceAmber.copy(alpha = 0.5f)
            } else {
                IceCyan.copy(alpha = 0.4f)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(bannerBg)
                    .border(1.dp, bannerBorder, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = if (role == "DONOR") {
                        if (canSupportTransfer) Icons.Default.CheckCircle else Icons.Default.Warning
                    } else {
                        Icons.Default.Info
                    },
                    contentDescription = null,
                    tint = if (role == "DONOR") {
                        if (canSupportTransfer) IceGreen else IceAmber
                    } else {
                        IceCyan
                    },
                    modifier = Modifier.size(20.dp)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (role == "DONOR") {
                            if (canSupportTransfer) "Safe to Transfer: Healthy 24h Profile" else "Tight Margin Alert"
                        } else {
                            "Recipient Ready: Power Boost"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = if (role == "DONOR") {
                            if (canSupportTransfer) {
                                "You have $safeMargin% headroom above your $safetyFloorPercent% safety floor. Sharing $targetPercent% leaves you with $remainingAfter%."
                            } else {
                                "Sharing $targetPercent% drops you below your $safetyFloorPercent% safety threshold ($remainingAfter% remaining). Consider lowering target to +${safeMargin.coerceAtLeast(1)}%."
                            }
                        } else {
                            "Your battery has varied between $minLevel% and $maxLevel% over the last 24 hours. Receiving +$targetPercent% will restore level to ${currentLevel + targetPercent}%."
                        },
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricChip(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0B141E))
            .border(1.dp, IceCardStroke.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
            .padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(10.dp)
                )
                Text(
                    text = label,
                    fontSize = 8.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = accentColor
            )
        }
    }
}

package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.TransferStatus
import com.example.ui.TransferUiState
import com.example.ui.theme.IceAmber
import com.example.ui.theme.IceCardDark
import com.example.ui.theme.IceCardStroke
import com.example.ui.theme.IceCyan
import com.example.ui.theme.IceGreen
import com.example.ui.theme.IceRed

@Composable
fun TransferControlsCard(
    state: TransferUiState,
    onRoleChange: (String) -> Unit,
    onModeChange: (String) -> Unit,
    onTargetChange: (Int) -> Unit,
    onSafetyFloorChange: (Int) -> Unit,
    onStartTransfer: () -> Unit,
    onPauseTransfer: () -> Unit,
    onResumeTransfer: () -> Unit,
    onCancelTransfer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isTransferring = state.status == TransferStatus.TRANSFERRING
    val isPaused = state.status == TransferStatus.PAUSED
    val isCompleted = state.status == TransferStatus.COMPLETED

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = IceCardDark),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(IceCardStroke))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // 1. Role Selector Segmented Control
            Text(
                text = "MY ROLE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0C1520))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Share Power Button
                val isDonor = state.role == "DONOR"
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isDonor) IceAmber.copy(alpha = 0.2f) else Color.Transparent)
                        .border(
                            width = if (isDonor) 1.dp else 0.dp,
                            color = if (isDonor) IceAmber else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable(enabled = !isTransferring) { onRoleChange("DONOR") }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = if (isDonor) IceAmber else Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Share Power",
                            fontSize = 13.sp,
                            fontWeight = if (isDonor) FontWeight.Bold else FontWeight.Medium,
                            color = if (isDonor) IceAmber else Color(0xFF94A3B8)
                        )
                    }
                }

                // Receive Power Button
                val isRecipient = state.role == "RECIPIENT"
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isRecipient) IceCyan.copy(alpha = 0.2f) else Color.Transparent)
                        .border(
                            width = if (isRecipient) 1.dp else 0.dp,
                            color = if (isRecipient) IceCyan else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable(enabled = !isTransferring) { onRoleChange("RECIPIENT") }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = if (isRecipient) IceCyan else Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Receive Power",
                            fontSize = 13.sp,
                            fontWeight = if (isRecipient) FontWeight.Bold else FontWeight.Medium,
                            color = if (isRecipient) IceCyan else Color(0xFF94A3B8)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Hardware Mode Selection (Wireless vs Cable)
            Text(
                text = "CONNECTION TYPE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val isWireless = state.transferMode == "WIRELESS"
                FilterChip(
                    selected = isWireless,
                    onClick = { onModeChange("WIRELESS") },
                    label = { Text("Wireless PowerShare") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Contactless,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = IceCyan.copy(alpha = 0.2f),
                        selectedLabelColor = IceCyan,
                        selectedLeadingIconColor = IceCyan
                    )
                )

                val isCable = state.transferMode == "USB_C"
                FilterChip(
                    selected = isCable,
                    onClick = { onModeChange("USB_C") },
                    label = { Text("USB-C Cable") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Cable,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = IceCyan.copy(alpha = 0.2f),
                        selectedLabelColor = IceCyan,
                        selectedLeadingIconColor = IceCyan
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Target Percentage Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TARGET PERCENTAGE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "+${state.targetPercent}%",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = IceCyan
                )
            }

            // Quick preset chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(5, 10, 15, 20, 25).forEach { preset ->
                    val isSelected = state.targetPercent == preset
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) IceCyan else Color(0xFF162538))
                            .clickable(enabled = !isTransferring) { onTargetChange(preset) }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+$preset%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color(0xFF001F26) else Color(0xFF94A3B8)
                        )
                    }
                }
            }

            Slider(
                value = state.targetPercent.toFloat(),
                onValueChange = { onTargetChange(it.toInt()) },
                valueRange = 1f..40f,
                steps = 38,
                enabled = !isTransferring,
                colors = SliderDefaults.colors(
                    thumbColor = IceCyan,
                    activeTrackColor = IceCyan,
                    inactiveTrackColor = Color(0xFF1E293B)
                )
            )

            // Safety floor option for Donor
            if (state.role == "DONOR") {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = IceAmber,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Safety Floor (Auto-stop)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "Pause at ${state.safetyFloorPercent}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = IceAmber
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 4. Primary Action Button
            when {
                isTransferring -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onPauseTransfer,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("pause_button"),
                            shape = RoundedCornerShape(12.dp),
                            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(IceAmber))
                        ) {
                            Icon(Icons.Default.Pause, contentDescription = "Pause", tint = IceAmber)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pause", color = IceAmber, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onCancelTransfer,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("stop_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = IceRed)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = "Stop", tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Stop", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                isPaused -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onResumeTransfer,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("resume_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = IceCyan)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Resume", tint = Color(0xFF00222B))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Resume", color = Color(0xFF00222B), fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onCancelTransfer,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("cancel_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Finish Session", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                isCompleted -> {
                    Button(
                        onClick = onCancelTransfer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("complete_reset_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = IceGreen)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Transfer Complete! Start New Session", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
                else -> {
                    Button(
                        onClick = onStartTransfer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("start_transfer_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = IceCyan,
                            contentColor = Color(0xFF001F26)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (state.role == "DONOR") "START SHARING BATTERY" else "START RECEIVING BATTERY",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }
    }
}

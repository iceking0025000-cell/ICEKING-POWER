package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.Battery24hChartCard
import com.example.ui.components.BatteryGlowIndicator
import com.example.ui.components.DevicePairingSheet
import com.example.ui.components.DiagnosticsSheet
import com.example.ui.components.EnergyTransferBeam
import com.example.ui.components.HardwareGuideCard
import com.example.ui.components.LiveStatsCard
import com.example.ui.components.QrPairingDialog
import com.example.ui.components.SecurityConfirmationDialog
import com.example.ui.components.TransferControlsCard
import com.example.ui.components.TransferHistorySheet
import com.example.ui.components.TransferProgressBarIndicator
import com.example.ui.theme.IceAmber
import com.example.ui.theme.IceCardDark
import com.example.ui.theme.IceCardStroke
import com.example.ui.theme.IceCyan
import com.example.ui.theme.IceDeepNavy
import com.example.ui.theme.IceGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: TransferViewModel,
    state: TransferUiState,
    modifier: Modifier = Modifier
) {
    var showPairingSheet by remember { mutableStateOf(false) }
    var showQrPairingSheet by remember { mutableStateOf(false) }
    var showHistorySheet by remember { mutableStateOf(false) }
    var showDiagnosticsSheet by remember { mutableStateOf(false) }

    val pairingSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val qrPairingSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val historySheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val diagSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val isTransferring = state.status == TransferStatus.TRANSFERRING
    val partnerLevel = state.connectedPeer?.batteryLevel ?: 45
    val partnerName = state.connectedPeer?.name ?: "Tap to Link"
    val isPartnerConnected = state.connectedPeer != null

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = IceDeepNavy,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF003847))
                                .border(1.dp, IceCyan, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = IceCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "ICEPOWER",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Battery Transfer Hub",
                                fontSize = 10.sp,
                                color = IceCyan,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                },
                actions = {
                    // QR Code Pairing
                    IconButton(
                        onClick = { showQrPairingSheet = true },
                        modifier = Modifier.testTag("qr_pairing_top_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "QR Pairing",
                            tint = IceCyan
                        )
                    }

                    // Sound toggle
                    IconButton(
                        onClick = { viewModel.toggleSound() },
                        modifier = Modifier.testTag("sound_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (state.soundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = "Sound Settings",
                            tint = if (state.soundEnabled) IceCyan else Color(0xFF64748B)
                        )
                    }

                    // Battery Diagnostics
                    IconButton(
                        onClick = { showDiagnosticsSheet = true },
                        modifier = Modifier.testTag("diagnostics_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Diagnostics",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // History
                    IconButton(
                        onClick = { showHistorySheet = true },
                        modifier = Modifier.testTag("history_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "History",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = IceDeepNavy,
                    scrolledContainerColor = IceCardDark
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Notification Banner (if any)
            AnimatedVisibility(
                visible = state.statusBanner != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                if (state.statusBanner != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (state.statusBanner.contains("SAFETY", ignoreCase = true)) Color(0xFF331515)
                            else if (state.statusBanner.contains("Complete", ignoreCase = true)) Color(0xFF0F3323)
                            else Color(0xFF0C2433)
                        ),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(
                                if (state.statusBanner.contains("SAFETY", ignoreCase = true)) Color(0xFFEF4444)
                                else if (state.statusBanner.contains("Complete", ignoreCase = true)) IceGreen
                                else IceCyan
                            )
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = if (state.statusBanner.contains("SAFETY", ignoreCase = true)) Color(0xFFEF4444) else IceCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = state.statusBanner,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            IconButton(
                                onClick = { viewModel.dismissBanner() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }

            // Dual Battery Stage Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = IceCardDark),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(IceCardStroke))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Header Bar with Link Status
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
                                    .background(if (isPartnerConnected) IceGreen else IceAmber)
                            )
                            Text(
                                text = if (isPartnerConnected) "DEVICES LINKED" else "DISCONNECTED",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isPartnerConnected) IceGreen else IceAmber,
                                letterSpacing = 1.sp
                            )
                        }

                        // Connect / Change Device button
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0F1E2C))
                                .clickable { showPairingSheet = true }
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Devices,
                                contentDescription = null,
                                tint = IceCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (isPartnerConnected) "Change Device" else "Connect Phone",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = IceCyan
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Side-by-side Dual Indicators with Energy Beam in Center
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Local Device Indicator (Left)
                        BatteryGlowIndicator(
                            deviceName = state.localDeviceName,
                            level = state.localBattery.level,
                            isCharging = state.localBattery.isCharging,
                            role = state.role,
                            isTransferring = isTransferring,
                            modifier = Modifier.weight(1f),
                            size = 115.dp,
                            subtitle = "${state.localBattery.voltageV}V",
                            isLocal = true
                        )

                        // 2. Animated Energy Beam (Center)
                        val flowLeftToRight = state.role == "DONOR"
                        EnergyTransferBeam(
                            isTransferring = isTransferring,
                            flowFromLeftToRight = flowLeftToRight,
                            rateMa = state.currentRateMa,
                            modifier = Modifier
                                .weight(0.9f)
                                .padding(horizontal = 4.dp)
                        )

                        // 3. Partner Device Indicator (Right)
                        if (isPartnerConnected) {
                            BatteryGlowIndicator(
                                deviceName = partnerName,
                                level = partnerLevel,
                                isCharging = state.connectedPeer?.isCharging == true,
                                role = if (state.role == "DONOR") "RECIPIENT" else "DONOR",
                                isTransferring = isTransferring,
                                modifier = Modifier.weight(1f),
                                size = 115.dp,
                                subtitle = if (state.isSimulatedMode) "Demo Partner" else "Wi-Fi Direct",
                                isLocal = false
                            )
                        } else {
                            // Unconnected placeholder prompt
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF0C1520))
                                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp))
                                    .clickable { showPairingSheet = true }
                                    .padding(vertical = 20.dp, horizontal = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF162536)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhoneAndroid,
                                        contentDescription = null,
                                        tint = IceCyan,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Add Partner",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = IceCyan
                                )
                                Text(
                                    text = "Tap to pair",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Visual Progress Bar Indicator for Immediate Feedback on Stage
                    AnimatedVisibility(
                        visible = state.status != TransferStatus.IDLE,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(14.dp))
                            TransferProgressBarIndicator(
                                state = state,
                                barHeight = 10.dp,
                                showDetailedSyncTelemetry = false
                            )
                        }
                    }
                }
            }

            // Live Stats Card (visible if transferring, paused, or completed)
            if (state.status != TransferStatus.IDLE) {
                LiveStatsCard(state = state)
            }

            // 24-Hour Battery Timeline & Pre-Transfer Context Analysis
            Battery24hChartCard(
                history = state.batteryHistory24h,
                currentLevel = state.localBattery.level,
                safetyFloorPercent = state.safetyFloorPercent,
                targetPercent = state.targetPercent,
                role = state.role
            )

            // Transfer Controls Card (Role, Target, Safety, Start Button)
            TransferControlsCard(
                state = state,
                onRoleChange = { viewModel.setRole(it) },
                onModeChange = { viewModel.setTransferMode(it) },
                onTargetChange = { viewModel.setTargetPercent(it) },
                onSafetyFloorChange = { viewModel.setSafetyFloor(it) },
                onStartTransfer = { viewModel.startTransfer() },
                onPauseTransfer = { viewModel.pauseTransfer() },
                onResumeTransfer = { viewModel.resumeTransfer() },
                onCancelTransfer = { viewModel.cancelTransfer() }
            )

            // Hardware Battery Share Launcher & 2-Step Guide
            HardwareGuideCard()

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Bottom Sheets & Dialogs
    if (showPairingSheet) {
        DevicePairingSheet(
            sheetState = pairingSheetState,
            localDeviceName = state.localDeviceName,
            localIp = state.localIp,
            localPairingPin = state.localPairingPin,
            discoveredPeers = state.discoveredPeers,
            connectedPeer = state.connectedPeer,
            isSimulatedMode = state.isSimulatedMode,
            onConnectPeer = {
                viewModel.requestPairWithPeer(it)
                showPairingSheet = false
            },
            onDisconnectPeer = { viewModel.disconnectPeer() },
            onToggleSimulated = { viewModel.toggleSimulatedPartner(it) },
            onOpenQrPairing = {
                showPairingSheet = false
                showQrPairingSheet = true
            },
            onDismiss = { showPairingSheet = false }
        )
    }

    if (showQrPairingSheet) {
        QrPairingDialog(
            sheetState = qrPairingSheetState,
            localDeviceId = state.localDeviceId,
            localDeviceName = state.localDeviceName,
            localPairingPin = state.localPairingPin,
            localIp = state.localIp,
            localBatteryLevel = state.localBattery.level,
            currentRole = state.role,
            onRegeneratePin = { viewModel.regeneratePairingPin() },
            onQrScanned = { payload ->
                viewModel.pairViaQrCode(payload)
                showQrPairingSheet = false
            },
            onSimulateScanPartner = {
                viewModel.toggleSimulatedPartner(true)
                showQrPairingSheet = false
            },
            onDismiss = { showQrPairingSheet = false }
        )
    }

    // Security Confirmation Dialog for incoming pairing/transfer authorization requests
    state.pendingAuthRequest?.let { pendingPeer ->
        SecurityConfirmationDialog(
            peer = pendingPeer,
            onAuthorize = { viewModel.confirmAuthorization(pendingPeer, true) },
            onReject = { viewModel.confirmAuthorization(pendingPeer, false) }
        )
    }

    if (showHistorySheet) {
        TransferHistorySheet(
            sheetState = historySheetState,
            logs = state.historyLogs,
            onClearHistory = { viewModel.clearHistory() },
            onDismiss = { showHistorySheet = false }
        )
    }

    if (showDiagnosticsSheet) {
        DiagnosticsSheet(
            sheetState = diagSheetState,
            batteryInfo = state.localBattery,
            onDismiss = { showDiagnosticsSheet = false }
        )
    }
}

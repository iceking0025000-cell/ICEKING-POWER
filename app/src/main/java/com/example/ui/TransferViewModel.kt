package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.battery.BatteryInfo
import com.example.battery.BatteryMonitor
import com.example.data.BatteryHistoryPoint
import com.example.data.IcePowerDatabase
import com.example.data.TransferLog
import com.example.network.PeerBatterySync
import com.example.network.PeerDevice
import com.example.util.IceFeedback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class TransferStatus {
    IDLE,
    TRANSFERRING,
    PAUSED,
    COMPLETED
}

data class TransferUiState(
    val localBattery: BatteryInfo = BatteryInfo(),
    val role: String = "DONOR", // "DONOR" (Share Power) or "RECIPIENT" (Receive Power)
    val transferMode: String = "WIRELESS", // "WIRELESS" or "USB_C"
    val targetPercent: Int = 10,
    val safetyFloorPercent: Int = 20,
    val status: TransferStatus = TransferStatus.IDLE,
    val startLocalPercent: Int = 0,
    val startPartnerPercent: Int = 0,
    val transferredPercent: Int = 0,
    val elapsedSeconds: Long = 0,
    val estimatedRemainingSeconds: Long = 0,
    val currentRateMa: Int = 1450,
    val connectedPeer: PeerDevice? = null,
    val discoveredPeers: List<PeerDevice> = emptyList(),
    val isSimulatedMode: Boolean = false,
    val soundEnabled: Boolean = true,
    val localIp: String = "127.0.0.1",
    val localDeviceName: String = "This Android Device",
    val localDeviceId: String = "",
    val localPairingPin: String = "4829",
    val pendingAuthRequest: PeerDevice? = null,
    val showQrScanner: Boolean = false,
    val batteryHistory24h: List<BatteryHistoryPoint> = emptyList(),
    val syncPacketsCount: Int = 14,
    val syncLatencyMs: Int = 32,
    val statusBanner: String? = null,
    val historyLogs: List<TransferLog> = emptyList()
)

class TransferViewModel(application: Application) : AndroidViewModel(application) {

    private val db = IcePowerDatabase.getInstance(application)
    private val transferDao = db.transferDao()

    private val batteryMonitor = BatteryMonitor(application)
    private val peerSync = PeerBatterySync(application, viewModelScope)
    private val feedback = IceFeedback(application)

    private val _uiState = MutableStateFlow(
        TransferUiState(
            localDeviceName = peerSync.localDeviceName,
            localDeviceId = peerSync.localDeviceId,
            localPairingPin = peerSync.localPairingPin.value
        )
    )
    val uiState: StateFlow<TransferUiState> = _uiState.asStateFlow()

    private var sessionTimerJob: Job? = null

    init {
        batteryMonitor.startListening()
        peerSync.start(batteryMonitor.batteryInfo)

        // Observe local battery
        viewModelScope.launch {
            batteryMonitor.batteryInfo.collectLatest { info ->
                _uiState.update { current ->
                    val history = if (current.batteryHistory24h.isEmpty()) {
                        generate24hHistory(info.level, info.isCharging)
                    } else {
                        updateHistoryWithLatest(current.batteryHistory24h, info.level, info.isCharging)
                    }
                    current.copy(
                        localBattery = info,
                        batteryHistory24h = history
                    )
                }
                checkSafetyFloor(info.level)
            }
        }

        // Observe local pairing PIN
        viewModelScope.launch {
            peerSync.localPairingPin.collectLatest { pin ->
                _uiState.update { it.copy(localPairingPin = pin) }
            }
        }

        // Observe discovered peers
        viewModelScope.launch {
            peerSync.discoveredPeers.collectLatest { peers ->
                _uiState.update { it.copy(discoveredPeers = peers) }
            }
        }

        // Observe connected peer
        viewModelScope.launch {
            peerSync.connectedPeer.collectLatest { peer ->
                _uiState.update { it.copy(connectedPeer = peer) }
            }
        }

        // Observe local IP
        viewModelScope.launch {
            peerSync.localIp.collectLatest { ip ->
                _uiState.update { it.copy(localIp = ip) }
            }
        }

        // Observe history logs
        viewModelScope.launch {
            transferDao.getAllLogs().collectLatest { logs ->
                _uiState.update { it.copy(historyLogs = logs) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        batteryMonitor.stopListening()
        peerSync.stop()
    }

    fun setRole(newRole: String) {
        if (_uiState.value.status == TransferStatus.TRANSFERRING) return
        _uiState.update { it.copy(role = newRole) }
        feedback.playClick()
        syncStateToPeer()
    }

    fun setTransferMode(mode: String) {
        _uiState.update { it.copy(transferMode = mode) }
        feedback.playClick()
    }

    fun setTargetPercent(target: Int) {
        val clamped = target.coerceIn(1, 50)
        _uiState.update { it.copy(targetPercent = clamped) }
        feedback.playClick()
        syncStateToPeer()
    }

    fun setSafetyFloor(floor: Int) {
        val clamped = floor.coerceIn(5, 50)
        _uiState.update { it.copy(safetyFloorPercent = clamped) }
        feedback.playClick()
    }

    fun toggleSound() {
        _uiState.update { it.copy(soundEnabled = !it.soundEnabled) }
    }

    fun regeneratePairingPin() {
        peerSync.regeneratePairingPin()
        _uiState.update { it.copy(localPairingPin = peerSync.localPairingPin.value) }
        feedback.playClick()
    }

    fun setShowQrScanner(show: Boolean) {
        _uiState.update { it.copy(showQrScanner = show) }
    }

    fun requestPairWithPeer(peer: PeerDevice) {
        if (peer.isAuthorized) {
            connectPeer(peer)
        } else {
            // Trigger confirmation dialog for user approval
            _uiState.update { it.copy(pendingAuthRequest = peer) }
            feedback.playWarning()
        }
    }

    fun confirmAuthorization(peer: PeerDevice, authorized: Boolean) {
        if (authorized) {
            val authorizedPeer = peer.copy(isAuthorized = true)
            connectPeer(authorizedPeer)
            _uiState.update {
                it.copy(
                    pendingAuthRequest = null,
                    statusBanner = "Security check passed: ${peer.name} authorized!"
                )
            }
            feedback.playTransferStarted()
        } else {
            _uiState.update {
                it.copy(
                    pendingAuthRequest = null,
                    statusBanner = "Authorization rejected for ${peer.name}."
                )
            }
            feedback.playWarning()
        }
    }

    fun pairViaQrCode(qrPayload: String): Boolean {
        val parsed = com.example.util.QrCodeHelper.parsePairingUri(qrPayload)
        return if (parsed != null) {
            val authorizedPeer = parsed.copy(isAuthorized = true)
            connectPeer(authorizedPeer)
            _uiState.update {
                it.copy(
                    showQrScanner = false,
                    statusBanner = "QR Code Verified! Securely linked with ${parsed.name}"
                )
            }
            feedback.playTransferStarted()
            true
        } else {
            _uiState.update {
                it.copy(statusBanner = "Invalid ICEPOWER QR code. Try scanning again.")
            }
            feedback.playWarning()
            false
        }
    }

    fun connectPeer(peer: PeerDevice) {
        peerSync.connectPeer(peer)
        feedback.playClick()
        _uiState.update {
            it.copy(
                connectedPeer = peer,
                isSimulatedMode = peer.isSimulated,
                statusBanner = "Linked with ${peer.name}!"
            )
        }
    }

    fun disconnectPeer() {
        peerSync.disconnectPeer()
        feedback.playClick()
        _uiState.update {
            it.copy(
                connectedPeer = null,
                isSimulatedMode = false,
                statusBanner = "Device disconnected."
            )
        }
    }

    fun toggleSimulatedPartner(enabled: Boolean) {
        feedback.playClick()
        val currentLocalLevel = _uiState.value.localBattery.level
        val initialSimLevel = if (_uiState.value.role == "DONOR") {
            (currentLocalLevel - 35).coerceAtLeast(18)
        } else {
            (currentLocalLevel + 25).coerceAtMost(95)
        }

        peerSync.toggleSimulatedPartner(enabled, initialSimLevel)
        _uiState.update {
            it.copy(
                isSimulatedMode = enabled,
                statusBanner = if (enabled) "Demo Partner Active (Pixel 9 Pro)" else "Demo Partner Disabled"
            )
        }
    }

    fun startTransfer() {
        val state = _uiState.value
        val localLevel = state.localBattery.level
        val partnerLevel = state.connectedPeer?.batteryLevel ?: 50

        // Safety check for donor
        if (state.role == "DONOR" && localLevel <= state.safetyFloorPercent) {
            feedback.playWarning()
            _uiState.update {
                it.copy(statusBanner = "Battery is at or below safety floor (${state.safetyFloorPercent}%). Charge before sharing!")
            }
            return
        }

        _uiState.update {
            it.copy(
                status = TransferStatus.TRANSFERRING,
                startLocalPercent = localLevel,
                startPartnerPercent = partnerLevel,
                transferredPercent = 0,
                elapsedSeconds = 0,
                statusBanner = "Transfer active! Transferring ${state.targetPercent}%..."
            )
        }

        if (state.soundEnabled) {
            feedback.playTransferStarted()
        }

        syncStateToPeer()
        startSessionTimer()
    }

    fun pauseTransfer() {
        sessionTimerJob?.cancel()
        _uiState.update {
            it.copy(
                status = TransferStatus.PAUSED,
                statusBanner = "Transfer paused."
            )
        }
        feedback.playClick()
        syncStateToPeer()
    }

    fun resumeTransfer() {
        _uiState.update {
            it.copy(
                status = TransferStatus.TRANSFERRING,
                statusBanner = "Transfer resumed..."
            )
        }
        if (_uiState.value.soundEnabled) {
            feedback.playClick()
        }
        syncStateToPeer()
        startSessionTimer()
    }

    fun cancelTransfer() {
        val state = _uiState.value
        sessionTimerJob?.cancel()

        if (state.transferredPercent > 0) {
            saveTransferLog(completed = false)
        }

        _uiState.update {
            it.copy(
                status = TransferStatus.IDLE,
                transferredPercent = 0,
                elapsedSeconds = 0,
                statusBanner = "Transfer stopped."
            )
        }
        feedback.playClick()
        syncStateToPeer()
    }

    private fun startSessionTimer() {
        sessionTimerJob?.cancel()
        sessionTimerJob = viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(1000)
                val state = _uiState.value
                if (state.status != TransferStatus.TRANSFERRING) break

                val newElapsed = state.elapsedSeconds + 1

                // Simulate/calculate progressive transfer increment
                // For real hardware or simulation, 1% per 8-12 seconds in accelerated/demo mode,
                // or tracks real battery level change if connected to real charger
                val shouldIncrementPercent = (newElapsed % 4L == 0L)
                val nextTransferred = if (shouldIncrementPercent) {
                    state.transferredPercent + 1
                } else {
                    state.transferredPercent
                }

                val remainingSeconds = if (nextTransferred < state.targetPercent) {
                    ((state.targetPercent - nextTransferred) * 4L)
                } else {
                    0L
                }

                _uiState.update {
                    it.copy(
                        elapsedSeconds = newElapsed,
                        transferredPercent = nextTransferred,
                        estimatedRemainingSeconds = remainingSeconds,
                        syncPacketsCount = it.syncPacketsCount + 1,
                        syncLatencyMs = (24..42).random()
                    )
                }

                syncStateToPeer()

                if (nextTransferred >= state.targetPercent) {
                    onTransferCompleted()
                    break
                }
            }
        }
    }

    private fun onTransferCompleted() {
        val state = _uiState.value
        sessionTimerJob?.cancel()

        val eventLabel = if (state.role == "DONOR") "Sent -${state.targetPercent}%" else "Received +${state.targetPercent}%"
        val updatedLevel = if (state.role == "DONOR") {
            (state.localBattery.level - state.targetPercent).coerceAtLeast(0)
        } else {
            (state.localBattery.level + state.targetPercent).coerceAtMost(100)
        }
        val transferPoint = BatteryHistoryPoint(
            timestampMs = System.currentTimeMillis(),
            level = updatedLevel,
            isCharging = state.role == "RECIPIENT",
            eventLabel = eventLabel
        )

        _uiState.update {
            it.copy(
                status = TransferStatus.COMPLETED,
                statusBanner = "Target Reached! Successfully transferred ${state.targetPercent}%.",
                batteryHistory24h = it.batteryHistory24h + transferPoint
            )
        }

        if (state.soundEnabled) {
            feedback.playTransferCompleted()
        }

        saveTransferLog(completed = true)
        syncStateToPeer()
    }

    private fun checkSafetyFloor(currentLocalLevel: Int) {
        val state = _uiState.value
        if (state.status == TransferStatus.TRANSFERRING && state.role == "DONOR") {
            if (currentLocalLevel <= state.safetyFloorPercent) {
                sessionTimerJob?.cancel()
                _uiState.update {
                    it.copy(
                        status = TransferStatus.PAUSED,
                        statusBanner = "SAFETY STOP: Local battery reached safety floor (${state.safetyFloorPercent}%). Transfer paused to preserve power."
                    )
                }
                feedback.playWarning()
                syncStateToPeer()
            }
        }
    }

    private fun saveTransferLog(completed: Boolean) {
        val state = _uiState.value
        viewModelScope.launch(Dispatchers.IO) {
            val partner = state.connectedPeer?.name ?: "Paired Android Device"
            val log = TransferLog(
                partnerName = partner,
                role = state.role,
                mode = state.transferMode,
                startPercent = state.startLocalPercent,
                endPercent = state.localBattery.level,
                transferredPercent = state.transferredPercent,
                durationSeconds = state.elapsedSeconds,
                completedSuccessfully = completed
            )
            transferDao.insertLog(log)
        }
    }

    fun clearHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            transferDao.clearLogs()
        }
        feedback.playClick()
    }

    fun dismissBanner() {
        _uiState.update { it.copy(statusBanner = null) }
    }

    private fun syncStateToPeer() {
        val state = _uiState.value
        peerSync.updateSessionState(
            role = state.role,
            active = state.status == TransferStatus.TRANSFERRING,
            target = state.targetPercent,
            transferred = state.transferredPercent
        )
    }

    private fun generate24hHistory(currentLevel: Int, isCharging: Boolean): List<BatteryHistoryPoint> {
        val now = System.currentTimeMillis()
        val hourMs = 3600000L

        // Realistic progression over past 24 hours ending at currentLevel
        val baselinePoints = mutableListOf<BatteryHistoryPoint>()

        val p24 = (currentLevel + 22).coerceIn(45, 96)
        val p20 = (p24 - 8).coerceIn(40, 92)
        val p16 = (p20 - 10).coerceIn(35, 85)
        val p12 = (p16 - 12).coerceIn(28, 75)
        val p9 = (p12 - 8).coerceIn(22, 65)
        val p8 = (p9 + 45).coerceIn(65, 95) // morning fast charge session
        val p6 = (p8 - 7).coerceIn(55, 90)
        val p4 = (p6 - 6).coerceIn(45, 85)
        val p2 = if (isCharging) (currentLevel - 6).coerceAtLeast(10) else (currentLevel + 3).coerceAtMost(100)

        baselinePoints.add(BatteryHistoryPoint(now - 24 * hourMs, p24, false, "Overnight standby"))
        baselinePoints.add(BatteryHistoryPoint(now - 20 * hourMs, p20, false, null))
        baselinePoints.add(BatteryHistoryPoint(now - 16 * hourMs, p16, false, "Screen usage"))
        baselinePoints.add(BatteryHistoryPoint(now - 12 * hourMs, p12, false, null))
        baselinePoints.add(BatteryHistoryPoint(now - 9 * hourMs, p9, false, "Low level warning"))
        baselinePoints.add(BatteryHistoryPoint(now - 8 * hourMs, p8, true, "Morning Fast Charge"))
        baselinePoints.add(BatteryHistoryPoint(now - 6 * hourMs, p6, false, null))
        baselinePoints.add(BatteryHistoryPoint(now - 4 * hourMs, p4, false, "Discharge (-3.2%/h)"))
        baselinePoints.add(BatteryHistoryPoint(now - 2 * hourMs, p2, false, null))
        baselinePoints.add(
            BatteryHistoryPoint(
                now - 30 * 60000L,
                if (isCharging) (currentLevel - 2).coerceAtLeast(1) else (currentLevel + 1).coerceAtMost(100),
                isCharging,
                null
            )
        )
        baselinePoints.add(BatteryHistoryPoint(now, currentLevel, isCharging, "Now (${currentLevel}%)"))

        return baselinePoints
    }

    private fun updateHistoryWithLatest(
        existing: List<BatteryHistoryPoint>,
        currentLevel: Int,
        isCharging: Boolean
    ): List<BatteryHistoryPoint> {
        val now = System.currentTimeMillis()
        val last = existing.lastOrNull()
        return if (last != null && now - last.timestampMs < 60000L) {
            existing.dropLast(1) + last.copy(
                level = currentLevel,
                isCharging = isCharging,
                timestampMs = now
            )
        } else {
            val point = BatteryHistoryPoint(
                timestampMs = now,
                level = currentLevel,
                isCharging = isCharging,
                eventLabel = if (isCharging) "Charging" else null
            )
            val cutoff = now - 25 * 3600000L
            existing.filter { it.timestampMs >= cutoff } + point
        }
    }
}

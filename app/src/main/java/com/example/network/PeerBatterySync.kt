package com.example.network

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import com.example.battery.BatteryInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.UUID

data class PeerDevice(
    val id: String,
    val name: String,
    val batteryLevel: Int,
    val isCharging: Boolean,
    val role: String, // "DONOR" or "RECIPIENT"
    val isTransferring: Boolean,
    val targetPercent: Int,
    val currentTransferred: Int,
    val lastSeenMs: Long = System.currentTimeMillis(),
    val ipAddress: String = "",
    val isSimulated: Boolean = false,
    val pairingPin: String = "",
    val isAuthorized: Boolean = false
)

class PeerBatterySync(
    private val context: Context,
    private val scope: CoroutineScope
) {
    val localDeviceId = UUID.randomUUID().toString().take(8)
    val localDeviceName: String = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"

    private val _localPairingPin = MutableStateFlow(generateRandomPin())
    val localPairingPin: StateFlow<String> = _localPairingPin.asStateFlow()

    private val _discoveredPeers = MutableStateFlow<List<PeerDevice>>(emptyList())
    val discoveredPeers: StateFlow<List<PeerDevice>> = _discoveredPeers.asStateFlow()

    private val _connectedPeer = MutableStateFlow<PeerDevice?>(null)
    val connectedPeer: StateFlow<PeerDevice?> = _connectedPeer.asStateFlow()

    private val _localIp = MutableStateFlow("127.0.0.1")
    val localIp: StateFlow<String> = _localIp.asStateFlow()

    fun regeneratePairingPin() {
        _localPairingPin.value = generateRandomPin()
    }

    private fun generateRandomPin(): String {
        return (1000..9999).random().toString()
    }

    private var broadcastJob: Job? = null
    private var listenJob: Job? = null
    private var simulatedPeerJob: Job? = null

    private var currentBatteryInfo: BatteryInfo = BatteryInfo()
    private var currentRole: String = "DONOR"
    private var isTransferActive: Boolean = false
    private var sessionTarget: Int = 10
    private var sessionTransferred: Int = 0

    private val SYNC_PORT = 45454
    private var multicastLock: WifiManager.MulticastLock? = null

    init {
        updateLocalIp()
    }

    fun start(batteryInfoFlow: StateFlow<BatteryInfo>) {
        acquireMulticastLock()
        updateLocalIp()

        // Keep local battery info updated
        scope.launch {
            batteryInfoFlow.collect { info ->
                currentBatteryInfo = info
            }
        }

        // Start broadcasting our presence
        broadcastJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    sendBroadcastPacket()
                } catch (_: Exception) {}
                delay(1500)
            }
        }

        // Start listening for peer broadcasts
        listenJob = scope.launch(Dispatchers.IO) {
            listenForPackets()
        }

        // Cleanup stale peers every 4 seconds
        scope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(4000)
                val now = System.currentTimeMillis()
                _discoveredPeers.value = _discoveredPeers.value.filter {
                    it.isSimulated || (now - it.lastSeenMs < 6000)
                }
            }
        }
    }

    fun stop() {
        broadcastJob?.cancel()
        listenJob?.cancel()
        simulatedPeerJob?.cancel()
        releaseMulticastLock()
    }

    fun updateSessionState(role: String, active: Boolean, target: Int, transferred: Int) {
        currentRole = role
        isTransferActive = active
        sessionTarget = target
        sessionTransferred = transferred
    }

    fun connectPeer(peer: PeerDevice) {
        _connectedPeer.value = peer
    }

    fun disconnectPeer() {
        val current = _connectedPeer.value
        if (current?.isSimulated == true) {
            simulatedPeerJob?.cancel()
            _discoveredPeers.value = _discoveredPeers.value.filter { it.id != current.id }
        }
        _connectedPeer.value = null
    }

    fun toggleSimulatedPartner(enabled: Boolean, initialLevel: Int = 42) {
        if (!enabled) {
            simulatedPeerJob?.cancel()
            val current = _connectedPeer.value
            if (current?.isSimulated == true) {
                _connectedPeer.value = null
            }
            _discoveredPeers.value = _discoveredPeers.value.filterNot { it.isSimulated }
            return
        }

        val simPeer = PeerDevice(
            id = "sim-device-01",
            name = "Pixel 9 Pro (Partner)",
            batteryLevel = initialLevel,
            isCharging = currentRole == "DONOR",
            role = if (currentRole == "DONOR") "RECIPIENT" else "DONOR",
            isTransferring = isTransferActive,
            targetPercent = sessionTarget,
            currentTransferred = 0,
            ipAddress = "192.168.1.105 (Direct)",
            isSimulated = true,
            pairingPin = "6842",
            isAuthorized = false
        )

        _discoveredPeers.value = listOf(simPeer) + _discoveredPeers.value.filterNot { it.isSimulated }
        _connectedPeer.value = simPeer

        // Run simulation loop
        simulatedPeerJob?.cancel()
        simulatedPeerJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(1200)
                val peer = _connectedPeer.value
                if (peer != null && peer.isSimulated) {
                    if (isTransferActive) {
                        val newLevel = if (currentRole == "DONOR") {
                            // Local phone gives power to simulated partner
                            (peer.batteryLevel + 1).coerceAtMost(100)
                        } else {
                            // Simulated partner gives power to local phone
                            (peer.batteryLevel - 1).coerceAtLeast(5)
                        }
                        _connectedPeer.value = peer.copy(
                            batteryLevel = newLevel,
                            isCharging = currentRole == "DONOR",
                            isTransferring = true,
                            lastSeenMs = System.currentTimeMillis()
                        )
                    }
                }
            }
        }
    }

    private fun sendBroadcastPacket() {
        val socket = DatagramSocket()
        socket.broadcast = true
        try {
            val json = JSONObject().apply {
                put("v", 1)
                put("id", localDeviceId)
                put("name", localDeviceName)
                put("level", currentBatteryInfo.level)
                put("charging", currentBatteryInfo.isCharging)
                put("role", currentRole)
                put("transferring", isTransferActive)
                put("target", sessionTarget)
                put("transferred", sessionTransferred)
                put("pin", _localPairingPin.value)
            }

            val data = json.toString().toByteArray(Charsets.UTF_8)
            val broadcastAddress = getBroadcastAddress()
            val packet = DatagramPacket(data, data.size, broadcastAddress, SYNC_PORT)
            socket.send(packet)
        } finally {
            socket.close()
        }
    }

    private fun listenForPackets() {
        var socket: DatagramSocket? = null
        try {
            socket = DatagramSocket(SYNC_PORT)
            val buffer = ByteArray(1024)
            while (scope.isActive) {
                val packet = DatagramPacket(buffer, buffer.size)
                socket.receive(packet)
                val raw = String(packet.data, 0, packet.length, Charsets.UTF_8)
                val senderIp = packet.address.hostAddress ?: ""

                handleReceivedJson(raw, senderIp)
            }
        } catch (_: Exception) {
        } finally {
            socket?.close()
        }
    }

    private fun handleReceivedJson(rawJson: String, senderIp: String) {
        try {
            val json = JSONObject(rawJson)
            val id = json.optString("id", "")
            if (id.isEmpty() || id == localDeviceId) return // Ignore self

            val name = json.optString("name", "Android Peer")
            val level = json.optInt("level", 50)
            val charging = json.optBoolean("charging", false)
            val role = json.optString("role", "RECIPIENT")
            val transferring = json.optBoolean("transferring", false)
            val target = json.optInt("target", 10)
            val transferred = json.optInt("transferred", 0)
            val pin = json.optString("pin", "")

            val previous = _connectedPeer.value?.takeIf { it.id == id }

            val peer = PeerDevice(
                id = id,
                name = name,
                batteryLevel = level,
                isCharging = charging,
                role = role,
                isTransferring = transferring,
                targetPercent = target,
                currentTransferred = transferred,
                lastSeenMs = System.currentTimeMillis(),
                ipAddress = senderIp,
                isSimulated = false,
                pairingPin = pin.ifEmpty { previous?.pairingPin ?: "" },
                isAuthorized = previous?.isAuthorized ?: false
            )

            // Update discovered peers
            val currentList = _discoveredPeers.value.filter { it.id != id }
            _discoveredPeers.value = listOf(peer) + currentList

            // If this is our currently connected peer, update live stats
            if (_connectedPeer.value?.id == id) {
                _connectedPeer.value = peer
            }
        } catch (_: Exception) {}
    }

    private fun getBroadcastAddress(): InetAddress {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                if (networkInterface.isLoopback || !networkInterface.isUp) continue
                for (interfaceAddress in networkInterface.interfaceAddresses) {
                    val broadcast = interfaceAddress.broadcast
                    if (broadcast != null) return broadcast
                }
            }
        } catch (_: Exception) {}
        return InetAddress.getByName("255.255.255.255")
    }

    private fun updateLocalIp() {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val net = interfaces.nextElement()
                if (net.isLoopback || !net.isUp) continue
                val addrs = net.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (!addr.isLoopbackAddress && addr.hostAddress?.contains(":") == false) {
                        _localIp.value = addr.hostAddress ?: "127.0.0.1"
                        return
                    }
                }
            }
        } catch (_: Exception) {
            _localIp.value = "127.0.0.1"
        }
    }

    private fun acquireMulticastLock() {
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            multicastLock = wifiManager?.createMulticastLock("icepower_multicast")
            multicastLock?.setReferenceCounted(true)
            multicastLock?.acquire()
        } catch (_: Exception) {}
    }

    private fun releaseMulticastLock() {
        try {
            if (multicastLock?.isHeld == true) {
                multicastLock?.release()
            }
        } catch (_: Exception) {}
    }
}

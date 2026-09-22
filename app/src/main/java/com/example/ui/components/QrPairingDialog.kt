package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.IceBlue
import com.example.ui.theme.IceCardDark
import com.example.ui.theme.IceCardStroke
import com.example.ui.theme.IceCyan
import com.example.ui.theme.IceGreen
import com.example.util.QrCodeHelper
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrPairingDialog(
    sheetState: SheetState,
    localDeviceId: String,
    localDeviceName: String,
    localPairingPin: String,
    localIp: String,
    localBatteryLevel: Int,
    currentRole: String,
    onRegeneratePin: () -> Unit,
    onQrScanned: (String) -> Unit,
    onSimulateScanPartner: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = IceCardDark,
        dragHandle = null,
        modifier = Modifier.testTag("qr_pairing_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header with Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = IceCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "ICEPOWER Device Pairing",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_qr_sheet_button")
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White.copy(alpha = 0.7f))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tab Row: My QR Code vs Scan Screen
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF0C1622),
                contentColor = IceCyan,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = IceCyan
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("My QR Code", fontWeight = FontWeight.Bold)
                        }
                    },
                    modifier = Modifier.testTag("my_qr_tab")
                )

                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Scan Screen", fontWeight = FontWeight.Bold)
                        }
                    },
                    modifier = Modifier.testTag("scan_screen_tab")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedTab == 0) {
                // Tab 0: Generated QR Code for Device Pairing
                MyQrCodeView(
                    deviceId = localDeviceId,
                    deviceName = localDeviceName,
                    pairingPin = localPairingPin,
                    ipAddress = localIp,
                    batteryLevel = localBatteryLevel,
                    role = currentRole,
                    onRegeneratePin = onRegeneratePin
                )
            } else {
                // Tab 1: Camera QR Code Scanner with Manual PIN input
                ScannerView(
                    onQrScanned = onQrScanned,
                    onSimulateScanPartner = onSimulateScanPartner
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun MyQrCodeView(
    deviceId: String,
    deviceName: String,
    pairingPin: String,
    ipAddress: String,
    batteryLevel: Int,
    role: String,
    onRegeneratePin: () -> Unit
) {
    val pairingUri = remember(deviceId, deviceName, pairingPin, ipAddress, batteryLevel, role) {
        QrCodeHelper.buildPairingUri(
            deviceId = deviceId,
            deviceName = deviceName,
            pairingPin = pairingPin,
            ipAddress = ipAddress,
            batteryLevel = batteryLevel,
            role = role
        )
    }

    val qrBitmap: Bitmap? = remember(pairingUri) {
        QrCodeHelper.generateQrBitmap(
            content = pairingUri,
            sizePx = 420,
            foregroundColor = 0xFF00E5FF.toInt(),
            backgroundColor = 0xFF0A1420.toInt()
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Display this QR code to the partner phone to establish an authorized connection.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.75f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // High contrast QR Canvas
        Box(
            modifier = Modifier
                .size(230.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF0A1420))
                .border(2.dp, IceCyan.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
                .padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            if (qrBitmap != null) {
                Image(
                    bitmap = qrBitmap.asImageBitmap(),
                    contentDescription = "ICEPOWER Pairing QR Code",
                    modifier = Modifier
                        .size(200.dp)
                        .testTag("pairing_qr_image")
                )
            } else {
                Text("Generating QR Code...", color = IceCyan, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Dynamic 4-Digit Security PIN Box
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1826)),
            border = androidx.compose.foundation.BorderStroke(1.dp, IceCardStroke),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = IceCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "SECURITY PAIRING PIN",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = Color.White.copy(alpha = 0.6f)
                        )
                        Text(
                            text = pairingPin.chunked(1).joinToString(" "),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 4.sp,
                            color = IceCyan
                        )
                    }
                }

                IconButton(
                    onClick = onRegeneratePin,
                    modifier = Modifier.testTag("regenerate_pairing_pin_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Regenerate PIN",
                        tint = IceBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "$deviceName • $ipAddress • Level: $batteryLevel%",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = Color.White.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun ScannerView(
    onQrScanned: (String) -> Unit,
    onSimulateScanPartner: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    var manualPinInput by remember { mutableStateOf("") }
    var isTorchOn by remember { mutableStateOf(false) }
    var cameraControl: androidx.camera.core.CameraControl? by remember { mutableStateOf(null) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!hasCameraPermission) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1B2A)),
                border = androidx.compose.foundation.BorderStroke(1.dp, IceCardStroke),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = IceCyan,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Camera Permission Required",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Scan partner's screen directly to verify and pair securely.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(containerColor = IceCyan, contentColor = Color(0xFF001F26)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("request_camera_permission_button")
                    ) {
                        Text("Grant Camera Access", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            // Live CameraX Viewfinder
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(2.dp, IceCyan, RoundedCornerShape(20.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                        }

                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }

                            val imageAnalysis = ImageAnalysis.Builder()
                                .setTargetResolution(Size(640, 480))
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()

                            val executor = Executors.newSingleThreadExecutor()
                            var isProcessing = false

                            imageAnalysis.setAnalyzer(executor) { imageProxy ->
                                if (!isProcessing) {
                                    val result = decodeImageProxy(imageProxy)
                                    if (result != null) {
                                        isProcessing = true
                                        previewView.post {
                                            onQrScanned(result)
                                        }
                                    }
                                }
                                imageProxy.close()
                            }

                            try {
                                cameraProvider.unbindAll()
                                val camera = cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    imageAnalysis
                                )
                                cameraControl = camera.cameraControl
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }, ContextCompat.getMainExecutor(ctx))

                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Laser scan line animation
                val infiniteTransition = rememberInfiniteTransition(label = "scan_line")
                val scanOffset by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2000, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "scan_offset"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(2.dp)
                        .align(Alignment.TopCenter)
                        .padding(top = (scanOffset * 220).dp)
                        .background(IceCyan)
                )

                // Torch toggle on top-right of viewfinder
                cameraControl?.let { control ->
                    IconButton(
                        onClick = {
                            isTorchOn = !isTorchOn
                            control.enableTorch(isTorchOn)
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                            .testTag("toggle_torch_button")
                    ) {
                        Icon(
                            imageVector = if (isTorchOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                            contentDescription = "Torch",
                            tint = if (isTorchOn) IceCyan else Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Point camera at the partner's ICEPOWER screen",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Manual PIN entry or One-tap Demo Scan
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = manualPinInput,
                onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) manualPinInput = it },
                label = { Text("Enter 4-Digit Partner PIN", fontSize = 11.sp) },
                placeholder = { Text("e.g. 6842", fontSize = 12.sp) },
                leadingIcon = {
                    Icon(Icons.Default.VpnKey, contentDescription = null, tint = IceCyan, modifier = Modifier.size(18.dp))
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (manualPinInput.length == 4) {
                        onQrScanned("icepower://pair?id=manual-pin&name=Partner%20Device&pin=$manualPinInput&ip=192.168.1.120&level=65&role=RECIPIENT")
                    }
                }),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = IceCyan,
                    unfocusedBorderColor = IceCardStroke,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("manual_pin_input")
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    if (manualPinInput.length == 4) {
                        onQrScanned("icepower://pair?id=manual-pin&name=Partner%20Device&pin=$manualPinInput&ip=192.168.1.120&level=65&role=RECIPIENT")
                    }
                },
                enabled = manualPinInput.length == 4,
                colors = ButtonDefaults.buttonColors(containerColor = IceCyan, contentColor = Color(0xFF001F26)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .height(52.dp)
                    .testTag("submit_manual_pin_button")
            ) {
                Text("Pair", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // One-tap Simulation Button for Emulator & Testing
        OutlinedButton(
            onClick = onSimulateScanPartner,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = IceCyan),
            border = androidx.compose.foundation.BorderStroke(1.dp, IceCyan.copy(alpha = 0.7f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .testTag("simulate_partner_scan_button")
        ) {
            Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Test QR Scan with Demo Partner", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun decodeImageProxy(imageProxy: ImageProxy): String? {
    val plane = imageProxy.planes[0]
    val buffer = plane.buffer
    val data = ByteArray(buffer.remaining())
    buffer.get(data)

    val width = imageProxy.width
    val height = imageProxy.height

    return try {
        val source = PlanarYUVLuminanceSource(
            data,
            width,
            height,
            0,
            0,
            width,
            height,
            false
        )
        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
        val reader = MultiFormatReader()
        reader.decode(binaryBitmap).text
    } catch (_: Exception) {
        null
    }
}

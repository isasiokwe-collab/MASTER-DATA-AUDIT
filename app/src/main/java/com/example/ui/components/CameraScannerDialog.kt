package com.example.ui.components

import android.annotation.SuppressLint
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.Executors

enum class ScanMode {
    BARCODE,
    TEXT_OCR
}

@SuppressLint("UnsafeOptInUsageError")
@Composable
fun CameraScannerDialog(
    title: String,
    onCodeScanned: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var scanMode by remember { mutableStateOf(ScanMode.BARCODE) }
    var isTorchOn by remember { mutableStateOf(false) }
    var cameraControl: androidx.camera.core.CameraControl? by remember { mutableStateOf(null) }
    var lastScannedValue by remember { mutableStateOf<String?>(null) }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val barcodeScanner = remember { BarcodeScanning.getClient() }
    val textRecognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            barcodeScanner.close()
            textRecognizer.close()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            color = Color.Black
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Camera Preview
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
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()

                            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                val mediaImage = imageProxy.image
                                if (mediaImage != null) {
                                    val image = InputImage.fromMediaImage(
                                        mediaImage,
                                        imageProxy.imageInfo.rotationDegrees
                                    )

                                    if (scanMode == ScanMode.BARCODE) {
                                        barcodeScanner.process(image)
                                            .addOnSuccessListener { barcodes ->
                                                val firstCode = barcodes.firstOrNull()?.rawValue
                                                if (!firstCode.isNullOrBlank() && lastScannedValue == null) {
                                                    lastScannedValue = firstCode
                                                    onCodeScanned(firstCode)
                                                }
                                            }
                                            .addOnCompleteListener {
                                                imageProxy.close()
                                            }
                                    } else {
                                        textRecognizer.process(image)
                                            .addOnSuccessListener { visionText ->
                                                // Find longest alphanumeric sequence or best candidate tag/serial line
                                                val lines = visionText.textBlocks.flatMap { it.lines }
                                                val candidate = lines
                                                    .map { it.text.trim() }
                                                    .filter { it.length >= 4 && it.any { c -> c.isDigit() } }
                                                    .maxByOrNull { it.length }

                                                if (candidate != null && lastScannedValue == null) {
                                                    lastScannedValue = candidate
                                                    onCodeScanned(candidate)
                                                }
                                            }
                                            .addOnCompleteListener {
                                                imageProxy.close()
                                            }
                                    }
                                } else {
                                    imageProxy.close()
                                }
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
                            } catch (exc: Exception) {
                                exc.printStackTrace()
                            }
                        }, ContextCompat.getMainExecutor(ctx))

                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // High-contrast viewfinder overlay
                Box(
                    modifier = Modifier
                        .size(300.dp, 160.dp)
                        .align(Alignment.Center)
                        .border(4.dp, Color(0xFF38BDF8), RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                )

                // Top control bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(Color(0xCC0A2540))
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Scan $title",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (scanMode == ScanMode.BARCODE) "Point at Barcode or QR Code" else "Point at engraved / printed serial plate",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFE2E8F0)
                            )
                        }

                        Row {
                            // Flashlight Toggle
                            IconButton(
                                onClick = {
                                    isTorchOn = !isTorchOn
                                    cameraControl?.enableTorch(isTorchOn)
                                },
                                modifier = Modifier.testTag("toggle_torch_button")
                            ) {
                                Icon(
                                    imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                    contentDescription = "Toggle Torch",
                                    tint = if (isTorchOn) Color(0xFFFBBF24) else Color.White
                                )
                            }

                            // Close Button
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.testTag("close_scanner_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close Scanner",
                                    tint = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Mode switch buttons (Barcode vs OCR)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ElevatedButton(
                            onClick = { scanMode = ScanMode.BARCODE },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("scan_mode_barcode"),
                            colors = ButtonDefaults.elevatedButtonColors(
                                containerColor = if (scanMode == ScanMode.BARCODE) Color(0xFF0253B3) else Color(0xFF334155),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Barcode/QR", fontWeight = FontWeight.Bold)
                        }

                        ElevatedButton(
                            onClick = { scanMode = ScanMode.TEXT_OCR },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("scan_mode_ocr"),
                            colors = ButtonDefaults.elevatedButtonColors(
                                containerColor = if (scanMode == ScanMode.TEXT_OCR) Color(0xFF0253B3) else Color(0xFF334155),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.TextFields, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Text OCR", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Bottom instructions
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(Color(0xDD0F172A))
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Hold steady. Detection is automatic.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    FilledTonalButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("cancel_scan_button"),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFFE2E8F0),
                            contentColor = Color(0xFF0F172A)
                        )
                    ) {
                        Text("Cancel & Enter Manually", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

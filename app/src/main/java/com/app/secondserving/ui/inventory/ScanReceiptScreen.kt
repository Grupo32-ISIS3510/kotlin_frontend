package com.app.secondserving.ui.inventory

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.app.secondserving.data.ReceiptScanResult
import com.app.secondserving.data.ReceiptScanner
import com.app.secondserving.data.ScannedItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private val GreenDark = Color(0xFF386641)
private val BackgroundColor = Color(0xFFF5F5F0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanReceiptScreen(
    onItemsScanned: (List<ScannedItem>, String?) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var showGalleryPicker by remember { mutableStateOf(false) }
    var imageToProcess by remember { mutableStateOf<Uri?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var scanResult by remember { mutableStateOf<ReceiptScanResult?>(null) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    val scanner = remember { ReceiptScanner(context) }

    // Launcher para seleccionar imagen de galería
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { 
            imageToProcess = it
            showConfirmDialog = false // Reset dialog state
        }
    }

    // Process image when imageToProcess changes
    LaunchedEffect(imageToProcess) {
        imageToProcess?.let { uri ->
            isProcessing = true
            try {
                val result = scanner.scanReceipt(uri)
                scanResult = result
                showConfirmDialog = true
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "Error procesando imagen: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                isProcessing = false
                imageToProcess = null
            }
        }
    }

    // Launcher para permiso de cámara
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    // Solicitar permiso al iniciar
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Escanear Factura",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundColor
                )
            )
        },
        containerColor = BackgroundColor
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (hasCameraPermission) {
                CameraPreview(
                    modifier = Modifier.fillMaxSize(),
                    onImageCaptured = { uri ->
                        imageToProcess = uri
                        showConfirmDialog = false
                    }
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.Gray
                        )
                        Text(
                            "Permiso de cámara requerido",
                            color = Color.Gray,
                            fontSize = 16.sp
                        )
                        Button(onClick = {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }) {
                            Text("Conceder permiso")
                        }
                    }
                }
            }

            // Controles en la parte inferior
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Botón de galería
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    FloatingActionButton(
                        onClick = { showGalleryPicker = true },
                        containerColor = Color.White,
                        contentColor = GreenDark,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = "Seleccionar de galería")
                    }

                    // Botón de capturar
                    FloatingActionButton(
                        onClick = { /* Capturar se hace con el botón grande */ },
                        containerColor = GreenDark,
                        contentColor = Color.White,
                        modifier = Modifier.size(72.dp),
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Capturar")
                    }

                    // Placeholder para balance
                    Spacer(modifier = Modifier.size(56.dp))
                }

                Text(
                    "Apunta a una factura o ticket para escanear los productos",
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp)).padding(8.dp)
                )
            }

            // Loading overlay
            if (isProcessing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.padding(32.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                color = GreenDark,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                "Procesando factura...",
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp
                            )
                            Text(
                                "Extrayendo productos con OCR",
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialog para seleccionar de galería
    if (showGalleryPicker) {
        galleryLauncher.launch("image/*")
        showGalleryPicker = false
    }

    // Dialog de confirmación
    if (showConfirmDialog && scanResult != null) {
        ScanResultDialog(
            result = scanResult!!,
            onConfirm = {
                onItemsScanned(scanResult!!.items, scanResult!!.purchaseDate)
                showConfirmDialog = false
                onNavigateBack()
            },
            onDismiss = {
                showConfirmDialog = false
                scanResult = null
            }
        )
    }
}

@Composable
private fun CameraPreview(
    modifier: Modifier = Modifier,
    onImageCaptured: (Uri) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }

    LaunchedEffect(Unit) {
        val cameraProvider = cameraProviderFuture.get()
        val preview = Preview.Builder().build()

        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .build()

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )
        } catch (e: Exception) {
            Log.e("CameraPreview", "Error binding camera", e)
        }
    }

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }.also { previewView = it }
        },
        update = { view ->
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(view.surfaceProvider)
            }
            // Camera already bound in LaunchedEffect
        },
        modifier = modifier
    )
}

private suspend fun processImage(
    context: Context,
    scanner: ReceiptScanner,
    uri: Uri,
    onItemsScanned: (List<ScannedItem>, String?) -> Unit,
    onResult: (ReceiptScanResult) -> Unit
) {
    try {
        val result = withContext(Dispatchers.IO) {
            scanner.scanReceipt(uri)
        }
        withContext(Dispatchers.Main) {
            onResult(result)
        }
    } catch (e: Exception) {
        withContext(Dispatchers.Main) {
            Toast.makeText(
                context,
                "Error procesando imagen: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}

@Composable
private fun ScanResultDialog(
    result: ReceiptScanResult,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Productos Detectados") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (result.error != null) {
                    Text(
                        result.error,
                        color = MaterialTheme.colorScheme.error
                    )
                } else if (result.items.isEmpty()) {
                    Text("No se detectaron productos. Intenta con otra imagen.")
                } else {
                    Text("Se detectaron ${result.items.size} productos:")
                    result.items.forEach { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    item.name,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        item.category,
                                        color = Color.Gray,
                                        fontSize = 12.sp
                                    )
                                    if (item.price != null) {
                                        Text(
                                            "$${item.price}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (result.purchaseDate != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Fecha de compra: ${result.purchaseDate}",
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = Color.Gray
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Agregar al inventario")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

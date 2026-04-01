package com.app.secondserving.ui.inventory

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.app.secondserving.data.ReceiptScanner
import com.app.secondserving.data.ScannedItem
import kotlinx.coroutines.launch
import java.io.File

private val GreenDark = Color(0xFF386641)
private val BackgroundColor = Color(0xFFF5F5F0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanReceiptScreen(
    onItemsScanned: (List<ScannedItem>, String?) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scanner = remember { ReceiptScanner(context) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var showCamera by remember { mutableStateOf(false) }
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var scannedResult by remember { mutableStateOf<Pair<List<ScannedItem>, String?>?>(null) }

    // Función para procesar la imagen con OCR
    val processImage: (Uri) -> Unit = { uri ->
        scope.launch {
            isProcessing = true
            try {
                val result = scanner.scanReceipt(uri)
                if (result.error != null) {
                    Toast.makeText(context, result.error, Toast.LENGTH_LONG).show()
                } else if (result.items.isNotEmpty()) {
                    scannedResult = Pair(result.items, result.purchaseDate)
                } else {
                    Toast.makeText(context, "No se detectaron productos en la imagen", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error al procesar: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                isProcessing = false
            }
        }
    }

    // Launcher para cámara
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && capturedImageUri != null) {
            processImage(capturedImageUri!!)
        }
    }

    // Launcher para galería
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            processImage(it)
        }
    }

    // Launcher para permiso de cámara
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (isGranted) {
            showCamera = true
        }
    }

    // Solicitar permiso al iniciar
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            showCamera = true
        }
    }

    // Abrir cámara cuando showCamera sea true
    LaunchedEffect(showCamera) {
        if (showCamera && hasCameraPermission) {
            val photoFile = File(context.cacheDir, "receipt_capture.jpg")
            val photoUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                photoFile
            )
            capturedImageUri = photoUri
            cameraLauncher.launch(photoUri)
            showCamera = false
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
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            if (scannedResult != null) {
                // Navigate to review screen with scanned items
                ReviewScannedItemsScreen(
                    scannedItems = scannedResult!!.first,
                    purchaseDate = scannedResult!!.second,
                    onSaveItems = { items, date ->
                        onItemsScanned(items, date)
                    },
                    onNavigateBack = { scannedResult = null }
                )
            } else if (isProcessing) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = GreenDark)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Procesando factura con OCR...", color = GreenDark)
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = GreenDark
                    )

                    Text(
                        "Escanea una factura o ticket",
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp,
                        color = Color(0xFF333333)
                    )

                    Text(
                        "Usa la cámara para extraer los productos automáticamente",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Botones de acción
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Botón galería
                        FloatingActionButton(
                            onClick = { galleryLauncher.launch("image/*") },
                            containerColor = Color.White,
                            contentColor = GreenDark,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Icon(
                                Icons.Default.PhotoLibrary,
                                contentDescription = "Galería",
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        // Botón cámara
                        FloatingActionButton(
                            onClick = {
                                if (hasCameraPermission) {
                                    showCamera = true
                                } else {
                                    permissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            },
                            containerColor = GreenDark,
                            contentColor = Color.White,
                            modifier = Modifier.size(80.dp),
                            shape = CircleShape
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = "Cámara",
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        // Placeholder para balance simétrico
                        Box(modifier = Modifier.size(64.dp))
                    }

                    Text(
                        "Toca el icono central para capturar",
                        color = GreenDark,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }

    // Handle system back button
    BackHandler(enabled = scannedResult != null) {
        scannedResult = null
    }
    BackHandler(enabled = scannedResult == null) {
        onNavigateBack()
    }

    DisposableEffect(Unit) {
        onDispose {
            scanner.close()
        }
    }
}

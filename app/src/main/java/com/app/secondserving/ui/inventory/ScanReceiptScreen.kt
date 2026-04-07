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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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

    // Función para procesar la imagen con OCR
    val processImage: (Uri) -> Unit = { uri ->
        scope.launch {
            isProcessing = true
            try {
                val result = scanner.scanReceipt(uri)
                if (result.error != null) {
                    Toast.makeText(context, result.error, Toast.LENGTH_LONG).show()
                } else if (result.items.isNotEmpty()) {
                    onItemsScanned(result.items, result.purchaseDate)
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
            // Lanzar cámara directamente después de conceder el permiso
            val photoFile = File(context.cacheDir, "receipt_capture.jpg")
            val photoUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                photoFile
            )
            capturedImageUri = photoUri
            cameraLauncher.launch(photoUri)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Escanear Factura",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = GreenDark
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver al formulario", tint = GreenDark)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundColor
                )
            )
        },
        containerColor = BackgroundColor
    ) { padding ->
        if (isProcessing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = GreenDark)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Procesando imagen con OCR...", color = GreenDark)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Botones de acción centrados
                Row(
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Botón galería
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FloatingActionButton(
                            onClick = { galleryLauncher.launch("image/*") },
                            containerColor = Color.White,
                            contentColor = GreenDark,
                            modifier = Modifier.size(80.dp),
                            elevation = FloatingActionButtonDefaults.elevation(4.dp)
                        ) {
                            Icon(
                                Icons.Default.PhotoLibrary,
                                contentDescription = "Galería",
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        Text(
                            text = "Galería",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = GreenDark
                        )
                    }

                    // Botón cámara
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FloatingActionButton(
                            onClick = {
                                if (hasCameraPermission) {
                                    val photoFile = File(context.cacheDir, "receipt_capture.jpg")
                                    val photoUri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        photoFile
                                    )
                                    capturedImageUri = photoUri
                                    cameraLauncher.launch(photoUri)
                                } else {
                                    permissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            },
                            containerColor = GreenDark,
                            contentColor = Color.White,
                            modifier = Modifier.size(96.dp),
                            shape = CircleShape,
                            elevation = FloatingActionButtonDefaults.elevation(6.dp)
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = "Cámara",
                                modifier = Modifier.size(48.dp)
                            )
                        }
                        Text(
                            text = "Cámara",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = GreenDark
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    "Selecciona una imagen de tu factura o ticket",
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
            }
        }
    }

    // Handle system back button
    BackHandler {
        onNavigateBack()
    }

    DisposableEffect(Unit) {
        onDispose {
            scanner.close()
        }
    }
}

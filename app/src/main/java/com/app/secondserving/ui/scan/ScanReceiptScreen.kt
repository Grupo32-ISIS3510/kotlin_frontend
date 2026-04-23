package com.app.secondserving.ui.scan

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.io.File

private val GreenDark = Color(0xFF386641)
private val BackgroundColor = Color(0xFFF5F5F0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanReceiptScreen(
    viewModel: ScanViewModel,
    onItemsScanned: (List<EditableScannedItem>, String?) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var showGalleryPicker by remember { mutableStateOf(false) }
    var triggerCapture by remember { mutableStateOf(false) }

    // Reaccionar a los cambios de estado del ViewModel
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is ScanUiState.Success -> {
                onItemsScanned(state.items, state.purchaseDate)
                viewModel.resetState()
            }
            else -> {}
        }
    }

    // Launcher para seleccionar imagen de galería
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.scanReceipt(it) }
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

    if (showGalleryPicker) {
        LaunchedEffect(Unit) {
            galleryLauncher.launch("image/*")
            showGalleryPicker = false
        }
    }

    // Diálogo de error bloqueante
    if (uiState is ScanUiState.Error) {
        AlertDialog(
            onDismissRequest = { viewModel.resetState() },
            confirmButton = {
                Button(
                    onClick = { viewModel.resetState() },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenDark)
                ) {
                    Text("Aceptar")
                }
            },
            title = {
                Text("Error de Escaneo", fontWeight = FontWeight.Bold, color = GreenDark)
            },
            text = {
                Text(
                    text = (uiState as ScanUiState.Error).message,
                    color = GreenDark
                )
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White
        )
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
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Volver",
                            tint = GreenDark
                        )
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
                    triggerCapture = triggerCapture,
                    onCaptureHandled = { triggerCapture = false },
                    onImageCaptured = { uri ->
                        viewModel.scanReceipt(uri)
                    }
                )
            } else {
                PermissionDeniedState(onGrantPermission = {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                })
            }

            // Controles en la parte inferior
            ScanControls(
                onGalleryClick = { showGalleryPicker = true },
                onCaptureClick = { triggerCapture = true }
            )

            // Loading overlay
            if (uiState is ScanUiState.Processing) {
                LoadingOverlay()
            }
        }
    }
}

@Composable
private fun PermissionDeniedState(onGrantPermission: () -> Unit) {
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
                tint = GreenDark.copy(alpha = 0.5f)
            )
            Text(
                "Permiso de cámara requerido",
                color = GreenDark,
                fontSize = 16.sp
            )
            Button(
                onClick = onGrantPermission,
                colors = ButtonDefaults.buttonColors(containerColor = GreenDark)
            ) {
                Text("Conceder permiso", color = Color.White)
            }
        }
    }
}

@Composable
private fun ScanControls(onGalleryClick: () -> Unit, onCaptureClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FloatingActionButton(
                onClick = onGalleryClick,
                containerColor = Color.White,
                contentColor = GreenDark,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = "Seleccionar de galería")
            }

            FloatingActionButton(
                onClick = onCaptureClick,
                containerColor = GreenDark,
                contentColor = Color.White,
                modifier = Modifier.size(72.dp),
                shape = CircleShape
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = "Capturar")
            }

            Spacer(modifier = Modifier.size(56.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            "Apunta a una factura para escanear productos",
            color = Color.White,
            fontSize = 12.sp,
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .padding(8.dp)
        )
    }
}

@Composable
private fun LoadingOverlay() {
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
                    fontSize = 16.sp,
                    color = GreenDark
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

@Composable
private fun CameraPreview(
    modifier: Modifier = Modifier,
    triggerCapture: Boolean,
    onCaptureHandled: () -> Unit,
    onImageCaptured: (Uri) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .build()
    }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageCapture
                        )
                    } catch (exc: Exception) {
                        Log.e("CameraPreview", "Error al vincular cámara", exc)
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            }
        )
    }

    LaunchedEffect(triggerCapture) {
        if (triggerCapture) {
            val file = File(context.cacheDir, "receipt_${System.currentTimeMillis()}.jpg")
            val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()

            imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        onImageCaptured(Uri.fromFile(file))
                        onCaptureHandled()
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Log.e("CameraPreview", "Error al capturar imagen", exception)
                        onCaptureHandled()
                    }
                }
            )
        }
    }
}

package com.dafusshlosh.carphotos.ui

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.dafusshlosh.carphotos.data.CarPhotoRepository
import com.dafusshlosh.carphotos.ocr.PlateReader
import kotlinx.coroutines.launch

@Composable
fun CaptureScreen(navController: NavHostController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val repo = remember { CarPhotoRepository(context) }
    val scope = rememberCoroutineScope()

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var isPlateMode by remember { mutableStateOf(true) }
    var lastDetectedPlate by remember { mutableStateOf<String?>(null) }
    var statusText by remember { mutableStateOf("צלם את לוחית הרישוי") }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (!hasCameraPermission) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("צריך הרשאת מצלמה כדי לצלם", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))
            Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                Text("אשר גישה למצלמה")
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { navController.popBackStack() }) {
                Text("חזרה")
            }
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.weight(1f),
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = androidx.camera.core.Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val capture = ImageCapture.Builder().build()
                    imageCapture = capture
                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture
                        )
                    } catch (e: Exception) {
                        statusText = "שגיאה בפתיחת המצלמה: ${e.message}"
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            }
        )

        Column(modifier = Modifier.padding(16.dp)) {
            Text(statusText, style = MaterialTheme.typography.titleMedium)
            if (lastDetectedPlate != null) {
                Text("רכב פעיל: ${PlateReader.formatForDisplay(lastDetectedPlate!!)}")
            }
            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        val capture = imageCapture ?: return@Button
                        val name = "CAR_${System.currentTimeMillis()}.jpg"
                        val contentValues = ContentValues().apply {
                            put(MediaStore.Images.Media.DISPLAY_NAME, name)
                            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                        }
                        val outputOptions = ImageCapture.OutputFileOptions.Builder(
                            context.contentResolver,
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            contentValues
                        ).build()

                        val shotIsPlate = isPlateMode
                        capture.takePicture(
                            outputOptions,
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                    val savedUri = output.savedUri?.toString() ?: return
                                    scope.launch {
                                        val plate = repo.onPhotoCaptured(savedUri, shotIsPlate)
                                        if (shotIsPlate) {
                                            if (plate != null) {
                                                lastDetectedPlate = plate
                                                statusText = "לוחית זוהתה — צלם נזקים"
                                                isPlateMode = false
                                            } else {
                                                statusText = "לא זוהתה לוחית, נסה שוב"
                                            }
                                        } else {
                                            statusText = "תמונת נזק נשמרה"
                                        }
                                    }
                                }
                                override fun onError(exception: ImageCaptureException) {
                                    statusText = "שגיאה בצילום, נסה שוב"
                                }
                            }
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isPlateMode) "צלם לוחית" else "צלם נזק")
                }

                OutlinedButton(
                    onClick = {
                        isPlateMode = true
                        lastDetectedPlate = null
                        statusText = "צלם את לוחית הרישוי"
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("רכב חדש")
                }
            }

            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { navController.popBackStack() }) {
                Text("סיום וחזרה למסך הראשי")
            }
        }
    }
}

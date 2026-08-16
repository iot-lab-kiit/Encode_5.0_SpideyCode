package `in`.iot.spidey_code.view.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import `in`.iot.spidey_code.data.model.FilterType
import `in`.iot.spidey_code.data.model.displayName
import `in`.iot.spidey_code.view.components.CameraShutterButton
import `in`.iot.spidey_code.view.components.CameraTopBar
import `in`.iot.spidey_code.view.components.FaceOverlayCanvas
import `in`.iot.spidey_code.view.components.SpideyNetAnimation
import `in`.iot.spidey_code.vm.CameraViewModel
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors

@Composable
fun CameraScreen(
    selectedFilter: FilterType,
    onNavigateToReview: (FilterType, String) -> Unit,
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: CameraViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isPermissionGranted by viewModel.isPermissionGranted.collectAsState()
    val detectedFaces by viewModel.detectedFaces.collectAsState()

    var isCapturing by remember { mutableStateOf(false) }

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.updatePermissionStatus(isGranted)
    }

    LaunchedEffect(Unit) {
        val checkPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        viewModel.updatePermissionStatus(checkPermission)

        if (!checkPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // CameraX + ML Kit Face Detection Lifecycle Binding
    DisposableEffect(lifecycleOwner, isPermissionGranted) {
        if (!isPermissionGranted) return@DisposableEffect onDispose {}

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val cameraExecutor = Executors.newSingleThreadExecutor()

        val detectorOptions = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .build()

        val faceDetector = FaceDetection.getClient(detectorOptions)

        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    @OptIn(ExperimentalGetImage::class)
                    val mediaImage = imageProxy.image
                    if (mediaImage != null) {
                        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                        val inputImage = InputImage.fromMediaImage(mediaImage, rotationDegrees)

                        faceDetector.process(inputImage)
                            .addOnSuccessListener { faces ->
                                viewModel.processDetectedFaces(
                                    faces = faces,
                                    imageWidth = mediaImage.width,
                                    imageHeight = mediaImage.height,
                                    rotationDegrees = rotationDegrees,
                                    previewWidth = previewView.width.toFloat(),
                                    previewHeight = previewView.height.toFloat(),
                                    isFrontCamera = true
                                )
                            }
                            .addOnFailureListener {
                                // Ignore frame error
                            }
                            .addOnCompleteListener {
                                imageProxy.close()
                            }
                    } else {
                        imageProxy.close()
                    }
                }

                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis,
                    imageCapture
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            try {
                val cameraProvider = cameraProviderFuture.get()
                cameraProvider.unbindAll()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            faceDetector.close()
            cameraExecutor.shutdown()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (isPermissionGranted) {
            // 1. Full-Screen Edge-to-Edge Camera Viewport
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )

            // 2. Real-Time Face Detection Overlay Component
            FaceOverlayCanvas(
                faces = detectedFaces,
                modifier = Modifier.fillMaxSize()
            )

            // 3. Top Controls: CameraTopBar Component
            CameraTopBar(
                filterName = selectedFilter.displayName(),
                onBack = onNavigateBack,
                modifier = Modifier.align(Alignment.TopCenter)
            )

            // 4. Bottom Controls: White Camera Shutter Button with Centered Lottie Web Overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    // Classic White Circular Camera Shutter Button (Underneath)
                    CameraShutterButton(
                        isCapturing = isCapturing,
                        onClick = {
                            isCapturing = true
                            val cameraExecutor = Executors.newSingleThreadExecutor()
                            imageCapture.takePicture(
                                cameraExecutor,
                                object : ImageCapture.OnImageCapturedCallback() {
                                    override fun onCaptureSuccess(imageProxy: ImageProxy) {
                                        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                                        val rawBitmap = imageProxy.toBitmap()

                                        val matrix = Matrix().apply {
                                            postRotate(rotationDegrees.toFloat())
                                            postScale(-1f, 1f)
                                        }

                                        val rotatedBitmap = Bitmap.createBitmap(
                                            rawBitmap,
                                            0,
                                            0,
                                            rawBitmap.width,
                                            rawBitmap.height,
                                            matrix,
                                            true
                                        )

                                        imageProxy.close()

                                        val photoFile = File(
                                            context.cacheDir,
                                            "captured_spidey_${System.currentTimeMillis()}.jpg"
                                        )

                                        try {
                                            FileOutputStream(photoFile).use { out ->
                                                rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
                                            }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }

                                        cameraExecutor.shutdown()

                                        ContextCompat.getMainExecutor(context).execute {
                                            isCapturing = false
                                            if (photoFile.exists() && photoFile.length() > 0) {
                                                val fileUri = Uri.fromFile(photoFile).toString()
                                                onNavigateToReview(selectedFilter, fileUri)
                                            }
                                        }
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        exception.printStackTrace()
                                        cameraExecutor.shutdown()
                                        ContextCompat.getMainExecutor(context).execute {
                                            isCapturing = false
                                        }
                                    }
                                }
                            )
                        }
                    )

                    // Transparent Lottie Spider-Web Overlay (Drawn ON TOP of white shutter button)
                    SpideyNetAnimation(
                        modifier = Modifier.size(110.dp)
                    )
                }
            }
        } else {
            // Permission Denied View
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Camera Permission Required",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Please grant camera permission to use real-time face filter features.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedButton(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }
                ) {
                    Text("Grant Camera Permission")
                }
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

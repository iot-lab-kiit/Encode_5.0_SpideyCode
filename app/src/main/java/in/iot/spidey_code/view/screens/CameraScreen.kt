package `in`.iot.spidey_code.view.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.AspectRatio
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import `in`.iot.spidey_code.data.model.FilterType
import `in`.iot.spidey_code.data.model.NormalizedRect
import `in`.iot.spidey_code.data.model.badgeCorner
import `in`.iot.spidey_code.data.model.displayName
import `in`.iot.spidey_code.data.model.frameDefinition
import `in`.iot.spidey_code.data.model.showBrandingOverlay
import `in`.iot.spidey_code.utils.FrameWindowDetector
import `in`.iot.spidey_code.utils.ImageCompositionUtils
import `in`.iot.spidey_code.view.components.CameraControls
import `in`.iot.spidey_code.view.components.CameraTopBar
import `in`.iot.spidey_code.view.components.CameraViewport
import `in`.iot.spidey_code.view.components.FilterCarousel
import `in`.iot.spidey_code.vm.CameraViewModel
import java.util.concurrent.Executors

/**
 * Screen orchestrator for camera view, handling camera permission lifecycle,
 * ML Kit face detection analyzer binding, and high-level component coordination.
 */
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
    val isFrontCamera by viewModel.isFrontCamera.collectAsState()
    val isMaskEnabled by viewModel.isMaskEnabled.collectAsState()

    // Live-selected filter, switchable in-camera via the filter carousel below.
    // Seeded once from the Gear Selection nav argument, then owned by the ViewModel.
    LaunchedEffect(Unit) { viewModel.initializeFilter(selectedFilter) }
    val activeFilter by viewModel.selectedFilter.collectAsState()

    var isCapturing by remember { mutableStateOf(false) }

    val frameDefinition = activeFilter.frameDefinition

    val frameImageBitmap = remember(context, activeFilter) {
        frameDefinition?.assetPath?.let { assetPath ->
            runCatching {
                context.assets.open(assetPath).use { inputStream ->
                    val options = BitmapFactory.Options().apply {
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                        inPremultiplied = true
                    }
                    BitmapFactory.decodeStream(inputStream, null, options)?.asImageBitmap()
                }
            }.getOrNull()
        }
    }

    val normalizedWindow: NormalizedRect? = remember(frameImageBitmap, frameDefinition) {
        if (frameImageBitmap != null) {
            val androidBitmap = frameImageBitmap.asAndroidBitmap()
            FrameWindowDetector.detectTransparentWindow(androidBitmap)
                ?: frameDefinition?.fallbackWindow
        } else {
            frameDefinition?.fallbackWindow
        }
    }

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            setBackgroundColor(android.graphics.Color.BLACK)
        }
    }

    val imageCapture = remember {
        ImageCapture.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
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

    // CameraX + ML Kit Face Detection Lifecycle Binding (re-binds cleanly when camera facing changes)
    DisposableEffect(lifecycleOwner, isPermissionGranted, isFrontCamera) {
        if (!isPermissionGranted) return@DisposableEffect onDispose {}

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val cameraExecutor = Executors.newSingleThreadExecutor()

        val detectorOptions = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .build()

        val faceDetector = FaceDetection.getClient(detectorOptions)

        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder()
                    .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                    .build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setTargetAspectRatio(AspectRatio.RATIO_4_3)
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
                                    isFrontCamera = isFrontCamera
                                )
                            }
                            .addOnFailureListener { }
                            .addOnCompleteListener { imageProxy.close() }
                    } else {
                        imageProxy.close()
                    }
                }

                val cameraSelector = if (isFrontCamera) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }

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
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. Main Frame Poster Viewport (occupies top area)
            CameraViewport(
                isPermissionGranted = isPermissionGranted,
                previewView = previewView,
                detectedFaces = detectedFaces,
                isMaskEnabled = isMaskEnabled,
                frameImageBitmap = frameImageBitmap,
                normalizedWindow = normalizedWindow,
                badgeCorner = activeFilter.badgeCorner,
                showBrandingOverlay = activeFilter.showBrandingOverlay,
                onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                modifier = Modifier.weight(1f)
            )

            // 1b. Snapchat-style filter carousel: swipe or tap to switch the live frame
            // without leaving the camera screen.
            FilterCarousel(
                filters = remember { FilterType.entries.toList() },
                selectedFilter = activeFilter,
                onFilterSelected = { viewModel.selectFilter(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.95f))
                    .padding(vertical = 8.dp)
            )

            // 2. Compact Bottom Control Bar
            CameraControls(
                isMaskEnabled = isMaskEnabled,
                isCapturing = isCapturing,
                onToggleMask = { viewModel.toggleMaskEnabled() },
                onShutterClick = {
                    isCapturing = true
                    val facesSnapshot = detectedFaces.toList()
                    val isMaskEnabledSnapshot = isMaskEnabled
                    val filterSnapshot = activeFilter
                    val cameraExecutor = Executors.newSingleThreadExecutor()

                    imageCapture.takePicture(
                        cameraExecutor,
                        object : ImageCapture.OnImageCapturedCallback() {
                            override fun onCaptureSuccess(imageProxy: ImageProxy) {
                                val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                                val rawBitmap = imageProxy.toBitmap()

                                val matrix = Matrix().apply {
                                    postRotate(rotationDegrees.toFloat())
                                    if (isFrontCamera) {
                                        postScale(-1f, 1f)
                                    }
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

                                val photoFile = ImageCompositionUtils.createComposedPoster(
                                    context = context,
                                    rotatedBitmap = rotatedBitmap,
                                    selectedFilter = filterSnapshot,
                                    isMaskEnabled = isMaskEnabledSnapshot,
                                    facesSnapshot = facesSnapshot,
                                    previewWidth = previewView.width.toFloat(),
                                    previewHeight = previewView.height.toFloat()
                                )

                                cameraExecutor.shutdown()

                                ContextCompat.getMainExecutor(context).execute {
                                    isCapturing = false
                                    if (photoFile != null && photoFile.exists() && photoFile.length() > 0) {
                                        val fileUri = Uri.fromFile(photoFile).toString()
                                        onNavigateToReview(filterSnapshot, fileUri)
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
                },
                onToggleCameraFacing = { viewModel.toggleCameraFacing() }
            )
        }

        // TOPMOST Compose Layer: Floating Islands Top Control Bar
        CameraTopBar(
            filterName = activeFilter.displayName(),
            onBack = onNavigateBack,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

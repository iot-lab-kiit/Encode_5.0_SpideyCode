package `in`.iot.spidey_code.view.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.widget.Toast
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
import androidx.camera.core.UseCase
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
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
import `in`.iot.spidey_code.utils.VideoCompositionUtils
import `in`.iot.spidey_code.view.components.CameraControls
import `in`.iot.spidey_code.view.components.CameraTopBar
import `in`.iot.spidey_code.view.components.CameraViewport
import `in`.iot.spidey_code.view.components.FilterCarousel
import `in`.iot.spidey_code.vm.CameraViewModel
import `in`.iot.spidey_code.vm.FlashMode
import java.io.File
import java.util.concurrent.Executors

/** Max length of a held-to-record clip, matching a short social-video style limit. */
private const val MAX_RECORDING_MS = 15_000L

/**
 * Screen orchestrator for camera view, handling camera permission lifecycle,
 * ML Kit face detection analyzer binding, and high-level component coordination.
 */
@Composable
fun CameraScreen(
    selectedFilter: FilterType,
    onNavigateToReview: (FilterType, String) -> Unit,
    onNavigateToVideoReview: (FilterType, String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CameraViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isPermissionGranted by viewModel.isPermissionGranted.collectAsState()
    val detectedFaces by viewModel.detectedFaces.collectAsState()
    val isFrontCamera by viewModel.isFrontCamera.collectAsState()
    val isMaskEnabled by viewModel.isMaskEnabled.collectAsState()
    val flashMode by viewModel.flashMode.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val recordingElapsedMs by viewModel.recordingElapsedMs.collectAsState()

    // Live-selected filter, switchable in-camera via the filter carousel below.
    // Seeded once from the initial nav argument, then owned by the ViewModel.
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
            // Trades a little shutter latency for CameraX's higher-quality capture pipeline
            // (better noise reduction/processing on most devices) -- worth it for a photobooth
            // shot people will keep, unlike a burst-mode/action-shot use case.
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .build()
    }

    // Auto-stop at the max clip duration, same cap the recording ring animates toward.
    LaunchedEffect(recordingElapsedMs) {
        if (isRecording && recordingElapsedMs >= MAX_RECORDING_MS) {
            viewModel.stopRecordingTimer()
        }
    }

    val recorder = remember {
        Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.HD))
            .build()
    }
    val videoCapture = remember { VideoCapture.withOutput(recorder) }
    var activeRecording by remember { mutableStateOf<Recording?>(null) }
    var isProcessingVideo by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.updatePermissionStatus(isGranted)
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* Video still works without audio if this is denied -- just recorded muted. */ }

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* Pre-API 29 only; see the WRITE_EXTERNAL_STORAGE check below. */ }

    LaunchedEffect(Unit) {
        val checkPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        viewModel.updatePermissionStatus(checkPermission)

        if (!checkPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
        // Scoped storage (and the MediaStore-without-permission exemption for an app's own
        // files) only exists from API 29 onward -- below that, inserting into MediaStore.Video
        // needs this runtime permission or the insert silently fails.
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    // Keep capture flash mode in sync with the toggle in the top bar.
    LaunchedEffect(flashMode) {
        imageCapture.flashMode = when (flashMode) {
            FlashMode.OFF -> ImageCapture.FLASH_MODE_OFF
            FlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO
            FlashMode.ON -> ImageCapture.FLASH_MODE_ON
        }
    }

    fun startRecording() {
        if (activeRecording != null) return

        // Record to a temp cache file first (not straight to the gallery) so the frame
        // decoration can be baked in and the user can review before it's saved anywhere
        // permanent -- same pattern as photos.
        val rawVideoFile = File(context.cacheDir, "raw_spidey_video_${System.currentTimeMillis()}.mp4")
        val outputOptions = FileOutputOptions.Builder(rawVideoFile).build()

        val hasAudioPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        val pending = videoCapture.output.prepareRecording(context, outputOptions).let {
            if (hasAudioPermission) it.withAudioEnabled() else it
        }

        val filterSnapshot = activeFilter
        val isMaskEnabledSnapshot = isMaskEnabled
        val previewWSnapshot = previewView.width.toFloat()
        val previewHSnapshot = previewView.height.toFloat()

        viewModel.startRecordingTimer()
        activeRecording = pending.start(ContextCompat.getMainExecutor(context)) { event ->
            if (event is VideoRecordEvent.Finalize) {
                activeRecording = null
                val faceTimeline = viewModel.getRecordedFaceTimeline()
                viewModel.stopRecordingTimer()
                if (event.hasError()) {
                    Toast.makeText(context, "Recording failed: ${event.cause?.message}", Toast.LENGTH_SHORT).show()
                    return@start
                }

                isProcessingVideo = true
                val framedVideoFile = File(context.cacheDir, "framed_spidey_video_${System.currentTimeMillis()}.mp4")
                VideoCompositionUtils.composeVideoWithFrame(
                    context = context,
                    rawVideoFile = rawVideoFile,
                    selectedFilter = filterSnapshot,
                    isMaskEnabled = isMaskEnabledSnapshot,
                    faceTimeline = faceTimeline,
                    previewWidth = previewWSnapshot,
                    previewHeight = previewHSnapshot,
                    outputFile = framedVideoFile
                ) { composedFile ->
                    isProcessingVideo = false
                    rawVideoFile.delete()
                    if (composedFile != null) {
                        onNavigateToVideoReview(filterSnapshot, Uri.fromFile(composedFile).toString())
                    } else {
                        Toast.makeText(context, "Couldn't process video", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    fun stopRecording() {
        activeRecording?.stop()
    }

    // CameraX + ML Kit Face Detection Lifecycle Binding (re-binds cleanly when camera facing changes)
    DisposableEffect(lifecycleOwner, isPermissionGranted, isFrontCamera) {
        if (!isPermissionGranted) return@DisposableEffect onDispose {}

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val cameraExecutor = Executors.newSingleThreadExecutor()

        val detectorOptions = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .enableTracking()
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

                // Not every device's camera hardware level guarantees four concurrent streams
                // (Preview + ImageAnalysis + ImageCapture + VideoCapture). Try the full set
                // first; if the device can't support it, degrade gracefully rather than losing
                // the whole camera screen -- first dropping face analysis (mask stops working,
                // photo/video still do), then dropping video entirely as a last resort.
                val fullUseCases = arrayOf<UseCase>(preview, imageAnalysis, imageCapture, videoCapture)
                val noAnalysisUseCases = arrayOf<UseCase>(preview, imageCapture, videoCapture)
                val noVideoUseCases = arrayOf<UseCase>(preview, imageAnalysis, imageCapture)

                val attempts = listOf(fullUseCases, noAnalysisUseCases, noVideoUseCases)
                var bound = false
                for (useCases in attempts) {
                    try {
                        cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, *useCases)
                        bound = true
                        break
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                if (!bound) {
                    // Absolute fallback: preview + photo only.
                    runCatching {
                        cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageCapture)
                    }
                }
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
            // 1. Main Frame Poster Viewport (occupies top area). Double-tap flips the
            // camera, mirroring the common gesture shortcut alongside the explicit button.
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
                modifier = Modifier
                    .weight(1f)
                    .pointerInput(isRecording) {
                        if (isRecording) return@pointerInput
                        detectTapGestures(onDoubleTap = { viewModel.toggleCameraFacing() })
                    }
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
                isRecording = isRecording,
                recordingProgress = (recordingElapsedMs.toFloat() / MAX_RECORDING_MS).coerceIn(0f, 1f),
                recordingLabel = "%d:%02d".format((recordingElapsedMs / 1000) / 60, (recordingElapsedMs / 1000) % 60),
                onToggleMask = { viewModel.toggleMaskEnabled() },
                onStartRecording = { startRecording() },
                onStopRecording = { stopRecording() },
                onTapPhoto = {
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
            flashMode = flashMode,
            onCycleFlash = { viewModel.cycleFlashMode() },
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // Baking the frame into the recorded clip takes a few seconds -- block input with a
        // clear "processing" state rather than leaving the screen looking stuck.
        if (isProcessingVideo) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.White)
                    Text(
                        text = "Adding your frame...",
                        color = Color.White,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }
        }
    }
}

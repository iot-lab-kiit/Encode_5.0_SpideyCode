package `in`.iot.spidey_code.vm

import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.RectF
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceLandmark
import `in`.iot.spidey_code.data.model.FilterType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

data class TransformedFaceData(
    val boundingBox: RectF,
    val leftEye: PointF? = null,
    val rightEye: PointF? = null
)

data class TimestampedFaceFrame(
    val timestampUs: Long,
    val faces: List<TransformedFaceData>
)

/** Capture flash mode, independent of any CameraX type so the ViewModel stays framework-free. */
enum class FlashMode {
    OFF, AUTO, ON;

    fun next(): FlashMode = when (this) {
        OFF -> AUTO
        AUTO -> ON
        ON -> OFF
    }
}

private data class SmoothedFaceState(
    var boundingBox: RectF,
    var leftEye: PointF?,
    var rightEye: PointF?,
    var lastSeenAtMs: Long
)

class CameraViewModel : ViewModel() {

    private val _isPermissionGranted = MutableStateFlow(false)
    val isPermissionGranted: StateFlow<Boolean> = _isPermissionGranted.asStateFlow()

    private val _detectedFaces = MutableStateFlow<List<TransformedFaceData>>(emptyList())
    val detectedFaces: StateFlow<List<TransformedFaceData>> = _detectedFaces.asStateFlow()

    private val _capturedBitmap = MutableStateFlow<Bitmap?>(null)
    val capturedBitmap: StateFlow<Bitmap?> = _capturedBitmap.asStateFlow()

    private val _isFrontCamera = MutableStateFlow(true)
    val isFrontCamera: StateFlow<Boolean> = _isFrontCamera.asStateFlow()

    private val _isMaskEnabled = MutableStateFlow(false)
    val isMaskEnabled: StateFlow<Boolean> = _isMaskEnabled.asStateFlow()

    private val _flashMode = MutableStateFlow(FlashMode.OFF)
    val flashMode: StateFlow<FlashMode> = _flashMode.asStateFlow()

    // Live-selected filter/frame, switchable in-camera via the Snapchat-style filter
    // carousel without leaving the screen. Seeded once from the initial nav
    // argument (see initializeFilter), then owned entirely by this ViewModel.
    private val _selectedFilter = MutableStateFlow(FilterType.CLASSIC_MASK)
    val selectedFilter: StateFlow<FilterType> = _selectedFilter.asStateFlow()
    private var filterInitialized = false

    // Hold-to-record video state (Snapchat-style: tap = photo, press & hold = video).
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingElapsedMs = MutableStateFlow(0L)
    val recordingElapsedMs: StateFlow<Long> = _recordingElapsedMs.asStateFlow()

    private var recordingTimerJob: Job? = null
    private var recordingStartNanoTime: Long = 0L
    private val _recordedFaceTimeline = mutableListOf<TimestampedFaceFrame>()

    // Per-tracked-face smoothing state for the live mask overlay (see processDetectedFaces).
    private val smoothedFaces = mutableMapOf<Int, SmoothedFaceState>()

    fun initializeFilter(filter: FilterType) {
        if (!filterInitialized) {
            _selectedFilter.value = filter
            filterInitialized = true
        }
    }

    fun selectFilter(filter: FilterType) {
        _selectedFilter.value = filter
    }

    fun cycleFlashMode() {
        _flashMode.value = _flashMode.value.next()
    }

    fun startRecordingTimer() {
        _isRecording.value = true
        _recordingElapsedMs.value = 0L
        recordingStartNanoTime = System.nanoTime()
        synchronized(_recordedFaceTimeline) {
            _recordedFaceTimeline.clear()
            _recordedFaceTimeline.add(TimestampedFaceFrame(0L, _detectedFaces.value))
        }
        recordingTimerJob?.cancel()
        recordingTimerJob = viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            while (isActive) {
                _recordingElapsedMs.value = System.currentTimeMillis() - startTime
                delay(30)
            }
        }
    }

    fun stopRecordingTimer() {
        _isRecording.value = false
        recordingTimerJob?.cancel()
        recordingTimerJob = null
        _recordingElapsedMs.value = 0L
        if (recordingStartNanoTime > 0L) {
            val finalUs = (System.nanoTime() - recordingStartNanoTime) / 1000L
            synchronized(_recordedFaceTimeline) {
                _recordedFaceTimeline.add(TimestampedFaceFrame(finalUs, _detectedFaces.value))
            }
        }
    }

    fun getRecordedFaceTimeline(): List<TimestampedFaceFrame> {
        return synchronized(_recordedFaceTimeline) {
            _recordedFaceTimeline.toList()
        }
    }

    fun updatePermissionStatus(isGranted: Boolean) {
        _isPermissionGranted.value = isGranted
        if (!isGranted) {
            _detectedFaces.value = emptyList()
        }
    }

    fun toggleCameraFacing() {
        _isFrontCamera.value = !_isFrontCamera.value
        smoothedFaces.clear()
    }

    fun toggleMaskEnabled() {
        _isMaskEnabled.value = !_isMaskEnabled.value
    }

    fun setMaskEnabled(enabled: Boolean) {
        _isMaskEnabled.value = enabled
    }

    fun onImageCaptured(bitmap: Bitmap) {
        _capturedBitmap.value = bitmap
    }

    fun clearCapturedImage() {
        _capturedBitmap.value = null
    }

    /**
     * Unified transformation function mapping ML Kit output coordinates (which are already
     * in the rotation-aware coordinate space defined by rotationDegrees) to PreviewView screen
     * coordinates, taking into account aspect-fill scaling, center-crop offset, and front-camera mirroring.
     */
    fun transformPoint(
        pointX: Float,
        pointY: Float,
        imageWidth: Int,
        imageHeight: Int,
        rotationDegrees: Int,
        previewWidth: Float,
        previewHeight: Float,
        isFrontCamera: Boolean
    ): PointF {
        // ML Kit already rotates the image internally when provided rotationDegrees in InputImage.
        // Therefore, output coordinates are already in rotated image space (rotWidth x rotHeight).
        val rotWidth = if (rotationDegrees == 90 || rotationDegrees == 270) imageHeight.toFloat() else imageWidth.toFloat()
        val rotHeight = if (rotationDegrees == 90 || rotationDegrees == 270) imageWidth.toFloat() else imageHeight.toFloat()

        // 1. Aspect-fill scaling & center crop offset calculation
        val scale = max(previewWidth / rotWidth, previewHeight / rotHeight)
        val scaledWidth = rotWidth * scale
        val scaledHeight = rotHeight * scale
        val offsetX = (scaledWidth - previewWidth) / 2f
        val offsetY = (scaledHeight - previewHeight) / 2f

        // 2. Screen coordinate mapping with front-camera horizontal mirroring
        val scaledX = pointX * scale - offsetX
        val scaledY = pointY * scale - offsetY

        val screenX = if (isFrontCamera) {
            previewWidth - scaledX
        } else {
            scaledX
        }
        val screenY = scaledY

        return PointF(screenX, screenY)
    }

    /**
     * Processes detected ML Kit faces by transforming landmark and bounding box coordinates
     * through the unified transformation pipeline, then applies per-face exponential smoothing
     * (keyed by ML Kit's tracking ID) to remove frame-to-frame jitter in the live mask overlay,
     * and briefly holds a face's last known position through momentary detection dropouts
     * (blinks, quick head turns) instead of letting the mask flicker off for a single missed frame.
     */
    fun processDetectedFaces(
        faces: List<Face>,
        imageWidth: Int,
        imageHeight: Int,
        rotationDegrees: Int,
        previewWidth: Float,
        previewHeight: Float,
        isFrontCamera: Boolean = true
    ) {
        if (previewWidth <= 0f || previewHeight <= 0f || imageWidth <= 0 || imageHeight <= 0) {
            _detectedFaces.value = emptyList()
            return
        }

        val now = System.currentTimeMillis()
        val seenIds = mutableSetOf<Int>()
        val transformedList = mutableListOf<TransformedFaceData>()

        for (face in faces) {
            val rawBox = face.boundingBox

            // Transform bounding box corners through the exact same pipeline
            val topLeft = transformPoint(
                rawBox.left.toFloat(), rawBox.top.toFloat(),
                imageWidth, imageHeight, rotationDegrees, previewWidth, previewHeight, isFrontCamera
            )
            val bottomRight = transformPoint(
                rawBox.right.toFloat(), rawBox.bottom.toFloat(),
                imageWidth, imageHeight, rotationDegrees, previewWidth, previewHeight, isFrontCamera
            )

            val boxRect = RectF(
                min(topLeft.x, bottomRight.x),
                min(topLeft.y, bottomRight.y),
                max(topLeft.x, bottomRight.x),
                max(topLeft.y, bottomRight.y)
            )

            // Extract & transform ML Kit eye landmark points
            val rawLeftEye = face.getLandmark(FaceLandmark.LEFT_EYE)?.position
            val rawRightEye = face.getLandmark(FaceLandmark.RIGHT_EYE)?.position

            val screenLeftEye = rawLeftEye?.let {
                transformPoint(it.x, it.y, imageWidth, imageHeight, rotationDegrees, previewWidth, previewHeight, isFrontCamera)
            }
            val screenRightEye = rawRightEye?.let {
                transformPoint(it.x, it.y, imageWidth, imageHeight, rotationDegrees, previewWidth, previewHeight, isFrontCamera)
            }

            // Ensure finalLeftEye is always the point with smaller screen X (screen left)
            // and finalRightEye is always the point with larger screen X (screen right)
            val (finalLeftEye, finalRightEye) = if (screenLeftEye != null && screenRightEye != null) {
                if (screenLeftEye.x <= screenRightEye.x) {
                    screenLeftEye to screenRightEye
                } else {
                    screenRightEye to screenLeftEye
                }
            } else {
                screenLeftEye to screenRightEye
            }

            val trackingId = face.trackingId
            val result = if (trackingId == null) {
                // No stable identity available (tracking disabled or lost) -- pass through raw.
                TransformedFaceData(boxRect, finalLeftEye, finalRightEye)
            } else {
                seenIds += trackingId
                val prior = smoothedFaces[trackingId]
                val smoothedBox = if (prior == null) boxRect else lerpRect(prior.boundingBox, boxRect, SMOOTHING_FACTOR)
                val smoothedLeftEye = lerpPoint(prior?.leftEye, finalLeftEye, SMOOTHING_FACTOR)
                val smoothedRightEye = lerpPoint(prior?.rightEye, finalRightEye, SMOOTHING_FACTOR)
                smoothedFaces[trackingId] = SmoothedFaceState(smoothedBox, smoothedLeftEye, smoothedRightEye, now)
                TransformedFaceData(smoothedBox, smoothedLeftEye, smoothedRightEye)
            }

            transformedList.add(result)

            Log.d(
                "FaceTransform",
                "rotation=$rotationDegrees, img=${imageWidth}x${imageHeight}, prev=${previewWidth}x${previewHeight}, " +
                        "trackingId=$trackingId | rawL=$rawLeftEye, rawR=$rawRightEye | finalL=$finalLeftEye, finalR=$finalRightEye"
            )
        }

        // Grace period: keep a briefly-missed tracked face's last known position alive for a
        // short window so the mask doesn't flicker off for a single dropped detection frame.
        smoothedFaces.forEach { (id, state) ->
            if (id !in seenIds && now - state.lastSeenAtMs < STALE_FACE_TIMEOUT_MS) {
                transformedList.add(TransformedFaceData(state.boundingBox, state.leftEye, state.rightEye))
            }
        }
        smoothedFaces.keys.retainAll { id -> now - (smoothedFaces[id]?.lastSeenAtMs ?: 0L) < STALE_FACE_TIMEOUT_MS }

        _detectedFaces.value = transformedList

        if (_isRecording.value && recordingStartNanoTime > 0L) {
            val elapsedUs = (System.nanoTime() - recordingStartNanoTime) / 1000L
            synchronized(_recordedFaceTimeline) {
                _recordedFaceTimeline.add(TimestampedFaceFrame(elapsedUs, transformedList))
            }
        }
    }

    private companion object {
        /** Weight given to each new sample; lower = smoother but more lag. */
        const val SMOOTHING_FACTOR = 0.35f

        /** How long to keep showing a tracked face's last known position after it briefly drops out. */
        const val STALE_FACE_TIMEOUT_MS = 250L

        fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

        fun lerpPoint(from: PointF?, to: PointF?, t: Float): PointF? {
            if (to == null) return null
            if (from == null) return to
            return PointF(lerp(from.x, to.x, t), lerp(from.y, to.y, t))
        }

        fun lerpRect(from: RectF, to: RectF, t: Float): RectF = RectF(
            lerp(from.left, to.left, t),
            lerp(from.top, to.top, t),
            lerp(from.right, to.right, t),
            lerp(from.bottom, to.bottom, t)
        )
    }
}

package `in`.iot.spidey_code.vm

import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.RectF
import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceLandmark
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.max
import kotlin.math.min

data class TransformedFaceData(
    val boundingBox: RectF,
    val leftEye: PointF? = null,
    val rightEye: PointF? = null
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

    fun updatePermissionStatus(isGranted: Boolean) {
        _isPermissionGranted.value = isGranted
        if (!isGranted) {
            _detectedFaces.value = emptyList()
        }
    }

    fun toggleCameraFacing() {
        _isFrontCamera.value = !_isFrontCamera.value
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
     * through the unified transformation pipeline.
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

        val transformedList = faces.map { face ->
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

            Log.d(
                "FaceTransform",
                "rotation=$rotationDegrees, img=${imageWidth}x${imageHeight}, prev=${previewWidth}x${previewHeight} | " +
                        "rawL=$rawLeftEye, rawR=$rawRightEye | finalL=$finalLeftEye, finalR=$finalRightEye"
            )

            TransformedFaceData(
                boundingBox = boxRect,
                leftEye = finalLeftEye,
                rightEye = finalRightEye
            )
        }

        _detectedFaces.value = transformedList
    }
}




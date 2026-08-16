package `in`.iot.spidey_code.vm

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.lifecycle.ViewModel
import com.google.mlkit.vision.face.Face
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.max

class CameraViewModel : ViewModel() {

    private val _isPermissionGranted = MutableStateFlow(false)
    val isPermissionGranted: StateFlow<Boolean> = _isPermissionGranted.asStateFlow()

    private val _detectedFaces = MutableStateFlow<List<RectF>>(emptyList())
    val detectedFaces: StateFlow<List<RectF>> = _detectedFaces.asStateFlow()

    private val _capturedBitmap = MutableStateFlow<Bitmap?>(null)
    val capturedBitmap: StateFlow<Bitmap?> = _capturedBitmap.asStateFlow()

    fun updatePermissionStatus(isGranted: Boolean) {
        _isPermissionGranted.value = isGranted
        if (!isGranted) {
            _detectedFaces.value = emptyList()
        }
    }

    fun onImageCaptured(bitmap: Bitmap) {
        _capturedBitmap.value = bitmap
    }

    fun clearCapturedImage() {
        _capturedBitmap.value = null
    }

    /**
     * Transforms ML Kit face bounding boxes into PreviewView screen coordinates,
     * taking into account sensor rotation, aspect-ratio crop scaling, and front-camera mirroring.
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

        val rotatedWidth = if (rotationDegrees == 90 || rotationDegrees == 270) imageHeight.toFloat() else imageWidth.toFloat()
        val rotatedHeight = if (rotationDegrees == 90 || rotationDegrees == 270) imageWidth.toFloat() else imageHeight.toFloat()

        val scale = max(previewWidth / rotatedWidth, previewHeight / rotatedHeight)
        val scaledWidth = rotatedWidth * scale
        val scaledHeight = rotatedHeight * scale
        val offsetX = (scaledWidth - previewWidth) / 2f
        val offsetY = (scaledHeight - previewHeight) / 2f

        val transformedList = faces.map { face ->
            val box = face.boundingBox

            val left: Float
            val right: Float
            if (isFrontCamera) {
                left = previewWidth - (box.right.toFloat() * scale - offsetX)
                right = previewWidth - (box.left.toFloat() * scale - offsetX)
            } else {
                left = box.left.toFloat() * scale - offsetX
                right = box.right.toFloat() * scale - offsetX
            }

            val top = box.top.toFloat() * scale - offsetY
            val bottom = box.bottom.toFloat() * scale - offsetY

            RectF(left, top, right, bottom)
        }

        _detectedFaces.value = transformedList
    }
}

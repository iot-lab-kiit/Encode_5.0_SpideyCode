package `in`.iot.spidey_code.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.util.Size
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.MatrixTransformation
import androidx.media3.effect.OverlayEffect
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import `in`.iot.spidey_code.data.model.FilterType
import `in`.iot.spidey_code.view.components.SpideyMaskReference
import `in`.iot.spidey_code.vm.TimestampedFaceFrame
import `in`.iot.spidey_code.vm.TransformedFaceData
import com.google.common.collect.ImmutableList
import java.io.File
import kotlin.math.atan2
import kotlin.math.max

/**
 * Bakes a filter's Spider-Man face mask (if enabled) and frame decoration (border art + branding overlay)
 * into a recorded video file, cropping/repositioning the raw footage into the frame's photo window exactly like
 * ImageCompositionUtils does for a single photo -- so a saved/shared video matches what the camera preview showed.
 */
object VideoCompositionUtils {

    private val HIGH_QUALITY_PAINT = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)

    /**
     * A static (time-invariant) GL matrix transformation that cover-crops the input video into
     * [window] (a pixel rect within an [outputWidth] x [outputHeight] canvas), leaving the rest
     * of that canvas blank so a frame overlay drawn on top can fill it in. Any part of the
     * cover-cropped video that spills outside the window is harmless -- it's covered by the
     * frame's own opaque art everywhere except the window itself.
     */
    private class WindowPlacementTransformation(
        private val outputWidth: Int,
        private val outputHeight: Int,
        private val windowLeft: Int,
        private val windowTop: Int,
        private val windowWidth: Int,
        private val windowHeight: Int
    ) : MatrixTransformation {

        override fun configure(inputWidth: Int, inputHeight: Int): Size {
            val safeInputW = if (inputWidth <= 0) outputWidth else inputWidth
            val safeInputH = if (inputHeight <= 0) outputHeight else inputHeight

            // Cover-crop scale: at least as big as the window in both dimensions, cropping
            // whatever overflows (which the frame overlay hides anyway).
            val coverScale = max(
                windowWidth.toFloat() / safeInputW,
                windowHeight.toFloat() / safeInputH
            )

            val displayedWidthNdc = (coverScale * safeInputW) / outputWidth
            val displayedHeightNdc = (coverScale * safeInputH) / outputHeight

            val windowCenterXFraction = (windowLeft + windowWidth / 2f) / outputWidth
            val windowCenterYFraction = (windowTop + windowHeight / 2f) / outputHeight
            // Image-space [0,1] (Y down) -> GL NDC [-1,1] (Y up).
            val centerXNdc = -1f + 2f * windowCenterXFraction
            val centerYNdc = 1f - 2f * windowCenterYFraction

            cachedMatrix = Matrix().apply {
                setScale(displayedWidthNdc, displayedHeightNdc)
                postTranslate(centerXNdc, centerYNdc)
            }
            return Size(outputWidth, outputHeight)
        }

        private var cachedMatrix: Matrix = Matrix()

        override fun getMatrix(presentationTimeUs: Long): Matrix = cachedMatrix
    }

    /**
     * Dynamic overlay that composites the Spider-Man face mask at continuous, interpolated face coordinates
     * per video frame timestamp, layered beneath the static frame border and branding overlay.
     */
    private class DynamicMaskAndFrameOverlay(
        private val staticDecorationBitmap: Bitmap,
        private val windowRect: Rect,
        private val maskBitmap: Bitmap?,
        private val isMaskEnabled: Boolean,
        private val faceTimeline: List<TimestampedFaceFrame>,
        private val previewWidth: Float,
        private val previewHeight: Float
    ) : BitmapOverlay() {

        private val outputWidth = staticDecorationBitmap.width
        private val outputHeight = staticDecorationBitmap.height
        private val compositeBitmap = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
        private val compositeCanvas = Canvas(compositeBitmap)

        override fun getBitmap(presentationTimeUs: Long): Bitmap {
            compositeCanvas.drawColor(android.graphics.Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)

            // 1. Draw Spider-Man face mask(s) at interpolated face positions for this presentation timestamp
            if (isMaskEnabled && maskBitmap != null && previewWidth > 0f && previewHeight > 0f && faceTimeline.isNotEmpty()) {
                val faces = getInterpolatedFaces(presentationTimeUs)
                if (faces.isNotEmpty()) {
                    val windowW = windowRect.width().toFloat()
                    val windowH = windowRect.height().toFloat()

                    for (faceData in faces) {
                        val box = faceData.boundingBox
                        val leftEye = faceData.leftEye
                        val rightEye = faceData.rightEye

                        if (leftEye != null && rightEye != null) {
                            val normEyeX = (leftEye.x + rightEye.x) / 2f / previewWidth
                            val normEyeY = (leftEye.y + rightEye.y) / 2f / previewHeight

                            val normDx = (rightEye.x - leftEye.x) / previewWidth
                            val normDy = (rightEye.y - leftEye.y) / previewHeight

                            val normFaceWidth = box.width() / previewWidth

                            val eyeCenterX = windowRect.left + (normEyeX * windowW)
                            val eyeCenterY = windowRect.top + (normEyeY * windowH)

                            val dx = normDx * windowW
                            val dy = normDy * windowH

                            val detectedEyeAngleDeg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                            val faceWidthOnComposite = normFaceWidth * windowW
                            val maskScale = faceWidthOnComposite / SpideyMaskReference.referenceMaskFaceWidth
                            val deltaAngleDeg = detectedEyeAngleDeg - SpideyMaskReference.maskEyeAngleDeg

                            compositeCanvas.save()
                            compositeCanvas.translate(eyeCenterX, eyeCenterY)
                            compositeCanvas.rotate(deltaAngleDeg)
                            compositeCanvas.scale(maskScale, maskScale)
                            compositeCanvas.drawBitmap(
                                maskBitmap,
                                -SpideyMaskReference.maskEyeCenter.x,
                                -SpideyMaskReference.maskEyeCenter.y,
                                HIGH_QUALITY_PAINT
                            )
                            compositeCanvas.restore()
                        }
                    }
                }
            }

            // 2. Draw static frame decoration (border art + branding overlay) over the mask & video
            compositeCanvas.drawBitmap(staticDecorationBitmap, 0f, 0f, HIGH_QUALITY_PAINT)
            return compositeBitmap
        }

        private fun getInterpolatedFaces(targetUs: Long): List<TransformedFaceData> {
            if (faceTimeline.isEmpty()) return emptyList()
            if (targetUs <= faceTimeline.first().timestampUs) return faceTimeline.first().faces
            if (targetUs >= faceTimeline.last().timestampUs) {
                return if (targetUs - faceTimeline.last().timestampUs <= 350_000L) {
                    faceTimeline.last().faces
                } else emptyList()
            }

            var low = 0
            var high = faceTimeline.size - 1
            while (low <= high) {
                val mid = (low + high) ushr 1
                val midVal = faceTimeline[mid].timestampUs
                if (midVal <= targetUs) {
                    if (mid == faceTimeline.size - 1 || faceTimeline[mid + 1].timestampUs > targetUs) {
                        val f1 = faceTimeline[mid]
                        val f2 = faceTimeline[mid + 1]
                        val duration = f2.timestampUs - f1.timestampUs
                        if (duration > 500_000L) {
                            return if (targetUs - f1.timestampUs <= 250_000L) f1.faces else emptyList()
                        }
                        if (duration <= 0L || f1.faces.size != f2.faces.size || f1.faces.isEmpty()) {
                            return if (targetUs - f1.timestampUs < f2.timestampUs - targetUs) f1.faces else f2.faces
                        }
                        val t = (targetUs - f1.timestampUs).toFloat() / duration.toFloat()
                        return f1.faces.indices.map { idx ->
                            val face1 = f1.faces[idx]
                            val face2 = f2.faces[idx]
                            TransformedFaceData(
                                boundingBox = lerpRect(face1.boundingBox, face2.boundingBox, t),
                                leftEye = lerpPoint(face1.leftEye, face2.leftEye, t),
                                rightEye = lerpPoint(face1.rightEye, face2.rightEye, t)
                            )
                        }
                    }
                    low = mid + 1
                } else {
                    high = mid - 1
                }
            }
            return emptyList()
        }

        private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

        private fun lerpPoint(from: PointF?, to: PointF?, t: Float): PointF? {
            if (to == null) return from
            if (from == null) return to
            return PointF(lerp(from.x, to.x, t), lerp(from.y, to.y, t))
        }

        private fun lerpRect(from: RectF, to: RectF, t: Float): RectF = RectF(
            lerp(from.left, to.left, t),
            lerp(from.top, to.top, t),
            lerp(from.right, to.right, t),
            lerp(from.bottom, to.bottom, t)
        )
    }

    /**
     * Re-encodes [rawVideoFile] with the selected filter's frame decoration and Spider-Man face mask (if enabled)
     * baked in, writing the result to [outputFile]. Runs the Media3 Transformer export and reports success/failure
     * via [onResult] on the calling (main) thread once finished.
     */
    fun composeVideoWithFrame(
        context: Context,
        rawVideoFile: File,
        selectedFilter: FilterType,
        isMaskEnabled: Boolean,
        faceTimeline: List<TimestampedFaceFrame>,
        previewWidth: Float,
        previewHeight: Float,
        outputFile: File,
        onResult: (File?) -> Unit
    ) {
        val decoration = ImageCompositionUtils.buildFrameDecorationOverlay(context, selectedFilter)
        if (decoration == null) {
            // No frame art for this filter (shouldn't normally happen) -- fall back to the raw clip.
            rawVideoFile.copyTo(outputFile, overwrite = true)
            onResult(outputFile)
            return
        }
        val (decorationBitmap, windowRect) = decoration

        val maskBitmap = if (isMaskEnabled) {
            runCatching {
                context.assets.open("masks/spidey_mask.png").use { inputStream ->
                    val options = BitmapFactory.Options().apply {
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                        inPremultiplied = true
                    }
                    BitmapFactory.decodeStream(inputStream, null, options)
                }
            }.getOrNull()
        } else null

        val (videoWidth, videoHeight) = readDisplayDimensions(rawVideoFile)

        val placementTransformation = WindowPlacementTransformation(
            outputWidth = decorationBitmap.width,
            outputHeight = decorationBitmap.height,
            windowLeft = windowRect.left,
            windowTop = windowRect.top,
            windowWidth = windowRect.width(),
            windowHeight = windowRect.height()
        )
        // Prime the cached matrix with the real source dimensions before the pipeline starts
        placementTransformation.configure(videoWidth, videoHeight)

        val dynamicOverlay = DynamicMaskAndFrameOverlay(
            staticDecorationBitmap = decorationBitmap,
            windowRect = windowRect,
            maskBitmap = maskBitmap,
            isMaskEnabled = isMaskEnabled,
            faceTimeline = faceTimeline,
            previewWidth = previewWidth,
            previewHeight = previewHeight
        )

        val overlayEffect = OverlayEffect(
            ImmutableList.of(dynamicOverlay)
        )

        val editedMediaItem = EditedMediaItem.Builder(MediaItem.fromUri(Uri.fromFile(rawVideoFile)))
            .setEffects(Effects(emptyList(), listOf(placementTransformation, overlayEffect)))
            .build()

        val transformer = Transformer.Builder(context)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    onResult(if (outputFile.exists() && outputFile.length() > 0) outputFile else null)
                }

                override fun onError(composition: Composition, exportResult: ExportResult, exception: ExportException) {
                    exception.printStackTrace()
                    onResult(null)
                }
            })
            .build()

        transformer.start(editedMediaItem, outputFile.absolutePath)
    }

    /**
     * Reads a video's display-oriented width/height (i.e. swapped if the file's rotation
     * metadata is 90/270, matching how it actually appears the right way up on screen).
     */
    private fun readDisplayDimensions(videoFile: File): Pair<Int, Int> {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(videoFile.absolutePath)
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            if (rotation == 90 || rotation == 270) height to width else width to height
        } catch (e: Exception) {
            e.printStackTrace()
            0 to 0
        } finally {
            runCatching { retriever.release() }
        }
    }
}

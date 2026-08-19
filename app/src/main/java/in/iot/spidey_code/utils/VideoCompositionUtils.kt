package `in`.iot.spidey_code.utils

import android.content.Context
import android.graphics.Matrix
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
import com.google.common.collect.ImmutableList
import java.io.File
import kotlin.math.max

/**
 * Bakes a filter's frame decoration (border art + branding overlay) into a recorded video file,
 * cropping/repositioning the raw footage into the frame's photo window exactly like
 * ImageCompositionUtils does for a single photo -- so a saved/shared video matches what the
 * camera preview showed. The face mask is intentionally NOT baked in here (it would need
 * frame-by-frame face-tracking synced into the re-encode, a much larger undertaking); the mask
 * remains a live-preview-only feature.
 */
object VideoCompositionUtils {

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
     * Re-encodes [rawVideoFile] with the selected filter's frame decoration baked in, writing
     * the result to [outputFile]. Runs the Media3 Transformer export and reports success/failure
     * via [onResult] on the calling (main) thread once finished.
     */
    fun composeVideoWithFrame(
        context: Context,
        rawVideoFile: File,
        selectedFilter: FilterType,
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
        // (configure() is normally called by the frame processor itself, but doing it once up
        // front here guarantees a sane matrix even if configure() is invoked with a placeholder).
        placementTransformation.configure(videoWidth, videoHeight)

        val overlayEffect = OverlayEffect(
            ImmutableList.of(BitmapOverlay.createStaticBitmapOverlay(decorationBitmap))
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

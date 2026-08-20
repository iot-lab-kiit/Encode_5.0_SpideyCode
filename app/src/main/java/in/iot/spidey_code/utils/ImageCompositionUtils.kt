package `in`.iot.spidey_code.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.util.Log
import `in`.iot.spidey_code.data.model.BadgeCorner
import `in`.iot.spidey_code.data.model.FilterType
import `in`.iot.spidey_code.data.model.NormalizedRect
import `in`.iot.spidey_code.data.model.badgeCorner
import `in`.iot.spidey_code.data.model.frameAssetPath
import `in`.iot.spidey_code.data.model.frameDefinition
import `in`.iot.spidey_code.data.model.showBrandingOverlay
import `in`.iot.spidey_code.view.components.SpideyMaskReference
import `in`.iot.spidey_code.vm.TransformedFaceData
import java.io.File
import java.io.FileOutputStream
import kotlin.math.atan2

/**
 * Encapsulates high-resolution poster bitmap composition including photo center-cropping,
 * normalized face mask positioning, and photobooth frame overlay rendering.
 */
object ImageCompositionUtils {

    /**
     * Bilinear-filtered, anti-aliased, dithered -- used for every scaled drawBitmap call in this
     * file (photo crop-into-window, mask, logos, badge). Without FILTER_BITMAP_FLAG, Android
     * falls back to nearest-neighbor sampling when a bitmap is drawn at a different size than its
     * source, which is the main cause of the composited photo looking soft/blocky compared to
     * the raw camera capture.
     */
    private val HIGH_QUALITY_PAINT = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)

    /**
     * Composites captured photo, face mask (if enabled), and selected frame overlay into a single
     * high-resolution poster bitmap matching the native frame asset resolution.
     */
    fun createComposedPoster(
        context: Context,
        rotatedBitmap: Bitmap,
        selectedFilter: FilterType,
        isMaskEnabled: Boolean,
        facesSnapshot: List<TransformedFaceData>,
        previewWidth: Float,
        previewHeight: Float
    ): File? {
        val rawFrameBitmap = decodeFrameBitmap(context, selectedFilter)

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

        val finalBitmap = if (rawFrameBitmap != null) {
            val frameW = rawFrameBitmap.width
            val frameH = rawFrameBitmap.height

            val windowRect = resolveWindowRect(selectedFilter, rawFrameBitmap, frameW, frameH)

            val composite = Bitmap.createBitmap(frameW, frameH, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(composite)

            // 1. Draw photo center-cropped into normalized window rect (FILL_CENTER equivalent)
            val photoW = rotatedBitmap.width.toFloat()
            val photoH = rotatedBitmap.height.toFloat()
            val winW = windowRect.width().toFloat()
            val winH = windowRect.height().toFloat()

            if (photoW > 0f && photoH > 0f && winW > 0f && winH > 0f) {
                val photoRatio = photoW / photoH
                val winRatio = winW / winH

                val srcRect: Rect = if (photoRatio > winRatio) {
                    val cropW = (photoH * winRatio).toInt()
                    val left = ((photoW - cropW) / 2f).toInt()
                    Rect(left, 0, left + cropW, photoH.toInt())
                } else {
                    val cropH = (photoW / winRatio).toInt()
                    val top = ((photoH - cropH) / 2f).toInt()
                    Rect(0, top, photoW.toInt(), top + cropH)
                }

                canvas.drawBitmap(rotatedBitmap, srcRect, windowRect, HIGH_QUALITY_PAINT)
            } else {
                canvas.drawBitmap(rotatedBitmap, null, windowRect, HIGH_QUALITY_PAINT)
            }

            // 2. Draw Face Mask onto captured photo using shutter snapshot if mask toggle was ON & faces detected
            if (maskBitmap != null && facesSnapshot.isNotEmpty() && previewWidth > 0f && previewHeight > 0f) {
                val windowW = windowRect.width().toFloat()
                val windowH = windowRect.height().toFloat()

                for (faceData in facesSnapshot) {
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

                        Log.d(
                            "MaskTransform",
                            "CAPTURE: winSize=${windowW}x${windowH}, normFaceW=$normFaceWidth, faceWOnComp=$faceWidthOnComposite, eyeCenter=($eyeCenterX, $eyeCenterY), maskScale=$maskScale, deltaAngle=$deltaAngleDeg"
                        )

                        canvas.save()
                        canvas.translate(eyeCenterX, eyeCenterY)
                        canvas.rotate(deltaAngleDeg)
                        canvas.scale(maskScale, maskScale)
                        canvas.drawBitmap(
                            maskBitmap,
                            -SpideyMaskReference.maskEyeCenter.x,
                            -SpideyMaskReference.maskEyeCenter.y,
                            HIGH_QUALITY_PAINT
                        )
                        canvas.restore()
                    }
                }
            }

            // 3. Draw rawFrameBitmap (frame poster) over full composite
            canvas.drawBitmap(rawFrameBitmap, 0f, 0f, HIGH_QUALITY_PAINT)

            // 4. Draw the shared branding overlay (corner logos + event badge) on top of
            // every filter -- except Classic, whose own frame art already has full
            // branding baked in (see FilterType.showBrandingOverlay).
            if (selectedFilter.showBrandingOverlay) {
                drawBrandingOverlay(context, canvas, frameW, frameH, windowRect, selectedFilter.badgeCorner)
            }

            composite
        } else {
            rotatedBitmap
        }

        val photoFile = File(
            context.cacheDir,
            "captured_spidey_${System.currentTimeMillis()}.jpg"
        )

        return try {
            FileOutputStream(photoFile).use { out ->
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, 96, out)
            }
            if (photoFile.exists() && photoFile.length() > 0) photoFile else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Builds just the decorative layer of a filter -- frame art + branding overlay, with the
     * photo window left fully transparent -- with no photo or mask drawn into it. Used to bake
     * the same frame decoration onto a recorded *video* (see VideoCompositionUtils), where the
     * "photo" is a whole video track rather than a single bitmap so it can't go through
     * createComposedPoster directly. Returns the decoration bitmap plus the pixel rect (within
     * that bitmap) where the video should be positioned to show through the window.
     */
    fun buildFrameDecorationOverlay(context: Context, selectedFilter: FilterType): Pair<Bitmap, Rect>? {
        val rawFrameBitmap = decodeFrameBitmap(context, selectedFilter) ?: return null
        val frameW = rawFrameBitmap.width
        val frameH = rawFrameBitmap.height
        val windowRect = resolveWindowRect(selectedFilter, rawFrameBitmap, frameW, frameH)

        val composite = Bitmap.createBitmap(frameW, frameH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(composite)
        canvas.drawBitmap(rawFrameBitmap, 0f, 0f, HIGH_QUALITY_PAINT)

        if (selectedFilter.showBrandingOverlay) {
            drawBrandingOverlay(context, canvas, frameW, frameH, windowRect, selectedFilter.badgeCorner)
        }

        return composite to windowRect
    }

    private fun decodeFrameBitmap(context: Context, selectedFilter: FilterType): Bitmap? {
        return selectedFilter.frameAssetPath?.let { assetPath ->
            runCatching {
                context.assets.open(assetPath).use { inputStream ->
                    val options = BitmapFactory.Options().apply {
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                        inPremultiplied = true
                    }
                    BitmapFactory.decodeStream(inputStream, null, options)
                }
            }.getOrNull()
        }
    }

    private fun resolveWindowRect(selectedFilter: FilterType, rawFrameBitmap: Bitmap, frameW: Int, frameH: Int): Rect {
        val activeWindow = FrameWindowDetector.detectTransparentWindow(rawFrameBitmap)
            ?: selectedFilter.frameDefinition?.fallbackWindow
            ?: NormalizedRect(0f, 0f, 1f, 1f)
        return activeWindow.toPixelRect(frameW, frameH)
    }

    private val CORNER_LOGO_ASSETS = listOf(
        "logos/algozenith_logo.png",
        "logos/iot_logo.png",
        "logos/ksac_logo.webp"
    )
    private const val EVENT_BADGE_ASSET = "logos/encodexzenith_logo.png"

    /**
     * Draws the shared branding layer on top of the fully composed frame: a small row of
     * society logos in the top-right corner, and the event badge straddling one bottom
     * corner of the photo window (half over the photo, half over the frame border). Mirrors
     * BrandingOverlay's exact math so the final photo matches what the live preview showed.
     */
    private fun drawBrandingOverlay(
        context: Context,
        canvas: Canvas,
        frameW: Int,
        frameH: Int,
        windowRect: Rect,
        badgeCorner: BadgeCorner
    ) {
        val config = BrandingConfig.load(context)
        val logoSize = frameW * config.logoSizeRatio
        val logoGap = frameW * config.logoGapRatio
        val marginTop = frameH * config.logoMarginTopRatio
        val marginEnd = frameW * config.logoMarginEndRatio
        val totalWidth = CORNER_LOGO_ASSETS.size * logoSize + (CORNER_LOGO_ASSETS.size - 1) * logoGap

        var x = frameW - marginEnd - totalWidth
        for (path in CORNER_LOGO_ASSETS) {
            val logoBitmap = decodeAssetBitmap(context, path)
            if (logoBitmap != null) {
                drawCircularBitmap(canvas, logoBitmap, RectF(x, marginTop, x + logoSize, marginTop + logoSize))
            }
            x += logoSize + logoGap
        }

        val badgeBitmap = decodeAssetBitmap(context, EVENT_BADGE_ASSET)
        if (badgeBitmap != null) {
            val badgeWidth = frameW * config.badgeWidthRatio
            val aspect = badgeBitmap.width.toFloat() / badgeBitmap.height.toFloat()
            val badgeHeight = badgeWidth / aspect

            val cornerX = if (badgeCorner == BadgeCorner.RIGHT) windowRect.right.toFloat() else windowRect.left.toFloat()
            val cornerY = windowRect.bottom.toFloat()
            val offsetX = if (badgeCorner == BadgeCorner.RIGHT) {
                cornerX - badgeWidth * config.badgeInsetRatio
            } else {
                cornerX - badgeWidth * (1f - config.badgeInsetRatio)
            }
            val offsetY = cornerY - badgeHeight * 0.5f

            canvas.drawBitmap(badgeBitmap, null, RectF(offsetX, offsetY, offsetX + badgeWidth, offsetY + badgeHeight), HIGH_QUALITY_PAINT)
        }
    }

    private fun drawCircularBitmap(canvas: Canvas, bitmap: Bitmap, destRect: RectF) {
        val paint = HIGH_QUALITY_PAINT
        canvas.save()
        val clipPath = Path().apply { addOval(destRect, Path.Direction.CW) }
        canvas.clipPath(clipPath)
        canvas.drawBitmap(bitmap, null, destRect, paint)
        canvas.restore()
    }

    private fun decodeAssetBitmap(context: Context, path: String): Bitmap? {
        return runCatching {
            context.assets.open(path).use { input ->
                val options = BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                    inPremultiplied = true
                }
                BitmapFactory.decodeStream(input, null, options)
            }
        }.getOrNull()
    }
}

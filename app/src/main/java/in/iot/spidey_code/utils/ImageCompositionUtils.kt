package `in`.iot.spidey_code.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Rect
import android.util.Log
import `in`.iot.spidey_code.data.model.FilterType
import `in`.iot.spidey_code.data.model.NormalizedRect
import `in`.iot.spidey_code.data.model.frameAssetPath
import `in`.iot.spidey_code.data.model.frameDefinition
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
        val frameDefinition = selectedFilter.frameDefinition

        val rawFrameBitmap = selectedFilter.frameAssetPath?.let { assetPath ->
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

            val activeWindow = FrameWindowDetector.detectTransparentWindow(rawFrameBitmap)
                ?: frameDefinition?.fallbackWindow
                ?: NormalizedRect(0f, 0f, 1f, 1f)

            val windowRect = activeWindow.toPixelRect(frameW, frameH)

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

                canvas.drawBitmap(rotatedBitmap, srcRect, windowRect, null)
            } else {
                canvas.drawBitmap(rotatedBitmap, null, windowRect, null)
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
                            null
                        )
                        canvas.restore()
                    }
                }
            }

            // 3. Draw rawFrameBitmap (frame poster) over full composite
            canvas.drawBitmap(rawFrameBitmap, 0f, 0f, null)
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
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
            if (photoFile.exists() && photoFile.length() > 0) photoFile else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

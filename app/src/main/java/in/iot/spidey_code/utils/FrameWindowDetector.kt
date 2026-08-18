package `in`.iot.spidey_code.utils

import android.graphics.Bitmap
import `in`.iot.spidey_code.data.model.NormalizedRect
import java.util.ArrayDeque

/**
 * Utility for analyzing frame assets and automatically detecting the largest
 * contiguous transparent photo window using connected-component analysis (BFS).
 */
object FrameWindowDetector {

    /**
     * Inspects a Bitmap's alpha channel to find the largest contiguous transparent region.
     *
     * @param bitmap The frame bitmap to analyze.
     * @param alphaThreshold Alpha threshold below which a pixel is considered transparent (default 40).
     * @param minAreaRatio Minimum area ratio (default 5% of total image area) required to qualify.
     * @return NormalizedRect (0.0f..1.0f) of the detected photo window, or null if not detected.
     */
    fun detectTransparentWindow(
        bitmap: Bitmap,
        alphaThreshold: Int = 40,
        minAreaRatio: Float = 0.05f
    ): NormalizedRect? {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= 0 || height <= 0) return null

        // Downsample grid for fast connected component analysis
        val maxDim = 320
        val sampleSize = maxOf(1, maxOf(width, height) / maxDim)
        val sampledW = width / sampleSize
        val sampledH = height / sampleSize

        if (sampledW <= 0 || sampledH <= 0) return null

        val pixels = IntArray(sampledW * sampledH)
        val scaledBitmap = if (sampleSize > 1) {
            Bitmap.createScaledBitmap(bitmap, sampledW, sampledH, false)
        } else {
            bitmap
        }

        scaledBitmap.getPixels(pixels, 0, sampledW, 0, 0, sampledW, sampledH)
        if (scaledBitmap != bitmap) {
            scaledBitmap.recycle()
        }

        val visited = BooleanArray(sampledW * sampledH)
        var maxComponentSize = 0
        var bestMinX = sampledW
        var bestMaxX = 0
        var bestMinY = sampledH
        var bestMaxY = 0

        val totalPixels = sampledW * sampledH
        val minComponentPixels = (totalPixels * minAreaRatio).toInt()

        val dx = intArrayOf(-1, 1, 0, 0)
        val dy = intArrayOf(0, 0, -1, 1)

        val queue = ArrayDeque<Int>()

        for (y in 0 until sampledH) {
            for (x in 0 until sampledW) {
                val idx = y * sampledW + x
                if (visited[idx]) continue

                val alpha = (pixels[idx] ushr 24) and 0xFF
                if (alpha <= alphaThreshold) {
                    var currentSize = 0
                    var minX = x
                    var maxX = x
                    var minY = y
                    var maxY = y

                    queue.clear()
                    queue.add(idx)
                    visited[idx] = true

                    while (queue.isNotEmpty()) {
                        val curr = queue.poll() ?: break
                        currentSize++
                        val cx = curr % sampledW
                        val cy = curr / sampledW

                        if (cx < minX) minX = cx
                        if (cx > maxX) maxX = cx
                        if (cy < minY) minY = cy
                        if (cy > maxY) maxY = cy

                        for (i in 0 until 4) {
                            val nx = cx + dx[i]
                            val ny = cy + dy[i]
                            if (nx in 0 until sampledW && ny in 0 until sampledH) {
                                val nIdx = ny * sampledW + nx
                                if (!visited[nIdx]) {
                                    val nAlpha = (pixels[nIdx] ushr 24) and 0xFF
                                    if (nAlpha <= alphaThreshold) {
                                        visited[nIdx] = true
                                        queue.add(nIdx)
                                    }
                                }
                            }
                        }
                    }

                    if (currentSize > maxComponentSize && currentSize >= minComponentPixels) {
                        maxComponentSize = currentSize
                        bestMinX = minX
                        bestMaxX = maxX
                        bestMinY = minY
                        bestMaxY = maxY
                    }
                }
            }
        }

        if (maxComponentSize < minComponentPixels) {
            return null
        }

        val left = bestMinX.toFloat() / sampledW.toFloat()
        val top = bestMinY.toFloat() / sampledH.toFloat()
        val windowW = (bestMaxX - bestMinX).toFloat() / sampledW.toFloat()
        val windowH = (bestMaxY - bestMinY).toFloat() / sampledH.toFloat()

        return NormalizedRect(left, top, windowW, windowH)
    }
}

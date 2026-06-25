package com.receiptvault.app.scanner.detection

import android.graphics.Bitmap
import android.graphics.PointF
import com.receiptvault.app.scanner.model.DocumentCorners
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

/**
 * On-device document edge detection without cloud or GMS.
 * Finds a high-contrast bounding quad on a downscaled frame.
 */
@Singleton
class DocumentEdgeDetector @Inject constructor() {

    fun detect(bitmap: Bitmap): DocumentCorners {
        val sampleWidth = 240
        val scale = sampleWidth.toFloat() / bitmap.width
        val sampleHeight = (bitmap.height * scale).toInt().coerceAtLeast(1)
        val sample = Bitmap.createScaledBitmap(bitmap, sampleWidth, sampleHeight, true)

        val w = sample.width
        val h = sample.height
        val pixels = IntArray(w * h)
        sample.getPixels(pixels, 0, w, 0, 0, w, h)
        if (sample !== bitmap) sample.recycle()

        var minX = w
        var minY = h
        var maxX = 0
        var maxY = 0
        var hits = 0

        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                val lum = luminance(pixels[y * w + x])
                val left = luminance(pixels[y * w + (x - 1)])
                val right = luminance(pixels[y * w + (x + 1)])
                val up = luminance(pixels[(y - 1) * w + x])
                val down = luminance(pixels[(y + 1) * w + x])
                val edge = kotlin.math.abs(lum - left) + kotlin.math.abs(lum - right) +
                    kotlin.math.abs(lum - up) + kotlin.math.abs(lum - down)
                if (edge > 55) {
                    minX = min(minX, x)
                    minY = min(minY, y)
                    maxX = max(maxX, x)
                    maxY = max(maxY, y)
                    hits++
                }
            }
        }

        if (hits < (w * h) / 50) {
            return DocumentCorners.fullFrame(bitmap.width, bitmap.height)
        }

        val invScale = 1f / scale
        val padX = (maxX - minX) * 0.02f * invScale
        val padY = (maxY - minY) * 0.02f * invScale
        return DocumentCorners(
            topLeft = PointF(minX * invScale - padX, minY * invScale - padY),
            topRight = PointF(maxX * invScale + padX, minY * invScale - padY),
            bottomRight = PointF(maxX * invScale + padX, maxY * invScale + padY),
            bottomLeft = PointF(minX * invScale - padX, maxY * invScale + padY)
        ).clamped(bitmap.width, bitmap.height)
    }

    private fun DocumentCorners.clamped(width: Int, height: Int): DocumentCorners {
        fun clamp(p: PointF) = PointF(
            p.x.coerceIn(0f, width.toFloat()),
            p.y.coerceIn(0f, height.toFloat())
        )
        return DocumentCorners(
            topLeft = clamp(topLeft),
            topRight = clamp(topRight),
            bottomRight = clamp(bottomRight),
            bottomLeft = clamp(bottomLeft)
        )
    }

    private fun luminance(pixel: Int): Int {
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        return (0.299 * r + 0.587 * g + 0.114 * b).toInt()
    }
}

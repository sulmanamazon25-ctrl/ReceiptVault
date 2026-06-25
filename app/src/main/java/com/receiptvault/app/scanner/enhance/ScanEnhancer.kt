package com.receiptvault.app.scanner.enhance

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import com.receiptvault.app.scanner.model.DocumentCorners
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.hypot
import kotlin.math.max

/** Perspective correction, contrast boost, and document-style enhancement — all on-device. */
@Singleton
class ScanEnhancer @Inject constructor() {

    fun enhance(bitmap: Bitmap, corners: DocumentCorners): Bitmap {
        val warped = warpPerspective(bitmap, corners)
        return boostDocument(warped)
    }

    fun warpPerspective(source: Bitmap, corners: DocumentCorners): Bitmap {
        val src = corners.toFloatArray()
        val widthTop = hypot(
            (corners.topRight.x - corners.topLeft.x).toDouble(),
            (corners.topRight.y - corners.topLeft.y).toDouble()
        ).toInt()
        val widthBottom = hypot(
            (corners.bottomRight.x - corners.bottomLeft.x).toDouble(),
            (corners.bottomRight.y - corners.bottomLeft.y).toDouble()
        ).toInt()
        val outWidth = max(widthTop, widthBottom).coerceAtLeast(100)
        val heightLeft = hypot(
            (corners.bottomLeft.x - corners.topLeft.x).toDouble(),
            (corners.bottomLeft.y - corners.topLeft.y).toDouble()
        ).toInt()
        val heightRight = hypot(
            (corners.bottomRight.x - corners.topRight.x).toDouble(),
            (corners.bottomRight.y - corners.topRight.y).toDouble()
        ).toInt()
        val outHeight = max(heightLeft, heightRight).coerceAtLeast(100)

        val dst = floatArrayOf(
            0f, 0f,
            outWidth.toFloat(), 0f,
            outWidth.toFloat(), outHeight.toFloat(),
            0f, outHeight.toFloat()
        )
        val matrix = Matrix()
        matrix.setPolyToPoly(src, 0, dst, 0, 4)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    fun boostDocument(bitmap: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val contrast = ColorMatrix(
            floatArrayOf(
                1.25f, 0f, 0f, 0f, -20f,
                0f, 1.25f, 0f, 0f, -20f,
                0f, 0f, 1.25f, 0f, -20f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        val saturation = ColorMatrix()
        saturation.setSaturation(0.85f)
        contrast.postConcat(saturation)
        paint.colorFilter = ColorMatrixColorFilter(contrast)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        if (output !== bitmap) bitmap.recycle()
        return output
    }

    fun compositeOnWhite(bitmap: Bitmap): Bitmap {
        val out = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(bitmap, 0f, 0f, null)
        return out
    }
}

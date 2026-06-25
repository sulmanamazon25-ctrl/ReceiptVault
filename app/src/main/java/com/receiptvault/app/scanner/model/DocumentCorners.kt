package com.receiptvault.app.scanner.model

import android.graphics.PointF

/** Four corners of a detected document in image coordinates (top-left, top-right, bottom-right, bottom-left). */
data class DocumentCorners(
    val topLeft: PointF,
    val topRight: PointF,
    val bottomRight: PointF,
    val bottomLeft: PointF
) {
    fun toFloatArray(): FloatArray = floatArrayOf(
        topLeft.x, topLeft.y,
        topRight.x, topRight.y,
        bottomRight.x, bottomRight.y,
        bottomLeft.x, bottomLeft.y
    )

    companion object {
        fun fullFrame(width: Int, height: Int, marginFraction: Float = 0.05f): DocumentCorners {
            val mx = width * marginFraction
            val my = height * marginFraction
            return DocumentCorners(
                topLeft = PointF(mx, my),
                topRight = PointF(width - mx, my),
                bottomRight = PointF(width - mx, height - my),
                bottomLeft = PointF(mx, height - my)
            )
        }
    }
}

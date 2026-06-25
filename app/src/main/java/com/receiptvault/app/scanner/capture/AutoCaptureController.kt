package com.receiptvault.app.scanner.capture

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Triggers auto-capture when the preview frame is stable and sharp enough.
 * Pro users get full auto-capture; free tier uses manual shutter only.
 */
@Singleton
class AutoCaptureController @Inject constructor() {

    private var lastHash: Int = 0
    private var stableFrames: Int = 0

    fun reset() {
        lastHash = 0
        stableFrames = 0
    }

    /**
     * @param luminanceSample downscaled grayscale hash input (e.g. 16x16 luma sum)
     * @param sharpnessScore higher is sharper (variance of edges)
     */
    fun onFrame(luminanceSample: Int, sharpnessScore: Float, enabled: Boolean): Boolean {
        if (!enabled) return false
        if (sharpnessScore < 12f) {
            stableFrames = 0
            return false
        }
        val delta = kotlin.math.abs(luminanceSample - lastHash)
        lastHash = luminanceSample
        if (delta < 800) {
            stableFrames++
        } else {
            stableFrames = 0
        }
        return stableFrames >= 8
    }
}

package br.edu.robotcontroller

import kotlin.math.min

/** Reserves label space and keeps the whole thumb inside the ring at full travel. */
data class JoystickGeometry(val centerX: Float, val centerY: Float, val radius: Float) {
    val thumbRadius get() = radius * 0.26f
    val travel get() = radius - thumbRadius
    companion object {
        fun fit(width: Float, height: Float, labelHeight: Float, padding: Float): JoystickGeometry {
            val usableHeight = (height - labelHeight).coerceAtLeast(0f)
            val radius = (min(width, usableHeight) / 2f - padding).coerceAtLeast(0f)
            return JoystickGeometry(width / 2f, usableHeight / 2f, radius)
        }
    }
}
